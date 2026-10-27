package com.castrosafio.bossbalance;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CastroSafioBossBalance.MODID)
public final class CastroSafioBossBalance {
    public static final String MODID = "castrosafio_boss_balance";

    public CastroSafioBossBalance(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(new BossBalanceEvents());
    }
}
