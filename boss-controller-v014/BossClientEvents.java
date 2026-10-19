package com.castrosafio.bosscontroller.client;

import com.castrosafio.bosscontroller.CastroSafioBossController;
import com.castrosafio.bosscontroller.control.BossControlManager;
import com.castrosafio.bosscontroller.network.BossActionPayload;
import com.castrosafio.bosscontroller.network.BossInputPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CastroSafioBossController.MODID, value = Dist.CLIENT)
public final class BossClientEvents {
    private static boolean active;
    private static CameraType previousCamera;
    private static int controlledEntityId = -1;
    private static boolean rWasDown;

    private BossClientEvents() {}

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || event.getEntity() != minecraft.player || !isControlSeat(minecraft.player.getVehicle())) {
            return;
        }

        Input input = event.getInput();
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        Entity controlled = getControlledEntity(minecraft);
        if (controlled == null) return;
        copyMouseLookToBoss(minecraft, controlled);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || getControlledEntityId(minecraft.player.getVehicle()) < 0) return;

        if (event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            PacketDistributor.sendToServer(new BossActionPayload(BossActionPayload.ACTION_BASIC));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            deactivate(minecraft);
            return;
        }

        int targetId = getControlledEntityId(minecraft.player.getVehicle());
        if (targetId < 0) {
            deactivate(minecraft);
            return;
        }

        Entity controlled = minecraft.level.getEntity(targetId);
        if (controlled == null) {
            deactivate(minecraft);
            return;
        }

        if (!active) {
            active = true;
            previousCamera = minecraft.options.getCameraType();
        }
        controlledEntityId = targetId;
        minecraft.setCameraEntity(controlled);
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        copyMouseLookToBoss(minecraft, controlled);

        float forward = (minecraft.options.keyUp.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyDown.isDown() ? 1.0F : 0.0F);
        float strafe = (minecraft.options.keyLeft.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyRight.isDown() ? 1.0F : 0.0F);

        int flags = 0;
        if (minecraft.options.keyJump.isDown()) flags |= BossInputPayload.FLAG_JUMP;
        if (minecraft.options.keyShift.isDown()) flags |= BossInputPayload.FLAG_DESCEND;
        if (minecraft.options.keySprint.isDown()) flags |= BossInputPayload.FLAG_SPRINT;

        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();
        PacketDistributor.sendToServer(new BossInputPayload(forward, strafe, flags, yaw, pitch));

        boolean rDown = InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_R);
        if (rDown && !rWasDown) {
            PacketDistributor.sendToServer(new BossActionPayload(BossActionPayload.ACTION_ABILITY_1));
        }
        rWasDown = rDown;
    }

    private static void copyMouseLookToBoss(Minecraft minecraft, Entity controlled) {
        if (minecraft.player == null) return;
        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();

        controlled.setYRot(yaw);
        controlled.yRotO = yaw;
        controlled.setXRot(pitch);
        controlled.xRotO = pitch;
        if (controlled instanceof LivingEntity living) {
            living.setYHeadRot(yaw);
            living.yHeadRotO = yaw;
            living.setYBodyRot(yaw);
            living.yBodyRotO = yaw;
        }
    }

    private static Entity getControlledEntity(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return null;
        int id = getControlledEntityId(minecraft.player.getVehicle());
        return id < 0 ? null : minecraft.level.getEntity(id);
    }

    private static boolean isControlSeat(Entity vehicle) {
        return getControlledEntityId(vehicle) >= 0;
    }

    private static int getControlledEntityId(Entity vehicle) {
        if (!(vehicle instanceof ArmorStand) || vehicle.getCustomName() == null) return -1;
        String name = vehicle.getCustomName().getString();
        String prefix = BossControlManager.SEAT_NAME + ":";
        if (!name.startsWith(prefix)) return -1;
        try {
            return Integer.parseInt(name.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void deactivate(Minecraft minecraft) {
        if (!active) {
            rWasDown = false;
            return;
        }
        if (minecraft.player != null) {
            minecraft.setCameraEntity(minecraft.player);
        }
        if (previousCamera != null) {
            minecraft.options.setCameraType(previousCamera);
        }
        active = false;
        controlledEntityId = -1;
        previousCamera = null;
        rWasDown = false;
    }
}
