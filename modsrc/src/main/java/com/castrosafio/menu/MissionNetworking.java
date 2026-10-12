package com.castrosafio.menu;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MissionNetworking {
    private MissionNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(
                MissionSyncPayload.TYPE,
                MissionSyncPayload.STREAM_CODEC,
                (payload, context) -> MissionClientData.updateFromJson(payload.json())
        );
        registrar.playToClient(
                CatalogSyncPayload.TYPE,
                CatalogSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientCatalogData.update(payload.mobsJson(), payload.craftsJson())
        );
        registrar.playToClient(
                EditorOpenPayload.TYPE,
                EditorOpenPayload.STREAM_CODEC,
                (payload, context) -> ClientCatalogData.openEditor(payload.mode())
        );
    }
}
