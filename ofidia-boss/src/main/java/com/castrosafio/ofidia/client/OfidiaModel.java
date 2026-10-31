package com.castrosafio.ofidia.client;

import com.castrosafio.ofidia.OfidiaMod;
import com.castrosafio.ofidia.entity.OfidiaEntity;
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

public class OfidiaModel extends HierarchicalModel<OfidiaEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(OfidiaMod.MODID, "ofidia"), "main");

    private final ModelPart root;
    private final ModelPart pelvis;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftForearm;
    private final ModelPart rightForearm;
    private final ModelPart tendrilL;
    private final ModelPart tendrilR;
    private final ModelPart tendrilC;
    private final ModelPart[] tail = new ModelPart[6];

    public OfidiaModel(ModelPart root) {
        this.root = root;
        this.pelvis = root.getChild("pelvis");
        this.torso = pelvis.getChild("torso");
        this.head = torso.getChild("head");
        this.jaw = head.getChild("jaw");
        this.leftArm = torso.getChild("left_arm");
        this.rightArm = torso.getChild("right_arm");
        this.leftForearm = leftArm.getChild("left_forearm");
        this.rightForearm = rightArm.getChild("right_forearm");
        this.tendrilL = torso.getChild("tendril_l");
        this.tendrilR = torso.getChild("tendril_r");
        this.tendrilC = torso.getChild("tendril_c");

        ModelPart p = pelvis.getChild("tail1");
        tail[0] = p;
        for (int i = 1; i < tail.length; i++) {
            p = p.getChild("tail" + (i + 1));
            tail[i] = p;
        }
    }

    private static CubeListBuilder dark() { return CubeListBuilder.create().texOffs(0, 0); }
    private static CubeListBuilder bone() { return CubeListBuilder.create().texOffs(128, 0); }
    private static CubeListBuilder glow() { return CubeListBuilder.create().texOffs(0, 128); }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition pelvis = root.addOrReplaceChild("pelvis",
            dark().addBox(-10, -7, -7, 20, 15, 14, new CubeDeformation(0.8F))
                  .addBox(-8, 5, -6, 16, 7, 12, new CubeDeformation(0.2F)),
            PartPose.offset(0, 12, 0));

        PartDefinition torso = pelvis.addOrReplaceChild("torso",
            dark().addBox(-13, -22, -8, 26, 25, 16, new CubeDeformation(0.7F))
                  .addBox(-10, -27, -6, 20, 9, 12, new CubeDeformation(0.3F)),
            PartPose.offsetAndRotation(0, -7, 0, 0.14F, 0, 0));

        for (int i = 0; i < 5; i++) {
            float y = -18 + i * 4.0F;
            float w = 18 - i * 1.4F;
            torso.addOrReplaceChild("rib_l_" + i,
                glow().addBox(-w, -1, -2, w - 1, 2, 4),
                PartPose.offsetAndRotation(-1, y, -7.5F, 0, 0, -0.10F - i * 0.015F));
            torso.addOrReplaceChild("rib_r_" + i,
                glow().addBox(1, -1, -2, w - 1, 2, 4),
                PartPose.offsetAndRotation(1, y, -7.5F, 0, 0, 0.10F + i * 0.015F));
        }
        torso.addOrReplaceChild("heart",
            glow().addBox(-5, -6, -2, 10, 12, 4, new CubeDeformation(0.5F)),
            PartPose.offset(0, -11, -8.5F));

        PartDefinition head = torso.addOrReplaceChild("head",
            bone().addBox(-8, -9, -9, 16, 14, 17, new CubeDeformation(0.5F))
                  .addBox(-6, -6, -16, 12, 9, 10, new CubeDeformation(0.2F))
                  .addBox(-5, 2, -12, 10, 7, 12, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0, -27, -2, -0.12F, 0, 0));

        head.addOrReplaceChild("eye_l",
            glow().addBox(-2, -2, -2, 4, 4, 4), PartPose.offset(6, -3, -11));
        head.addOrReplaceChild("eye_r",
            glow().addBox(-2, -2, -2, 4, 4, 4), PartPose.offset(-6, -3, -11));
        head.addOrReplaceChild("face_core",
            glow().addBox(-4, -3, -2, 8, 7, 3), PartPose.offset(0, 2, -15));

        PartDefinition jaw = head.addOrReplaceChild("jaw",
            dark().addBox(-7, -2, -13, 14, 6, 15, new CubeDeformation(0.3F))
                  .addBox(-5, 3, -11, 10, 4, 12),
            PartPose.offset(0, 5, 0));
        jaw.addOrReplaceChild("mouth_glow",
            glow().addBox(-5, -1, -12, 10, 3, 11), PartPose.offset(0, -1, 0));

        PartDefinition hornL = head.addOrReplaceChild("horn_l",
            bone().addBox(-2, -3, -3, 4, 5, 20),
            PartPose.offsetAndRotation(6, -7, 1, -0.58F, -0.34F, 0.18F));
        hornL.addOrReplaceChild("horn_l_tip",
            bone().addBox(-1, -2, 0, 3, 4, 16),
            PartPose.offsetAndRotation(0, 0, 16, -0.35F, -0.18F, 0.08F));
        PartDefinition hornR = head.addOrReplaceChild("horn_r",
            bone().addBox(-2, -3, -3, 4, 5, 20),
            PartPose.offsetAndRotation(-6, -7, 1, -0.58F, 0.34F, -0.18F));
        hornR.addOrReplaceChild("horn_r_tip",
            bone().addBox(-2, -2, 0, 3, 4, 16),
            PartPose.offsetAndRotation(0, 0, 16, -0.35F, 0.18F, -0.08F));
        head.addOrReplaceChild("horn_center",
            bone().addBox(-2, -2, -3, 4, 4, 19),
            PartPose.offsetAndRotation(0, -9, 3, -0.70F, 0, 0));

        PartDefinition leftArm = torso.addOrReplaceChild("left_arm",
            dark().addBox(-4, -3, -4, 8, 22, 8, new CubeDeformation(0.6F)),
            PartPose.offsetAndRotation(13, -18, 0, 0.18F, 0.05F, -0.34F));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm",
            dark().addBox(-6, 0, -7, 12, 26, 14, new CubeDeformation(0.9F)),
            PartPose.offsetAndRotation(0, 17, 0, -0.18F, 0.05F, -0.08F));
        leftForearm.addOrReplaceChild("left_blade",
            bone().addBox(-3, -1, -23, 6, 7, 26, new CubeDeformation(0.2F)),
            PartPose.offsetAndRotation(1, 21, -2, -0.50F, -0.10F, 0.12F));
        leftForearm.addOrReplaceChild("left_claw_glow",
            glow().addBox(-4, -2, -9, 8, 5, 11), PartPose.offset(0, 24, -3));

        PartDefinition rightArm = torso.addOrReplaceChild("right_arm",
            dark().addBox(-4, -3, -4, 8, 22, 8, new CubeDeformation(0.6F)),
            PartPose.offsetAndRotation(-13, -18, 0, 0.18F, -0.05F, 0.34F));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm",
            dark().addBox(-6, 0, -7, 12, 26, 14, new CubeDeformation(0.9F)),
            PartPose.offsetAndRotation(0, 17, 0, -0.18F, -0.05F, 0.08F));
        rightForearm.addOrReplaceChild("right_blade",
            bone().addBox(-3, -1, -23, 6, 7, 26, new CubeDeformation(0.2F)),
            PartPose.offsetAndRotation(-1, 21, -2, -0.50F, 0.10F, -0.12F));
        rightForearm.addOrReplaceChild("right_claw_glow",
            glow().addBox(-4, -2, -9, 8, 5, 11), PartPose.offset(0, 24, -3));

        PartDefinition tendrilL = torso.addOrReplaceChild("tendril_l",
            dark().addBox(-1, -1, 0, 3, 3, 24),
            PartPose.offsetAndRotation(8, -24, 2, -0.55F, -0.42F, 0.12F));
        PartDefinition tendrilL2 = tendrilL.addOrReplaceChild("tendril_l2",
            dark().addBox(-1, -1, 0, 3, 3, 22),
            PartPose.offsetAndRotation(0, 0, 21, -0.28F, -0.22F, 0.08F));
        tendrilL2.addOrReplaceChild("tip_l",
            glow().addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 0, 22));

        PartDefinition tendrilR = torso.addOrReplaceChild("tendril_r",
            dark().addBox(-2, -1, 0, 3, 3, 24),
            PartPose.offsetAndRotation(-8, -24, 2, -0.55F, 0.42F, -0.12F));
        PartDefinition tendrilR2 = tendrilR.addOrReplaceChild("tendril_r2",
            dark().addBox(-2, -1, 0, 3, 3, 22),
            PartPose.offsetAndRotation(0, 0, 21, -0.28F, 0.22F, -0.08F));
        tendrilR2.addOrReplaceChild("tip_r",
            glow().addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 0, 22));

        PartDefinition tendrilC = torso.addOrReplaceChild("tendril_c",
            dark().addBox(-1, -1, 0, 3, 3, 27),
            PartPose.offsetAndRotation(0, -28, 3, -0.75F, 0, 0));
        PartDefinition tendrilC2 = tendrilC.addOrReplaceChild("tendril_c2",
            dark().addBox(-1, -1, 0, 3, 3, 22),
            PartPose.offsetAndRotation(0, 0, 24, -0.24F, 0, 0));
        tendrilC2.addOrReplaceChild("tip_c",
            glow().addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 0, 22));

        PartDefinition parent = pelvis;
        float[] widths = {18, 17, 15, 13, 10, 7};
        float[] heights = {14, 13, 12, 10, 8, 6};
        float[] lengths = {18, 18, 17, 16, 15, 14};
        for (int i = 0; i < 6; i++) {
            float w = widths[i], h = heights[i], l = lengths[i];
            String name = "tail" + (i + 1);
            PartDefinition seg = parent.addOrReplaceChild(name,
                dark().addBox(-w / 2, -h / 2, 0, w, h, l, new CubeDeformation(0.4F)),
                PartPose.offsetAndRotation(0, i == 0 ? 8 : 0, i == 0 ? 4 : lengths[i - 1] - 2,
                    i == 0 ? 0.42F : 0.12F, 0, 0));
            seg.addOrReplaceChild("tail_glow_" + i,
                glow().addBox(-2, -h * 0.35F, 2, 4, 4, Math.max(6, l - 5)), PartPose.ZERO);
            parent = seg;
        }
        parent.addOrReplaceChild("tail_stinger",
            glow().addBox(-4, -4, 0, 8, 8, 12, new CubeDeformation(0.5F)),
            PartPose.offsetAndRotation(0, 0, 12, -0.32F, 0, 0));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(OfidiaEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        int phase = entity.getPhase();
        float speed = phase == 3 ? 0.20F : phase == 2 ? 0.16F : 0.12F;
        float t = ageInTicks * speed;

        pelvis.yRot = Mth.sin(t * 0.55F) * 0.05F;
        torso.xRot = 0.14F + Mth.sin(t * 0.70F) * 0.025F;
        torso.yRot = Mth.sin(t * 0.45F) * 0.055F;

        head.xRot = -0.12F + headPitch * ((float)Math.PI / 180F) * 0.22F + Mth.sin(t * 0.8F) * 0.035F;
        head.yRot = netHeadYaw * ((float)Math.PI / 180F) * 0.24F;
        jaw.xRot = 0.10F + (Mth.sin(ageInTicks * (phase == 3 ? 0.32F : 0.18F)) + 1.0F) * (phase == 3 ? 0.14F : 0.09F);

        float armWave = Mth.sin(t * 0.9F) * (phase == 3 ? 0.13F : 0.08F);
        leftArm.zRot = -0.34F - armWave;
        rightArm.zRot = 0.34F + armWave;
        leftArm.xRot = 0.18F + Mth.sin(t * 0.62F) * 0.07F;
        rightArm.xRot = 0.18F + Mth.sin(t * 0.62F + 1.4F) * 0.07F;
        leftForearm.xRot = -0.18F + Mth.sin(t * 0.95F + 0.8F) * 0.09F;
        rightForearm.xRot = -0.18F + Mth.sin(t * 0.95F + 2.1F) * 0.09F;

        for (int i = 0; i < tail.length; i++) {
            float strength = 0.07F + i * 0.035F;
            tail[i].yRot = Mth.sin(t - i * 0.60F) * strength;
            tail[i].xRot = (i == 0 ? 0.42F : 0.12F) + Mth.sin(t * 0.72F - i * 0.44F) * (0.025F + i * 0.008F);
        }

        float tendril = phase == 3 ? 0.11F : 0.07F;
        tendrilL.yRot = -0.42F + Mth.sin(t * 1.10F) * tendril;
        tendrilR.yRot = 0.42F - Mth.sin(t * 1.10F + 0.6F) * tendril;
        tendrilC.yRot = Mth.sin(t * 0.85F + 1.2F) * tendril;
    }
}
