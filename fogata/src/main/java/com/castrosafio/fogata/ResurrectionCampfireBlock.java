package com.castrosafio.fogata;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ResurrectionCampfireBlock extends Block {
    public static final MapCodec<ResurrectionCampfireBlock> CODEC = simpleCodec(ResurrectionCampfireBlock::new);

    public ResurrectionCampfireBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    private static boolean isResurrectionTool(ItemStack stack) {
        return stack.is(FogataMod.CUCHARA.get()) || stack.is(FogataMod.TENEDOR.get());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isResurrectionTool(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("⚠ Necesitas una Cuchara de resurrección o un Tenedor de resurrección para usar esta fogata.")
                        .withStyle(ChatFormatting.RED), false);
                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.7F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            boolean anySpectator = serverPlayer.server.getPlayerList().getPlayers().stream().anyMatch(Player::isSpectator);
            if (!anySpectator) {
                serverPlayer.sendSystemMessage(Component.literal("No hay jugadores muertos conectados en modo espectador.")
                        .withStyle(ChatFormatting.YELLOW));
                return ItemInteractionResult.SUCCESS;
            }

            var requiredItem = stack.getItem();
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new ReviveMenu(id, inventory, serverLevel, pos, hand, requiredItem),
                    Component.literal("¿Revivir?").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
            ));
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal("⚠ Sostén una Cuchara de resurrección o un Tenedor de resurrección.")
                    .withStyle(ChatFormatting.RED), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.7;
            double y = pos.getY() + 1.1 + random.nextDouble() * 0.7;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.7;
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0, 0.01, 0.0);
        }
        if (random.nextInt(7) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    0.0, 0.03, 0.0);
        }
    }
}
