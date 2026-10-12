package com.castrosafio.menu;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = CastroSafioMenu.MOD_ID, dist = Dist.CLIENT)
public final class CastroSafioMenuClient {
    private static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.castrosafiomenu.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.castrosafiomenu"
    );

    public CastroSafioMenuClient(IEventBus modBus) {
        modBus.addListener(CastroSafioMenuClient::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(CastroSafioMenuClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(MissionOverlayRenderer::onScreenInit);
        NeoForge.EVENT_BUS.addListener(MissionOverlayRenderer::onMouseClicked);
        NeoForge.EVENT_BUS.addListener(MissionOverlayRenderer::onRender);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_MENU.consumeClick()) {
            if (minecraft.screen instanceof CastroMenuScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new CastroMenuScreen());
            }
        }
    }
}
