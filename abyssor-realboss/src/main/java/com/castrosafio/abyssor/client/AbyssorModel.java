package com.castrosafio.abyssor.client;

import com.castrosafio.abyssor.AbyssorMod;
import com.castrosafio.abyssor.entity.AbyssorEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AbyssorModel extends HierarchicalModel<AbyssorEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AbyssorMod.MODID, "abyssor"), "main");
    private final ModelPart root, head, jaw, leftFin, rightFin;
    private final ModelPart[] body = new ModelPart[8];

    public AbyssorModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.leftFin = root.getChild("left_fin");
        this.rightFin = root.getChild("right_fin");
        ModelPart p = root.getChild("body1");
        body[0] = p;
        for (int i = 1; i < body.length; i++) {
            p = p.getChild("body" + (i + 1));
            body[i] = p;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-20, -15, -31, 40, 30, 40, new CubeDeformation(0))
            .texOffs(0, 78).addBox(-16, -10, -39, 32, 20, 16, new CubeDeformation(0))
            .texOffs(160, 0).addBox(-4, -27, -7, 8, 18, 12, new CubeDeformation(0)), PartPose.offset(0, 4, -8));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 120).addBox(-17, -2, -34, 34, 9, 29), PartPose.offset(0, 11, -1));
        head.addOrReplaceChild("eye_l", CubeListBuilder.create().texOffs(128, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(14, -6, -31));
        head.addOrReplaceChild("eye_r", CubeListBuilder.create().texOffs(128, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(-14, -6, -31));
        head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(160, 0).addBox(-3, -3, -22, 6, 6, 24), PartPose.offsetAndRotation(13, -15, -3, -0.75F, 0.42F, 0));
        head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(160, 0).addBox(-3, -3, -22, 6, 6, 24), PartPose.offsetAndRotation(-13, -15, -3, -0.75F, -0.42F, 0));
        root.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(0, 170).addBox(0, -3, -8, 40, 5, 18), PartPose.offsetAndRotation(15, 6, 13, 0, -0.15F, -0.28F));
        root.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(0, 170).mirror().addBox(-40, -3, -8, 40, 5, 18).mirror(false), PartPose.offsetAndRotation(-15, 6, 13, 0, 0.15F, 0.28F));

        PartDefinition parent = root;
        float[] ws = {36, 34, 31, 28, 25, 21, 17, 12};
        float[] hs = {28, 27, 25, 23, 20, 17, 14, 10};
        float[] ls = {36, 36, 35, 34, 32, 30, 28, 25};
        for (int i = 0; i < 8; i++) {
            float w = ws[i], h = hs[i], l = ls[i];
            String name = "body" + (i + 1);
            PartDefinition seg = parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 0).addBox(-w / 2, -h / 2, 0, w, h, l), PartPose.offset(0, i == 0 ? 5 : 0, i == 0 ? 6 : ls[i - 1] - 2));
            seg.addOrReplaceChild("spine" + (i + 1), CubeListBuilder.create().texOffs(160, 0).addBox(-3, -18, 5, 6, 18, 10), PartPose.ZERO);
            parent = seg;
        }
        parent.addOrReplaceChild("tail_fin", CubeListBuilder.create().texOffs(0, 200).addBox(-24, -3, -2, 48, 6, 24), PartPose.offset(0, 0, 20));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override public ModelPart root() { return root; }

    @Override public void setupAnim(AbyssorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float speed = entity.getPhase() == 3 ? 0.22F : entity.getPhase() == 2 ? 0.18F : 0.14F;
        float wave = ageInTicks * speed;
        head.xRot = headPitch * ((float)Math.PI / 180F) * 0.25F + Mth.sin(wave * 0.55F) * 0.035F;
        head.yRot = netHeadYaw * ((float)Math.PI / 180F) * 0.18F;
        jaw.xRot = (entity.isNoAi() ? 0.18F : 0.08F) + (Mth.sin(ageInTicks * (entity.getPhase() == 3 ? 0.30F : 0.19F)) + 1F) * 0.11F;
        for (int i = 0; i < body.length; i++) {
            body[i].yRot = Mth.sin(wave - i * 0.62F) * (0.055F + i * 0.018F);
            body[i].xRot = Mth.sin(wave * 0.72F - i * 0.38F) * (0.018F + i * 0.004F);
        }
        float fin = Mth.sin(wave * 0.9F) * 0.12F;
        leftFin.zRot = -0.28F - fin;
        rightFin.zRot = 0.28F + fin;
    }
}
