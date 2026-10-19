package com.castrosafio.bosscontroller.network;

import com.castrosafio.bosscontroller.CastroSafioBossController;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BossActionPayload(int action) implements CustomPacketPayload {
    public static final int ACTION_BASIC = 1;
    public static final int ACTION_EXECUTE_SELECTED = 2;
    public static final int ACTION_SELECT_PREV = 3;
    public static final int ACTION_SELECT_NEXT = 4;

    // Compatibility name used by the previous beta.
    public static final int ACTION_ABILITY_1 = ACTION_EXECUTE_SELECTED;

    public static final Type<BossActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CastroSafioBossController.MODID, "boss_action")
    );

    public static final StreamCodec<ByteBuf, BossActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BossActionPayload::action,
            BossActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
