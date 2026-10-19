package com.castrosafio.bosscontroller.network;

import com.castrosafio.bosscontroller.control.BossControlManager;
import com.castrosafio.bosscontroller.control.IgnisActionController;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BossNetwork {
    private BossNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BossInputPayload.TYPE, BossInputPayload.STREAM_CODEC, BossNetwork::handleInput);
        registrar.playToServer(BossActionPayload.TYPE, BossActionPayload.STREAM_CODEC, BossNetwork::handleAction);
    }

    private static void handleInput(BossInputPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BossControlManager.updateInput(player, payload);
        }
    }

    private static void handleAction(BossActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            IgnisActionController.handle(player, payload.action());
        }
    }
}
