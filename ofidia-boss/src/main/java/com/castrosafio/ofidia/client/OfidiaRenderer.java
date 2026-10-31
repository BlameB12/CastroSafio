package com.castrosafio.ofidia.client;

import com.castrosafio.ofidia.OfidiaMod;
import com.castrosafio.ofidia.entity.OfidiaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class OfidiaRenderer extends MobRenderer<OfidiaEntity, OfidiaModel> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(OfidiaMod.MODID, "textures/entity/ofidia.png");

    public OfidiaRenderer(EntityRendererProvider.Context context) {
        super(context, new OfidiaModel(context.bakeLayer(OfidiaModel.LAYER)), 3.5F);
    }

    @Override
    protected void scale(OfidiaEntity entity, PoseStack poseStack, float partialTick) {
        float scale = entity.getPhase() == 3 ? 1.78F : entity.getPhase() == 2 ? 1.70F : 1.64F;
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected int getBlockLightLevel(OfidiaEntity entity, BlockPos pos) {
        return Math.max(13, super.getBlockLightLevel(entity, pos));
    }

    @Override
    public ResourceLocation getTextureLocation(OfidiaEntity entity) {
        return TEXTURE;
    }
}
