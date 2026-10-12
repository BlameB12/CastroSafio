package com.castrosafio.menu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EditorOpenPayload(String mode) implements CustomPacketPayload {
    public static final Type<EditorOpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CastroSafioMenu.MOD_ID, "open_editor"));
    public static final StreamCodec<ByteBuf, EditorOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EditorOpenPayload::mode,
            EditorOpenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
