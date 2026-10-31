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
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(AbyssorMod.MODID, "abyssor"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart whiskerL;
    private final ModelPart whiskerR;
    private final ModelPart[] body = new ModelPart[10];

    public AbyssorModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.whiskerL = head.getChild("whisker_l");
        this.whiskerR = head.getChild("whisker_r");
        this.leftFin = root.getChild("left_fin");
        this.rightFin = root.getChild("right_fin");
        ModelPart p = root.getChild("body1");
        body[0] = p;
        for (int i = 1; i < body.length; i++) {
            p = p.getChild("body" + (i + 1));
            body[i] = p;
        }
    }

    private static CubeListBuilder dark() { return CubeListBuilder.create().texOffs(0, 0); }
    private static CubeListBuilder bone() { return CubeListBuilder.create().texOffs(256, 0); }
    private static CubeListBuilder glow() { return CubeListBuilder.create().texOffs(384, 0); }
    private static CubeListBuilder fin() { return CubeListBuilder.create().texOffs(0, 256); }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            dark().addBox(-28, -20, -29, 56, 40, 46, new CubeDeformation(1.5F))
                  .addBox(-24, -13, -44, 48, 27, 26, new CubeDeformation(0.8F))
                  .addBox(-21, -24, -17, 42, 9, 35, new CubeDeformation(0.0F))
                  .addBox(-27, 10, -20, 54, 11, 31, new CubeDeformation(0.3F)),
            PartPose.offset(0, -5, -23));

        head.addOrReplaceChild("cheek_l",
            dark().addBox(0, -10, -18, 18, 22, 30, new CubeDeformation(0.7F)),
            PartPose.offsetAndRotation(20, 1, -13, 0.0F, -0.22F, -0.08F));
        head.addOrReplaceChild("cheek_r",
            dark().mirror().addBox(-18, -10, -18, 18, 22, 30, new CubeDeformation(0.7F)).mirror(false),
            PartPose.offsetAndRotation(-20, 1, -13, 0.0F, 0.22F, 0.08F));

        head.addOrReplaceChild("eye_l", glow().addBox(-4, -4, -4, 8, 8, 8), PartPose.offset(18, -7, -39));
        head.addOrReplaceChild("eye_r", glow().addBox(-4, -4, -4, 8, 8, 8), PartPose.offset(-18, -7, -39));
        head.addOrReplaceChild("brow_l", dark().addBox(-2, -4, -12, 11, 8, 17), PartPose.offsetAndRotation(13, -10, -34, 0.0F, -0.18F, -0.18F));
        head.addOrReplaceChild("brow_r", dark().mirror().addBox(-9, -4, -12, 11, 8, 17).mirror(false), PartPose.offsetAndRotation(-13, -10, -34, 0.0F, 0.18F, 0.18F));
        head.addOrReplaceChild("throat_glow", glow().addBox(-13, -2, 0, 26, 5, 28), PartPose.offset(0, 13, -39));

        PartDefinition jaw = head.addOrReplaceChild("jaw",
            dark().addBox(-27, -3, -39, 54, 12, 40, new CubeDeformation(0.9F))
                  .addBox(-22, 8, -34, 44, 9, 34, new CubeDeformation(0.3F)),
            PartPose.offset(0, 16, -3));

        int[] toothX = {-22, -16, -10, -4, 3, 9, 15, 21};
        for (int i = 0; i < toothX.length; i++) {
            int x = toothX[i];
            int extra = (i % 3 == 0) ? 4 : (i % 2) * 2;
            head.addOrReplaceChild("top_tooth_" + i,
                bone().addBox(-2, 0, -2, 4, 15 + extra, 6),
                PartPose.offsetAndRotation(x, 11, -43 + (i % 2), 0.11F, 0, 0));
            jaw.addOrReplaceChild("bottom_tooth_" + i,
                bone().addBox(-2, -15 - extra, -2, 4, 15 + extra, 6),
                PartPose.offsetAndRotation(x, 2, -34 + (i % 2), -0.10F, 0, 0));
        }
        head.addOrReplaceChild("fang_l", bone().addBox(-4, 0, -4, 8, 23, 9), PartPose.offset(24, 7, -31));
        head.addOrReplaceChild("fang_r", bone().addBox(-4, 0, -4, 8, 23, 9), PartPose.offset(-24, 7, -31));

        head.addOrReplaceChild("crest_center", bone().addBox(-4, -28, -4, 8, 29, 10), PartPose.offsetAndRotation(0, -17, -2, -0.34F, 0, 0));
        head.addOrReplaceChild("crest_l", bone().addBox(-3, -24, -3, 6, 26, 8), PartPose.offsetAndRotation(11, -17, 2, -0.47F, 0.18F, 0.12F));
        head.addOrReplaceChild("crest_r", bone().addBox(-3, -24, -3, 6, 26, 8), PartPose.offsetAndRotation(-11, -17, 2, -0.47F, -0.18F, -0.12F));

        PartDefinition hornL1 = head.addOrReplaceChild("horn_l1", bone().addBox(-4, -4, -25, 8, 8, 28), PartPose.offsetAndRotation(19, -14, 6, -0.48F, 0.54F, 0.16F));
        PartDefinition hornL2 = hornL1.addOrReplaceChild("horn_l2", bone().addBox(-3, -3, -23, 6, 6, 25), PartPose.offsetAndRotation(0, 0, -23, -0.25F, 0.30F, 0.10F));
        hornL2.addOrReplaceChild("horn_l3", bone().addBox(-2, -2, -18, 4, 4, 20), PartPose.offsetAndRotation(0, 0, -20, -0.18F, 0.24F, 0.08F));

        PartDefinition hornR1 = head.addOrReplaceChild("horn_r1", bone().addBox(-4, -4, -25, 8, 8, 28), PartPose.offsetAndRotation(-19, -14, 6, -0.48F, -0.54F, -0.16F));
        PartDefinition hornR2 = hornR1.addOrReplaceChild("horn_r2", bone().addBox(-3, -3, -23, 6, 6, 25), PartPose.offsetAndRotation(0, 0, -23, -0.25F, -0.30F, -0.10F));
        hornR2.addOrReplaceChild("horn_r3", bone().addBox(-2, -2, -18, 4, 4, 20), PartPose.offsetAndRotation(0, 0, -20, -0.18F, -0.24F, -0.08F));

        PartDefinition whiskerL = head.addOrReplaceChild("whisker_l", dark().addBox(-1, -1, -2, 2, 2, 34), PartPose.offsetAndRotation(22, 6, -37, 0.15F, -0.62F, 0.28F));
        PartDefinition whiskerL2 = whiskerL.addOrReplaceChild("whisker_l2", dark().addBox(-1, -1, 0, 2, 2, 32), PartPose.offsetAndRotation(0, 0, 30, 0.28F, -0.19F, 0.12F));
        whiskerL2.addOrReplaceChild("whisker_l_tip", glow().addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 0, 31));
        PartDefinition whiskerR = head.addOrReplaceChild("whisker_r", dark().addBox(-1, -1, -2, 2, 2, 34), PartPose.offsetAndRotation(-22, 6, -37, 0.15F, 0.62F, -0.28F));
        PartDefinition whiskerR2 = whiskerR.addOrReplaceChild("whisker_r2", dark().addBox(-1, -1, 0, 2, 2, 32), PartPose.offsetAndRotation(0, 0, 30, 0.28F, 0.19F, -0.12F));
        whiskerR2.addOrReplaceChild("whisker_r_tip", glow().addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 0, 31));

        PartDefinition leftFin = root.addOrReplaceChild("left_fin",
            fin().addBox(0, -5, -15, 58, 10, 34), PartPose.offsetAndRotation(22, 8, -1, 0.08F, -0.16F, -0.44F));
        PartDefinition leftFinOuter = leftFin.addOrReplaceChild("left_fin_outer",
            fin().addBox(0, -4, -18, 72, 8, 40), PartPose.offsetAndRotation(52, 0, 1, 0.02F, -0.18F, -0.34F));
        leftFinOuter.addOrReplaceChild("left_fin_glow", glow().addBox(4, -2, -11, 60, 4, 26), PartPose.ZERO);
        leftFinOuter.addOrReplaceChild("left_fin_tip", fin().addBox(0, -3, -12, 46, 6, 28), PartPose.offsetAndRotation(65, 0, 1, 0.02F, -0.18F, -0.28F));

        PartDefinition rightFin = root.addOrReplaceChild("right_fin",
            fin().mirror().addBox(-58, -5, -15, 58, 10, 34).mirror(false), PartPose.offsetAndRotation(-22, 8, -1, 0.08F, 0.16F, 0.44F));
        PartDefinition rightFinOuter = rightFin.addOrReplaceChild("right_fin_outer",
            fin().mirror().addBox(-72, -4, -18, 72, 8, 40).mirror(false), PartPose.offsetAndRotation(-52, 0, 1, 0.02F, 0.18F, 0.34F));
        rightFinOuter.addOrReplaceChild("right_fin_glow", glow().mirror().addBox(-64, -2, -11, 60, 4, 26).mirror(false), PartPose.ZERO);
        rightFinOuter.addOrReplaceChild("right_fin_tip", fin().mirror().addBox(-46, -3, -12, 46, 6, 28).mirror(false), PartPose.offsetAndRotation(-65, 0, 1, 0.02F, 0.18F, 0.28F));

        PartDefinition parent = root;
        float[] widths  = {54, 53, 50, 46, 40, 34, 28, 23, 18, 13};
        float[] heights = {42, 42, 40, 36, 31, 26, 21, 17, 13, 10};
        float[] lengths = {38, 38, 37, 36, 34, 32, 30, 28, 26, 24};

        for (int i = 0; i < 10; i++) {
            float w = widths[i], h = heights[i], l = lengths[i];
            String name = "body" + (i + 1);
            PartDefinition seg = parent.addOrReplaceChild(name,
                dark().addBox(-w / 2, -h / 2, 0, w, h, l, new CubeDeformation(i < 4 ? 1.0F : 0.25F)),
                PartPose.offset(0, i == 0 ? 4 : 0, i == 0 ? 10 : lengths[i - 1] - 3));

            float armorW = Math.max(6, w * 0.26F);
            seg.addOrReplaceChild("armor_" + i,
                bone().addBox(-armorW / 2, -2, 0, armorW, 4, Math.min(10, l - 8)),
                PartPose.offsetAndRotation(0, -h * 0.48F, l * 0.34F, -0.10F, 0, 0));

            seg.addOrReplaceChild("glow_l_" + i,
                glow().addBox(0, -2, 0, 5, 4, Math.max(9, l - 8)), PartPose.offset(w * 0.40F, 3, 5));
            seg.addOrReplaceChild("glow_r_" + i,
                glow().addBox(-5, -2, 0, 5, 4, Math.max(9, l - 8)), PartPose.offset(-w * 0.40F, 3, 5));

            float spineH = Math.max(9, 23 - i * 1.45F);
            seg.addOrReplaceChild("spine_a_" + i,
                fin().addBox(-3, -spineH, -2, 6, spineH, 9),
                PartPose.offsetAndRotation(0, -h * 0.46F, l * 0.24F, -0.42F, 0, 0));
            seg.addOrReplaceChild("spine_b_" + i,
                fin().addBox(-2, -spineH * 0.72F, -2, 4, spineH * 0.72F, 8),
                PartPose.offsetAndRotation(0, -h * 0.46F, l * 0.67F, -0.46F, 0, 0));

            if (i < 5) {
                float sideLen = 24 - i * 3;
                seg.addOrReplaceChild("side_fin_l_" + i,
                    fin().addBox(0, -2, -5, sideLen, 4, 14),
                    PartPose.offsetAndRotation(w * 0.45F, 1, l * 0.55F, 0.12F, -0.20F, -0.42F));
                seg.addOrReplaceChild("side_fin_r_" + i,
                    fin().mirror().addBox(-sideLen, -2, -5, sideLen, 4, 14).mirror(false),
                    PartPose.offsetAndRotation(-w * 0.45F, 1, l * 0.55F, 0.12F, 0.20F, 0.42F));
            }
            parent = seg;
        }

        PartDefinition tail = parent.addOrReplaceChild("tail_root",
            dark().addBox(-7, -5, 0, 14, 10, 28), PartPose.offset(0, 0, 21));
        tail.addOrReplaceChild("tail_upper", fin().addBox(-5, -47, -4, 10, 49, 34), PartPose.offsetAndRotation(0, 0, 14, -0.10F, 0, 0));
        tail.addOrReplaceChild("tail_lower", fin().addBox(-5, -2, -4, 10, 41, 31), PartPose.offsetAndRotation(0, 0, 14, 0.10F, 0, 0));
        tail.addOrReplaceChild("tail_glow", glow().addBox(-2, -35, 1, 4, 66, 22), PartPose.offset(0, 0, 15));

        return LayerDefinition.create(mesh, 512, 512);
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(AbyssorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float phaseSpeed = entity.getPhase() == 3 ? 0.24F : entity.getPhase() == 2 ? 0.19F : 0.15F;
        float wave = ageInTicks * phaseSpeed;
        head.xRot = headPitch * ((float)Math.PI / 180F) * 0.18F + Mth.sin(wave * 0.50F) * 0.025F;
        head.yRot = netHeadYaw * ((float)Math.PI / 180F) * 0.14F;
        float jawBase = entity.isNoAi() ? 0.30F : 0.16F;
        jaw.xRot = jawBase + (Mth.sin(ageInTicks * (entity.getPhase() == 3 ? 0.33F : 0.21F)) + 1F) * 0.16F;

        for (int i = 0; i < body.length; i++) {
            float progress = i / (float)(body.length - 1);
            float yawAmp = 0.045F + progress * 0.15F;
            float pitchAmp = 0.012F + progress * 0.030F;
            body[i].yRot = Mth.sin(wave - i * 0.56F) * yawAmp;
            body[i].xRot = Mth.sin(wave * 0.72F - i * 0.41F) * pitchAmp;
        }

        float finWave = Mth.sin(wave * 0.82F) * 0.18F;
        leftFin.zRot = -0.44F - finWave;
        rightFin.zRot = 0.44F + finWave;
        leftFin.xRot = 0.08F + Mth.sin(wave * 0.55F) * 0.05F;
        rightFin.xRot = 0.08F + Mth.sin(wave * 0.55F + 0.6F) * 0.05F;

        whiskerL.yRot = -0.62F + Mth.sin(wave * 0.90F) * 0.12F;
        whiskerR.yRot = 0.62F - Mth.sin(wave * 0.90F + 0.5F) * 0.12F;
        whiskerL.xRot = 0.15F + Mth.sin(wave * 0.67F) * 0.08F;
        whiskerR.xRot = 0.15F + Mth.sin(wave * 0.67F + 0.7F) * 0.08F;
    }
}
