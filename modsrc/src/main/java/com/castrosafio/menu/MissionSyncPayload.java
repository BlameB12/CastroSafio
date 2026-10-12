package com.castrosafio.menu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MissionSyncPayload(String json) implements CustomPacketPayload {
    public static final Type<MissionSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CastroSafioMenu.MOD_ID, "mission_sync"));

    public static final StreamCodec<ByteBuf, MissionSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    MissionSyncPayload::json,
                    MissionSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
