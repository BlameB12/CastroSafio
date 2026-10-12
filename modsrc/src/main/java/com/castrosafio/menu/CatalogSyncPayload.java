package com.castrosafio.menu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CatalogSyncPayload(String mobsJson, String craftsJson) implements CustomPacketPayload {
    public static final Type<CatalogSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CastroSafioMenu.MOD_ID, "catalog_sync"));
    public static final StreamCodec<ByteBuf, CatalogSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CatalogSyncPayload::mobsJson,
            ByteBufCodecs.STRING_UTF8, CatalogSyncPayload::craftsJson,
            CatalogSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
