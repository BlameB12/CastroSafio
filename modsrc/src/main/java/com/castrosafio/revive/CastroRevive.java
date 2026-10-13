package com.castrosafio.revive;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod(CastroRevive.MOD_ID)
public final class CastroRevive {
    public static final String MOD_ID = "castrorevive";

    private static final int TOTAL_TICKS = 320; // 16 s
    private static final List<ReviveRitual> ACTIVE_RITUALS = new ArrayList<>();

    private static final Vector3f CYAN = new Vector3f(0.05F, 0.95F, 1.00F);
    private static final Vector3f GREEN = new Vector3f(0.62F, 1.00F, 0.16F);
    private static final Vector3f YELLOW = new Vector3f(1.00F, 0.95F, 0.12F);
    private static final Vector3f WHITE = new Vector3f(1.00F, 1.00F, 1.00F);
    private static final Vector3f BLUE = new Vector3f(0.05F, 0.45F, 1.00F);

    public CastroRevive() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                literal("revivir")
                        .requires(source -> source.hasPermission(2))
                        .then(argument("jugador", EntityArgument.player())
                                .executes(ctx -> startRitual(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "jugador")
                                )))
        );
    }

    private static int startRitual(CommandSourceStack source, ServerPlayer target) {
        ServerLevel level = source.getLevel();
        Vec3 raw = source.getPosition();

        // Center on the block, so the ritual looks symmetrical around a campfire/altar.
        Vec3 center = new Vec3(
                Math.floor(raw.x) + 0.5D,
                Math.floor(raw.y),
                Math.floor(raw.z) + 0.5D
        );

        ACTIVE_RITUALS.removeIf(ritual -> ritual.targetId.equals(target.getUUID()));
        ACTIVE_RITUALS.add(new ReviveRitual(level.dimension(), center, target.getUUID(), target.getGameProfile().getName()));

        source.sendSuccess(() -> Component.literal("§bResurrección de §f" + target.getGameProfile().getName() + " §bpreparada."), true);
        broadcast(level, Component.literal("§b✦ Resurrección de §f" + target.getGameProfile().getName() + " §bpreparada. §7El ritual comenzará ahora."));

        level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 1.7F, 0.72F);
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Iterator<ReviveRitual> iterator = ACTIVE_RITUALS.iterator();

        while (iterator.hasNext()) {
            ReviveRitual ritual = iterator.next();
            ServerLevel level = server.getLevel(ritual.dimension);
            if (level == null) {
                iterator.remove();
                continue;
            }

            ritual.tick++;
            renderRitual(level, ritual);

            if (ritual.tick >= TOTAL_TICKS) {
                finishRitual(server, level, ritual);
                iterator.remove();
            }
        }
    }

    private static void renderRitual(ServerLevel level, ReviveRitual ritual) {
        final int t = ritual.tick;
        final Vec3 c = ritual.center;

        // Persistent bright core, visible during the whole ritual.
        if ((t & 1) == 0) {
            send(level, ParticleTypes.END_ROD, c.x, c.y + 5.3D, c.z, 1, 0.02D, 0.04D, 0.02D, 0.001D);
        }

        // 0-2 s: a tiny light rises over the altar, like the first frames of the reference.
        if (t <= 40) {
            double p = t / 40.0D;
            double y = c.y + 1.6D + p * 4.0D;
            spiralColumn(level, c, y, t, 0.25D + p * 0.45D, 8);
            if (t % 10 == 0) {
                level.playSound(null, BlockPos.containing(c), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.MASTER, 0.8F, 0.75F + t / 160.0F);
            }
        }

        // 2-5.5 s: three neon rings appear in the sky and rotate independently.
        if (t >= 35 && t <= 110) {
            double p = ease((t - 35) / 75.0D);
            double y = c.y + 5.7D + p * 1.5D;
            double base = 0.8D + p * 2.5D;
            int points = 22;
            ringXZ(level, c.x, y, c.z, base, t * 0.035D, points, dust(CYAN, GREEN, 1.25F));
            ringTilted(level, c, y, base + 0.32D, t * 0.028D + 0.8D, 0.38D, points, dust(GREEN, YELLOW, 1.2F));
            ringTilted(level, c, y, base + 0.62D, -t * 0.031D + 1.9D, -0.42D, points, dust(CYAN, YELLOW, 1.15F));
        }

        // 5-7.5 s: geometric outlines grow behind/around the floating rings.
        if (t >= 95 && t <= 150 && (t & 1) == 0) {
            double p = ease((t - 95) / 55.0D);
            double y = c.y + 6.3D;
            double size = 1.5D + p * 5.2D;
            rotatedSquare(level, c.x, y, c.z, size, t * 0.014D, dust(GREEN, CYAN, 1.0F));
            rotatedSquare(level, c.x, y - 0.15D, c.z, size * 0.72D, -t * 0.021D + Math.PI / 4.0D, dust(YELLOW, CYAN, 0.9F));
        }

        // 7-11.5 s: the scene fills with distant cyan/yellow stars and a wide horizontal aura.
        if (t >= 135 && t <= 230) {
            starField(level, ritual, 7);
            if (t % 3 == 0) {
                double p = (t - 135) / 95.0D;
                double radius = 3.0D + p * 15.0D;
                ringXZ(level, c.x, c.y + 0.35D, c.z, radius, t * 0.01D, 34, dust(CYAN, GREEN, 0.75F));
                ringXZ(level, c.x, c.y + 0.65D, c.z, radius * 0.72D, -t * 0.014D, 28, dust(YELLOW, CYAN, 0.65F));
            }
        }

        // 8.5-15 s: giant spherical/orbital energy lines surround the altar.
        if (t >= 170 && t <= 300) {
            double p = ease((t - 170) / 130.0D);
            double radius = 2.3D + p * 7.5D;
            if ((t & 1) == 0) {
                orbitSphere(level, c.add(0, 2.2D, 0), radius, t, 26);
            }

            // Nested ground spirals from the reference's big glowing discs.
            if (t % 3 == 0) {
                for (int layer = 0; layer < 3; layer++) {
                    double r = radius * (0.35D + layer * 0.28D);
                    ringXZ(level, c.x, c.y + 0.18D + layer * 0.12D, c.z, r, t * (0.018D + layer * 0.004D), 28,
                            layer == 1 ? dust(YELLOW, GREEN, 0.85F) : dust(CYAN, GREEN, 0.85F));
                }
            }
        }

        // 12-15.75 s: energy contracts into a bright cage around the center.
        if (t >= 240 && t <= 315) {
            double p = (t - 240) / 75.0D;
            double radius = 5.0D * (1.0D - p) + 0.65D;
            for (int lane = 0; lane < 3; lane++) {
                double phase = t * (0.065D + lane * 0.008D) + lane * 2.1D;
                verticalOrbit(level, c.add(0, 2.0D, 0), radius + lane * 0.2D, phase, 16,
                        lane == 1 ? dust(YELLOW, CYAN, 1.2F) : dust(CYAN, GREEN, 1.15F));
            }
            if (t % 4 == 0) {
                send(level, ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 1.0D, c.z, 12, 1.6D, 2.7D, 1.6D, 0.02D);
            }
        }

        // Final flash just before the actual resurrection.
        if (t == 316) {
            level.playSound(null, BlockPos.containing(c), SoundEvents.TOTEM_USE, SoundSource.MASTER, 2.0F, 0.72F);
            send(level, ParticleTypes.FLASH, c.x, c.y + 2.0D, c.z, 4, 0.25D, 0.7D, 0.25D, 0.0D);
            send(level, ParticleTypes.END_ROD, c.x, c.y + 1.5D, c.z, 90, 2.8D, 4.5D, 2.8D, 0.04D);
        }
    }

    private static void finishRitual(MinecraftServer server, ServerLevel level, ReviveRitual ritual) {
        Vec3 c = ritual.center;
        ServerPlayer target = server.getPlayerList().getPlayer(ritual.targetId);

        send(level, ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1.2D, c.z, 1, 0, 0, 0, 0);
        send(level, ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 1.2D, c.z, 120, 1.8D, 2.6D, 1.8D, 0.28D);
        level.playSound(null, BlockPos.containing(c), SoundEvents.BEACON_POWER_SELECT, SoundSource.MASTER, 2.0F, 1.25F);

        if (target != null) {
            target.setGameMode(GameType.SURVIVAL);
            target.setHealth(target.getMaxHealth());
            target.clearFire();
            target.fallDistance = 0.0F;
            target.teleportTo(level, c.x, c.y + 1.05D, c.z, target.getYRot(), target.getXRot());
            target.sendSystemMessage(Component.literal("§a✦ Has sido resucitado."));
        }

        broadcast(level, Component.literal("§a✦ La resurrección de §f" + ritual.targetName + " §aha finalizado."));
    }

    private static void spiralColumn(ServerLevel level, Vec3 c, double y, int t, double radius, int points) {
        ParticleOptions particle = dust(CYAN, GREEN, 1.0F);
        for (int i = 0; i < points; i++) {
            double a = t * 0.16D + i * (Math.PI * 2.0D / points);
            double py = y + Math.sin(a * 1.7D) * 0.35D;
            send(level, particle, c.x + Math.cos(a) * radius, py, c.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    private static void ringXZ(ServerLevel level, double cx, double y, double cz, double radius, double phase, int points, ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double a = phase + i * (Math.PI * 2.0D / points);
            send(level, particle, cx + Math.cos(a) * radius, y, cz + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    private static void ringTilted(ServerLevel level, Vec3 c, double y, double radius, double phase, double tilt, int points, ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double a = phase + i * (Math.PI * 2.0D / points);
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            double py = Math.sin(a) * radius * tilt;
            send(level, particle, c.x + x, y + py, c.z + z, 1, 0, 0, 0, 0);
        }
    }

    private static void rotatedSquare(ServerLevel level, double cx, double y, double cz, double size, double rotation, ParticleOptions particle) {
        Vec3[] corners = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            double a = rotation + Math.PI / 4.0D + i * Math.PI / 2.0D;
            corners[i] = new Vec3(cx + Math.cos(a) * size, y, cz + Math.sin(a) * size);
        }
        for (int i = 0; i < 4; i++) {
            line(level, corners[i], corners[(i + 1) % 4], 12, particle);
        }
    }

    private static void line(ServerLevel level, Vec3 a, Vec3 b, int points, ParticleOptions particle) {
        for (int i = 0; i <= points; i++) {
            double p = i / (double) points;
            send(level, particle,
                    a.x + (b.x - a.x) * p,
                    a.y + (b.y - a.y) * p,
                    a.z + (b.z - a.z) * p,
                    1, 0, 0, 0, 0);
        }
    }

    private static void starField(ServerLevel level, ReviveRitual ritual, int count) {
        Vec3 c = ritual.center;
        long seed = ritual.targetId.getMostSignificantBits() ^ ritual.targetId.getLeastSignificantBits() ^ (ritual.tick * 341873128712L);
        for (int i = 0; i < count; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            double a = ((seed >>> 16) & 0xFFFF) / 65535.0D * Math.PI * 2.0D;
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            double r = 5.0D + ((seed >>> 16) & 0xFFFF) / 65535.0D * 15.0D;
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            double y = c.y + 1.0D + ((seed >>> 16) & 0xFFFF) / 65535.0D * 10.0D;
            ParticleOptions p = (i & 1) == 0 ? dust(CYAN, WHITE, 0.7F) : dust(YELLOW, GREEN, 0.65F);
            send(level, p, c.x + Math.cos(a) * r, y, c.z + Math.sin(a) * r, 1, 0.02D, 0.02D, 0.02D, 0);
        }
    }

    private static void orbitSphere(ServerLevel level, Vec3 center, double radius, int t, int points) {
        ParticleOptions cyan = dust(CYAN, GREEN, 0.85F);
        ParticleOptions yellow = dust(YELLOW, CYAN, 0.8F);
        for (int ring = 0; ring < 3; ring++) {
            double tilt = -0.75D + ring * 0.75D;
            double phase = t * (0.020D + ring * 0.006D) + ring * 1.3D;
            for (int i = 0; i < points; i++) {
                double a = phase + i * (Math.PI * 2.0D / points);
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius * Math.cos(tilt);
                double y = Math.sin(a) * radius * Math.sin(tilt);
                send(level, ring == 1 ? yellow : cyan, center.x + x, center.y + y, center.z + z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void verticalOrbit(ServerLevel level, Vec3 center, double radius, double phase, int points, ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double a = phase + i * (Math.PI * 2.0D / points);
            double x = Math.cos(a) * radius;
            double y = Math.sin(a) * radius;
            double z = Math.sin(a * 1.55D + phase * 0.35D) * radius * 0.55D;
            send(level, particle, center.x + x, center.y + y, center.z + z, 1, 0, 0, 0, 0);
        }
    }

    private static DustColorTransitionOptions dust(Vector3f from, Vector3f to, float scale) {
        return new DustColorTransitionOptions(from, to, scale);
    }

    private static void send(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                             int count, double dx, double dy, double dz, double speed) {
        // longDistance=true is deliberate: every player in the dimension receives the same world effect.
        for (ServerPlayer viewer : level.players()) {
            level.sendParticles(viewer, particle, true, x, y, z, count, dx, dy, dz, speed);
        }
    }

    private static void broadcast(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
        }
    }

    private static double ease(double x) {
        x = Math.max(0.0D, Math.min(1.0D, x));
        return x * x * (3.0D - 2.0D * x);
    }

    private static final class ReviveRitual {
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final UUID targetId;
        private final String targetName;
        private int tick;

        private ReviveRitual(ResourceKey<Level> dimension, Vec3 center, UUID targetId, String targetName) {
            this.dimension = dimension;
            this.center = center;
            this.targetId = targetId;
            this.targetName = targetName;
        }
    }
}
