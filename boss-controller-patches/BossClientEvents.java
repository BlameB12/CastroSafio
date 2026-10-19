package com.castrosafio.bosscontroller.client;

import com.castrosafio.bosscontroller.CastroSafioBossController;
import com.castrosafio.bosscontroller.control.BossControlManager;
import com.castrosafio.bosscontroller.network.BossInputPayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.Input;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CastroSafioBossController.MODID, value = Dist.CLIENT)
public final class BossClientEvents {
    private static boolean active;
    private static CameraType previousCamera;

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
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !isControlSeat(minecraft.player.getVehicle())) {
            deactivate(minecraft);
            return;
        }

        if (!active) {
            active = true;
            previousCamera = minecraft.options.getCameraType();
        }
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        float forward = (minecraft.options.keyUp.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyDown.isDown() ? 1.0F : 0.0F);
        float strafe = (minecraft.options.keyLeft.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyRight.isDown() ? 1.0F : 0.0F);

        int flags = 0;
        if (minecraft.options.keyJump.isDown()) flags |= BossInputPayload.FLAG_JUMP;
        if (minecraft.options.keyShift.isDown()) flags |= BossInputPayload.FLAG_DESCEND;
        if (minecraft.options.keySprint.isDown()) flags |= BossInputPayload.FLAG_SPRINT;

        PacketDistributor.sendToServer(new BossInputPayload(
                forward,
                strafe,
                flags,
                minecraft.player.getYRot(),
                minecraft.player.getXRot()
        ));
    }

    private static boolean isControlSeat(Entity vehicle) {
        return vehicle instanceof ArmorStand
                && vehicle.getCustomName() != null
                && BossControlManager.SEAT_NAME.equals(vehicle.getCustomName().getString());
    }

    private static void deactivate(Minecraft minecraft) {
        if (!active) return;
        if (previousCamera != null) {
            minecraft.options.setCameraType(previousCamera);
        }
        active = false;
        previousCamera = null;
    }
}
