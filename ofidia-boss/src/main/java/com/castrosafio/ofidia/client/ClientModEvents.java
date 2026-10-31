package com.castrosafio.ofidia.client;

import com.castrosafio.ofidia.ModEntities;
import com.castrosafio.ofidia.OfidiaMod;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = OfidiaMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OfidiaModel.LAYER, OfidiaModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.OFIDIA.get(), OfidiaRenderer::new);
        event.registerEntityRenderer(ModEntities.OFIDIA_PART.get(), NoopRenderer::new);
    }
}
