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
            dark().addBox(-23, -17, -27, 46, 34, 42, new CubeDeformation(1.0F))
                  .addBox(-19, -12, -39, 38, 24, 24, new CubeDeformation(0.5F))
                  .addBox(-15, -20, -18, 30, 7, 31, new CubeDeformation(0.0F))
                  .addBox(-21, 10, -21, 42, 10, 29, new CubeDeformation(0.0F)),
            PartPose.offset(0, -2, -18));

        head.addOrReplaceChild("brow_glow_l",
            glow().addBox(-1, -3, -9, 6, 6, 15), PartPose.offset(12, -8, -31));
        head.addOrReplaceChild("brow_glow_r",
            glow().mirror().addBox(-5, -3, -9, 6, 6, 15).mirror(false), PartPose.offset(-12, -8, -31));
        head.addOrReplaceChild("eye_l",
            glow().addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(15, -5, -35));
        head.addOrReplaceChild("eye_r",
            glow().addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(-15, -5, -35));
        head.addOrReplaceChild("throat_glow",
            glow().addBox(-10, -1, -2, 20, 4, 24), PartPose.offset(0, 13, -32));

        PartDefinition jaw = head.addOrReplaceChild("jaw",
            dark().addBox(-21, -2, -35, 42, 10, 36, new CubeDeformation(0.5F))
                  .addBox(-17, 7, -30, 34, 7, 30, new CubeDeformation(0.0F)),
            PartPose.offset(0, 14, -2));

        int[] toothX = {-17, -12, -7, -2, 3, 8, 13, 18};
        for (int i = 0; i < toothX.length; i++) {
            int x = toothX[i];
            head.addOrReplaceChild("top_tooth_" + i,
                bone().addBox(-2, 0, -2, 4, 12 + (i % 2) * 3, 5),
                PartPose.offsetAndRotation(x, 10, -38 + (i % 2) * 2, 0.10F, 0, 0));
            jaw.addOrReplaceChild("bottom_tooth_" + i,
                bone().addBox(-2, -12 - (i % 2) * 2, -2, 4, 12 + (i % 2) * 2, 5),
                PartPose.offsetAndRotation(x, 1, -29 + (i % 2) * 2, -0.08F, 0, 0));
        }
        head.addOrReplaceChild("fang_l", bone().addBox(-3, 0, -3, 6, 18, 7), PartPose.offset(19, 8, -28));
        head.addOrReplaceChild("fang_r", bone().addBox(-3, 0, -3, 6, 18, 7), PartPose.offset(-19, 8, -28));

        head.addOrReplaceChild("crest_center",
            bone().addBox(-4, -27, -4, 8, 27, 10), PartPose.offsetAndRotation(0, -15, -3, -0.30F, 0, 0));
        head.addOrReplaceChild("crest_l",
            bone().addBox(-3, -22, -3, 6, 25, 8), PartPose.offsetAndRotation(11, -14, 0, -0.45F, 0.15F, 0.12F));
        head.addOrReplaceChild("crest_r",
            bone().addBox(-3, -22, -3, 6, 25, 8), PartPose.offsetAndRotation(-11, -14, 0, -0.45F, -0.15F, -0.12F));

        PartDefinition hornL1 = head.addOrReplaceChild("horn_l1",
            bone().addBox(-4, -4, -24, 8, 8, 27), PartPose.offsetAndRotation(16, -12, 4, -0.58F, 0.40F, 0.08F));
        PartDefinition hornL2 = hornL1.addOrReplaceChild("horn_l2",
            bone().addBox(-3, -3, -22, 6, 6, 24), PartPose.offsetAndRotation(0, 0, -22, -0.30F, 0.24F, 0.03F));
        hornL2.addOrReplaceChild("horn_l3",
            bone().addBox(-2, -2, -18, 4, 4, 20), PartPose.offsetAndRotation(0, 0, -19, -0.25F, 0.18F, 0));

        PartDefinition hornR1 = head.addOrReplaceChild("horn_r1",
            bone().addBox(-4, -4, -24, 8, 8, 27), PartPose.offsetAndRotation(-16, -12, 4, -0.58F, -0.40F, -0.08F));
        PartDefinition hornR2 = hornR1.addOrReplaceChild("horn_r2",
            bone().addBox(-3, -3, -22, 6, 6, 24), PartPose.offsetAndRotation(0, 0, -22, -0.30F, -0.24F, -0.03F));
        hornR2.addOrReplaceChild("horn_r3",
            bone().addBox(-2, -2, -18, 4, 4, 20), PartPose.offsetAndRotation(0, 0, -19, -0.25F, -0.18F, 0));

        PartDefinition whiskerL = head.addOrReplaceChild("whisker_l",
            dark().addBox(-1, -1, -2, 2, 2, 30), PartPose.offsetAndRotation(18, 5, -32, 0.16F, -0.55F, 0.25F));
        PartDefinition whiskerL2 = whiskerL.addOrReplaceChild("whisker_l2",
            dark().addBox(-1, -1, 0, 2, 2, 28), PartPose.offsetAndRotation(0, 0, 26, 0.25F, -0.18F, 0.12F));
        whiskerL2.addOrReplaceChild("whisker_l_tip",
            glow().addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 0, 28));

        PartDefinition whiskerR = head.addOrReplaceChild("whisker_r",
            dark().addBox(-1, -1, -2, 2, 2, 30), PartPose.offsetAndRotation(-18, 5, -32, 0.16F, 0.55F, -0.25F));
        PartDefinition whiskerR2 = whiskerR.addOrReplaceChild("whisker_r2",
            dark().addBox(-1, -1, 0, 2, 2, 28), PartPose.offsetAndRotation(0, 0, 26, 0.25F, 0.18F, -0.12F));
        whiskerR2.addOrReplaceChild("whisker_r_tip",
            glow().addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 0, 28));

        PartDefinition leftFin = root.addOrReplaceChild("left_fin",
            fin().addBox(0, -4, -10, 36, 8, 26), PartPose.offsetAndRotation(17, 7, 4, 0.10F, -0.20F, -0.38F));
        PartDefinition leftFinOuter = leftFin.addOrReplaceChild("left_fin_outer",
            fin().addBox(0, -3, -13, 46, 6, 30), PartPose.offsetAndRotation(31, 0, 3, 0.03F, -0.20F, -0.30F));
        leftFinOuter.addOrReplaceChild("left_fin_glow",
            glow().addBox(2, -2, -9, 38, 3, 20), PartPose.ZERO);

        PartDefinition rightFin = root.addOrReplaceChild("right_fin",
            fin().mirror().addBox(-36, -4, -10, 36, 8, 26).mirror(false), PartPose.offsetAndRotation(-17, 7, 4, 0.10F, 0.20F, 0.38F));
        PartDefinition rightFinOuter = rightFin.addOrReplaceChild("right_fin_outer",
            fin().mirror().addBox(-46, -3, -13, 46, 6, 30).mirror(false), PartPose.offsetAndRotation(-31, 0, 3, 0.03F, 0.20F, 0.30F));
        rightFinOuter.addOrReplaceChild("right_fin_glow",
            glow().mirror().addBox(-40, -2, -9, 38, 3, 20).mirror(false), PartPose.ZERO);

        PartDefinition parent = root;
        float[] widths = {40, 39, 37, 34, 31, 28, 24, 20, 16, 12};
        float[] heights = {31, 31, 29, 27, 24, 21, 18, 15, 12, 9};
        float[] lengths = {34, 35, 35, 34, 33, 31, 29, 27, 25, 23};

        for (int i = 0; i < 10; i++) {
            float w = widths[i], h = heights[i], l = lengths[i];
            String name = "body" + (i + 1);
            PartDefinition seg = parent.addOrReplaceChild(name,
                dark().addBox(-w / 2, -h / 2, 0, w, h, l, new CubeDeformation(i < 3 ? 0.8F : 0.2F)),
                PartPose.offset(0, i == 0 ? 5 : 0, i == 0 ? 10 : lengths[i - 1] - 3));

            seg.addOrReplaceChild("armor_" + i,
                bone().addBox(-w * 0.44F, -h * 0.42F, 4, w * 0.88F, 4, Math.min(15, l - 4)),
                PartPose.offsetAndRotation(0, -h * 0.20F, 0, -0.08F, 0, 0));
            seg.addOrReplaceChild("glow_l_" + i,
                glow().addBox(0, -2, 0, 4, 4, Math.max(8, l - 8)),
                PartPose.offset(w * 0.40F, 2, 5));
            seg.addOrReplaceChild("glow_r_" + i,
                glow().addBox(-4, -2, 0, 4, 4, Math.max(8, l - 8)),
                PartPose.offset(-w * 0.40F, 2, 5));

            float spineH = Math.max(10, 25 - i * 1.6F);
            seg.addOrReplaceChild("spine_a_" + i,
                bone().addBox(-3, -spineH, -3, 6, spineH, 9),
                PartPose.offsetAndRotation(0, -h * 0.44F, l * 0.25F, -0.35F, 0, 0));
            seg.addOrReplaceChild("spine_b_" + i,
                bone().addBox(-2, -spineH * 0.78F, -2, 4, spineH * 0.78F, 7),
                PartPose.offsetAndRotation(0, -h * 0.44F, l * 0.68F, -0.42F, 0, 0));

            parent = seg;
        }

        PartDefinition tail = parent.addOrReplaceChild("tail_root",
            dark().addBox(-6, -5, 0, 12, 10, 24), PartPose.offset(0, 0, 19));
        tail.addOrReplaceChild("tail_upper",
            fin().addBox(-4, -38, -3, 8, 40, 27), PartPose.offsetAndRotation(0, 0, 12, -0.12F, 0, 0));
        tail.addOrReplaceChild("tail_lower",
            fin().addBox(-4, -2, -3, 8, 34, 25), PartPose.offsetAndRotation(0, 0, 12, 0.12F, 0, 0));
        tail.addOrReplaceChild("tail_glow",
            glow().addBox(-2, -28, 1, 4, 53, 18), PartPose.offset(0, 0, 13));

        return LayerDefinition.create(mesh, 512, 512);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(AbyssorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float phaseSpeed = entity.getPhase() == 3 ? 0.235F : entity.getPhase() == 2 ? 0.195F : 0.155F;
        float wave = ageInTicks * phaseSpeed;

        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.20F + Mth.sin(wave * 0.38F) * 0.030F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.14F + Mth.sin(wave * 0.25F) * 0.025F;

        float roar = (Mth.sin(ageInTicks * (entity.getPhase() == 3 ? 0.31F : 0.19F)) + 1.0F) * 0.5F;
        jaw.xRot = (entity.isNoAi() ? 0.32F : 0.12F) + roar * (entity.getPhase() == 3 ? 0.34F : 0.22F);

        for (int i = 0; i < body.length; i++) {
            float t = i / (float)(body.length - 1);
            body[i].yRot = Mth.sin(wave - i * 0.56F) * (0.045F + t * 0.125F);
            body[i].xRot = Mth.sin(wave * 0.68F - i * 0.41F) * (0.018F + t * 0.035F);
            body[i].zRot = Mth.sin(wave * 0.43F - i * 0.30F) * (0.008F + t * 0.020F);
        }

        float finBeat = Mth.sin(wave * 0.72F) * 0.15F;
        leftFin.zRot = -0.38F - finBeat;
        rightFin.zRot = 0.38F + finBeat;
        leftFin.xRot = 0.10F + Mth.cos(wave * 0.50F) * 0.05F;
        rightFin.xRot = 0.10F + Mth.cos(wave * 0.50F) * 0.05F;

        whiskerL.yRot = -0.55F + Mth.sin(wave * 0.80F) * 0.12F;
        whiskerR.yRot = 0.55F - Mth.sin(wave * 0.80F) * 0.12F;
        whiskerL.xRot = 0.16F + Mth.sin(wave * 0.57F + 0.7F) * 0.08F;
        whiskerR.xRot = 0.16F + Mth.sin(wave * 0.57F + 0.7F) * 0.08F;
    }
}
