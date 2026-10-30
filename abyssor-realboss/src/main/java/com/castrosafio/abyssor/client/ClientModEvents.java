package com.castrosafio.abyssor.client;

import com.castrosafio.abyssor.AbyssorMod;
import com.castrosafio.abyssor.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AbyssorMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AbyssorModel.LAYER, AbyssorModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ABYSSOR.get(), AbyssorRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSSOR_PART.get(), NoopRenderer::new);
    }
}
