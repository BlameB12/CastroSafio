package com.castrosafio.menu;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MissionNetworking {
    private MissionNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                MissionSyncPayload.TYPE,
                MissionSyncPayload.STREAM_CODEC,
                (payload, context) -> MissionClientData.updateFromJson(payload.json())
        );
    }
}
