package com.castrosafio.revive;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod(CastroRevive.MOD_ID)
public final class CastroRevive {
    public static final String MOD_ID = "castrorevive";
    public static final String FX_PREFIX = "CRVFX|";

    private static final int REVIVE_TICK = 320;
    private static final int END_TICK = 340;
    private static final List<ReviveRitual> ACTIVE_RITUALS = new ArrayList<>();

    public CastroRevive() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                literal("revivir")
                        .requires(source -> source.hasPermission(2))
                        .then(argument("jugador", EntityArgument.player())
                                .executes(ctx -> startRitual(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"))))
        );
    }

    private static int startRitual(CommandSourceStack source, ServerPlayer target) {
        ServerLevel level = source.getLevel();
        Vec3 raw = source.getPosition();
        Vec3 center = new Vec3(Math.floor(raw.x) + 0.5D, Math.floor(raw.y), Math.floor(raw.z) + 0.5D);

        Iterator<ReviveRitual> old = ACTIVE_RITUALS.iterator();
        while (old.hasNext()) {
            ReviveRitual ritual = old.next();
            if (ritual.targetId.equals(target.getUUID())) {
                ritual.marker.discard();
                old.remove();
            }
        }

        long startTime = level.getGameTime();
        ArmorStand marker = new ArmorStand(level, center.x, center.y, center.z);
        marker.setInvisible(true);
        marker.setInvulnerable(true);
        marker.setNoGravity(true);
        marker.setCustomNameVisible(false);
        marker.setCustomName(Component.literal(FX_PREFIX + startTime + "|" + target.getGameProfile().getName()));
        level.addFreshEntity(marker);

        ACTIVE_RITUALS.add(new ReviveRitual(
                level.dimension(), center, target.getUUID(), target.getGameProfile().getName(), marker
        ));

        source.sendSuccess(() -> Component.literal("§bResurrección de §f" + target.getGameProfile().getName() + " §bpreparada."), true);
        broadcast(level, Component.literal("§b✦ Resurrección de §f" + target.getGameProfile().getName() + " §bpreparada."));
        level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 1.5F, 0.72F);
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
                ritual.marker.discard();
                iterator.remove();
                continue;
            }

            ritual.tick++;
            playTimedSounds(level, ritual);

            if (!ritual.revived && ritual.tick >= REVIVE_TICK) {
                revivePlayer(server, level, ritual);
                ritual.revived = true;
            }

            if (ritual.tick >= END_TICK) {
                ritual.marker.discard();
                iterator.remove();
            }
        }
    }

    private static void playTimedSounds(ServerLevel level, ReviveRitual ritual) {
        int t = ritual.tick;
        BlockPos pos = BlockPos.containing(ritual.center);

        if (t == 35 || t == 70 || t == 105) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.MASTER, 1.1F, 0.8F + t / 300.0F);
        }
        if (t == 150) {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.MASTER, 1.5F, 0.65F);
        }
        if (t == 235) {
            level.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.MASTER, 1.5F, 0.8F);
        }
        if (t == 315) {
            level.playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.MASTER, 1.9F, 0.72F);
        }
    }

    private static void revivePlayer(MinecraftServer server, ServerLevel level, ReviveRitual ritual) {
        ServerPlayer target = server.getPlayerList().getPlayer(ritual.targetId);
        Vec3 c = ritual.center;

        level.playSound(null, BlockPos.containing(c), SoundEvents.BEACON_POWER_SELECT, SoundSource.MASTER, 2.0F, 1.2F);

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

    private static void broadcast(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
        }
    }

    private static final class ReviveRitual {
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final UUID targetId;
        private final String targetName;
        private final ArmorStand marker;
        private int tick;
        private boolean revived;

        private ReviveRitual(ResourceKey<Level> dimension, Vec3 center, UUID targetId, String targetName, ArmorStand marker) {
            this.dimension = dimension;
            this.center = center;
            this.targetId = targetId;
            this.targetName = targetName;
            this.marker = marker;
        }
    }
}
