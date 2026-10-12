package com.castrosafio.menu;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CastroSafioMenu.MOD_ID)
public final class CastroSafioMenu {
    public static final String MOD_ID = "castrosafiomenu";

    public CastroSafioMenu(IEventBus modBus) {
        modBus.addListener(MissionNetworking::register);
        NeoForge.EVENT_BUS.addListener(MissionCommands::register);
        NeoForge.EVENT_BUS.addListener(MissionManager::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(MobConfigManager::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(MobConfigManager::onEntityJoin);
    }
}
