package com.castrosafio.abyssor.client;

import com.castrosafio.abyssor.AbyssorMod;
import com.castrosafio.abyssor.entity.AbyssorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class AbyssorRenderer extends MobRenderer<AbyssorEntity, AbyssorModel> {
    private static final ResourceLocation P1 = ResourceLocation.fromNamespaceAndPath(AbyssorMod.MODID, "textures/entity/abyssor_phase1.png");
    private static final ResourceLocation P2 = ResourceLocation.fromNamespaceAndPath(AbyssorMod.MODID, "textures/entity/abyssor_phase2.png");
    private static final ResourceLocation P3 = ResourceLocation.fromNamespaceAndPath(AbyssorMod.MODID, "textures/entity/abyssor_phase3.png");

    public AbyssorRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssorModel(context.bakeLayer(AbyssorModel.LAYER)), 4.5F);
    }

    @Override protected void scale(AbyssorEntity entity, PoseStack poseStack, float partialTick) {
        float s = entity.getPhase() == 3 ? 1.58F : entity.getPhase() == 2 ? 1.48F : 1.40F;
        poseStack.scale(s, s, s);
    }

    @Override protected int getBlockLightLevel(AbyssorEntity entity, BlockPos pos) {
        return Math.max(12, super.getBlockLightLevel(entity, pos));
    }

    @Override public ResourceLocation getTextureLocation(AbyssorEntity entity) {
        return entity.getPhase() == 3 ? P3 : entity.getPhase() == 2 ? P2 : P1;
    }
}
