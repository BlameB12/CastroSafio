package com.castrosafio.bosscontroller.control;

import com.castrosafio.bosscontroller.network.BossInputPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BossControlManager {
    public static final String SEAT_NAME = "CSBC_CAMERA_SEAT";

    private static final Map<UUID, BossControlSession> SESSIONS = new HashMap<>();
    private static final Map<UUID, BossInputPayload> INPUTS = new HashMap<>();
    private static final Map<UUID, UUID> TARGET_OWNERS = new HashMap<>();

    private BossControlManager() {}

    public static boolean isControlling(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static LivingEntity getControlledTarget(ServerPlayer player) {
        BossControlSession session = SESSIONS.get(player.getUUID());
        if (session == null) return null;
        MinecraftServer server = player.getServer();
        if (server == null) return null;
        ServerLevel level = server.getLevel(session.levelKey());
        if (level == null) return null;
        Entity entity = level.getEntity(session.targetId());
        return entity instanceof LivingEntity living ? living : null;
    }

    public static Component start(ServerPlayer player, Entity requestedTarget) {
        if (!(requestedTarget instanceof LivingEntity target)) {
            return Component.literal("La entidad elegida no es una entidad viva.").withStyle(ChatFormatting.RED);
        }
        if (target instanceof ServerPlayer) {
            return Component.literal("No se puede usar el controlador sobre jugadores.").withStyle(ChatFormatting.RED);
        }
        if (player.level() != target.level()) {
            return Component.literal("El jugador y el jefe deben estar en la misma dimensión.").withStyle(ChatFormatting.RED);
        }

        UUID existingOwner = TARGET_OWNERS.get(target.getUUID());
        if (existingOwner != null && !existingOwner.equals(player.getUUID())) {
            return Component.literal("Ese jefe ya está siendo controlado por otro jugador.").withStyle(ChatFormatting.RED);
        }

        stop(player, false);

        boolean oldNoAi = target instanceof Mob mob && mob.isNoAi();
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        ServerLevel level = (ServerLevel) target.level();
        ArmorStand seat = new ArmorStand(level, target.getX(), cameraSeatY(target), target.getZ());
        seat.setInvisible(true);
        seat.setInvulnerable(true);
        seat.setNoGravity(true);
        seat.setSilent(true);
        seat.noPhysics = true;
        seat.setCustomName(Component.literal(SEAT_NAME + ":" + target.getId()));
        seat.setCustomNameVisible(false);
        level.addFreshEntity(seat);

        boolean oldInvisible = player.isInvisible();
        boolean oldInvulnerable = player.isInvulnerable();
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.stopRiding();
        player.startRiding(seat, true);

        BossControlSession session = new BossControlSession(
                player.getUUID(), target.getUUID(), level.dimension(), seat.getUUID(),
                oldNoAi, oldInvisible, oldInvulnerable
        );
        SESSIONS.put(player.getUUID(), session);
        TARGET_OWNERS.put(target.getUUID(), player.getUUID());
        INPUTS.put(player.getUUID(), new BossInputPayload(0, 0, 0, player.getYRot(), player.getXRot()));
        IgnisActionController.onControlStarted(player, target);

        return Component.literal("Controlando: ").withStyle(ChatFormatting.GREEN)
                .append(target.getDisplayName())
                .append(Component.literal(" | WASD mover, Espacio subir/saltar, Shift bajar, Ctrl rápido.")
                        .withStyle(ChatFormatting.GRAY));
    }

    public static void updateInput(ServerPlayer player, BossInputPayload input) {
        if (!SESSIONS.containsKey(player.getUUID())) return;
        float forward = Mth.clamp(input.forward(), -1.0F, 1.0F);
        float strafe = Mth.clamp(input.strafe(), -1.0F, 1.0F);
        float pitch = Mth.clamp(input.pitch(), -90.0F, 90.0F);
        INPUTS.put(player.getUUID(), new BossInputPayload(forward, strafe, input.flags(), input.yaw(), pitch));
    }

    public static void tick(MinecraftServer server) {
        for (UUID playerId : SESSIONS.keySet().toArray(UUID[]::new)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                cleanupOffline(server, playerId);
                continue;
            }

            BossControlSession session = SESSIONS.get(playerId);
            ServerLevel level = server.getLevel(session.levelKey());
            if (level == null) {
                stop(player, true);
                continue;
            }

            Entity rawTarget = level.getEntity(session.targetId());
            Entity rawSeat = level.getEntity(session.seatId());
            if (!(rawTarget instanceof LivingEntity target) || !target.isAlive() || !(rawSeat instanceof ArmorStand seat)) {
                stop(player, true);
                continue;
            }

            // Keep the hidden rider attached near the boss. The actual client camera is anchored to the boss itself.
            seat.setPos(target.getX(), cameraSeatY(target), target.getZ());
            seat.setYRot(target.getYRot());

            if (player.getVehicle() != seat) {
                player.startRiding(seat, true);
            }

            if (target instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setTarget(null);
                mob.getNavigation().stop();
            }

            BossInputPayload input = INPUTS.getOrDefault(playerId,
                    new BossInputPayload(0, 0, 0, player.getYRot(), player.getXRot()));
            IgnisActionController.tickControlledIgnis(player, target);
            applyMovement(target, input);
        }
    }

    private static void applyMovement(LivingEntity target, BossInputPayload input) {
        // Camera/look direction owns the boss rotation. Movement no longer changes where the boss is looking.
        float lookYaw = Mth.wrapDegrees(input.yaw());
        float lookPitch = Mth.clamp(input.pitch(), -90.0F, 90.0F);
        // Keep current and previous render rotations synchronized. Cataclysm interpolates these
        // values heavily; stale body/head angles are what caused the twisting/snapping seen on Ignis.
        target.setYRot(lookYaw);
        target.yRotO = lookYaw;
        target.setYHeadRot(lookYaw);
        target.yHeadRotO = lookYaw;
        target.setYBodyRot(lookYaw);
        target.yBodyRotO = lookYaw;
        target.setXRot(lookPitch);
        target.xRotO = lookPitch;

        double forward = input.forward();
        double strafe = input.strafe();
        double magnitude = Math.sqrt(forward * forward + strafe * strafe);
        if (magnitude > 1.0D) {
            forward /= magnitude;
            strafe /= magnitude;
        }

        double yawRad = Math.toRadians(lookYaw);
        double dx = -Math.sin(yawRad) * forward + Math.cos(yawRad) * strafe;
        double dz =  Math.cos(yawRad) * forward + Math.sin(yawRad) * strafe;
        double speed = input.sprint() ? 0.70D : 0.38D;

        boolean freeVertical = target.isInWaterOrBubble() || target.isNoGravity();
        double dy = 0.0D;
        if (freeVertical) {
            if (input.jump() && !input.descend()) dy = 0.32D;
            else if (input.descend() && !input.jump()) dy = -0.32D;
        } else if (input.jump() && target.onGround()) {
            dy = 0.52D;
        }

        // Cataclysm bosses can overwrite delta movement during their own tick. Move the entity directly
        // so WASD always produces a real server-side displacement with collision handling.
        Vec3 movement = new Vec3(dx * speed, dy, dz * speed);
        if (movement.lengthSqr() > 0.000001D) {
            target.move(MoverType.SELF, movement);
            target.hasImpulse = true;
        }

        // Do not leave horizontal velocity behind for the boss AI/custom tick to reinterpret.
        Vec3 after = target.getDeltaMovement();
        target.setDeltaMovement(0.0D, freeVertical ? 0.0D : after.y, 0.0D);
    }

    public static void stop(ServerPlayer player, boolean notify) {
        BossControlSession session = SESSIONS.remove(player.getUUID());
        INPUTS.remove(player.getUUID());
        IgnisActionController.clear(player.getUUID());
        if (session == null) return;

        TARGET_OWNERS.remove(session.targetId());
        MinecraftServer server = player.getServer();
        ServerLevel level = server == null ? null : server.getLevel(session.levelKey());
        if (level != null) {
            Entity target = level.getEntity(session.targetId());
            if (target instanceof Mob mob) {
                mob.setNoAi(session.targetNoAiBefore());
            }
            Entity seat = level.getEntity(session.seatId());
            if (seat != null) seat.discard();
        }

        player.stopRiding();
        player.setInvisible(session.playerInvisibleBefore());
        player.setInvulnerable(session.playerInvulnerableBefore());

        if (notify) {
            player.sendSystemMessage(Component.literal("Control de jefe finalizado.").withStyle(ChatFormatting.YELLOW));
        }
    }

    private static void cleanupOffline(MinecraftServer server, UUID playerId) {
        BossControlSession session = SESSIONS.remove(playerId);
        INPUTS.remove(playerId);
        IgnisActionController.clear(playerId);
        if (session == null) return;
        TARGET_OWNERS.remove(session.targetId());
        ServerLevel level = server.getLevel(session.levelKey());
        if (level != null) {
            Entity target = level.getEntity(session.targetId());
            if (target instanceof Mob mob) mob.setNoAi(session.targetNoAiBefore());
            Entity seat = level.getEntity(session.seatId());
            if (seat != null) seat.discard();
        }
    }

    public static void stopAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            stop(player, false);
        }
        SESSIONS.clear();
        INPUTS.clear();
        TARGET_OWNERS.clear();
        IgnisActionController.clearAll();
    }

    public static boolean isControlledTarget(ServerPlayer player, Entity target) {
        BossControlSession session = SESSIONS.get(player.getUUID());
        return session != null && session.targetId().equals(target.getUUID());
    }

    private static double cameraSeatY(LivingEntity target) {
        // The rider is only a hidden control anchor. Camera height comes from the controlled entity eye position on the client.
        return target.getY() + Math.max(0.25D, target.getBbHeight() * 0.5D);
    }
}
