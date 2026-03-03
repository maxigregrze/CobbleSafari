package maxigregrze.cobblesafari.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.BalloonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class BalloonEntityModel<T extends BalloonEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "balloon"), "main");

    private final ModelPart bb_main;

    public BalloonEntityModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-16.0F, -70.0F, -16.0F, 32.0F, 32.0F, 32.0F, new CubeDeformation(0.0F))
                        .texOffs(216, 0).addBox(-5.0F, -38.0F, -5.0F, 10.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 88).addBox(-17.0F, -57.0F, -17.0F, 34.0F, 6.0F, 34.0F, new CubeDeformation(0.0F))
                        .texOffs(128, 30).addBox(-1.0F, -51.0F, 16.0F, 2.0F, 13.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(128, 44).addBox(-1.0F, -51.0F, -17.0F, 2.0F, 13.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(128, 0).addBox(-17.0F, -51.0F, -1.0F, 1.0F, 13.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(128, 15).addBox(16.0F, -51.0F, -1.0F, 1.0F, 13.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(192, 96).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F))
                        .texOffs(206, 19).addBox(-6.0F, -24.0F, -5.0F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(232, 19).addBox(4.0F, -24.0F, -5.0F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(144, 43).addBox(-5.0F, -24.0F, 4.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(170, 60).addBox(-5.0F, -24.0F, -6.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(188, 75).addBox(-8.5F, -16.5F, -8.5F, 17.0F, 4.0F, 17.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        bb_main.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(104, 12).addBox(0.0F, -7.0F, -1.0F, 0.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(7.0F, -16.0F, -2.0F, 0.0F, 0.0F, -0.1745F));

        bb_main.addOrReplaceChild("cube_r2",
                CubeListBuilder.create()
                        .texOffs(104, 1).addBox(0.0F, -7.0F, -1.0F, 0.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-7.0F, -16.0F, -2.0F, 0.0F, 0.0F, 0.1745F));

        bb_main.addOrReplaceChild("cube_r3",
                CubeListBuilder.create()
                        .texOffs(153, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 19.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -37.2F, -16.4F, 0.6109F, 0.0F, 0.0F));

        bb_main.addOrReplaceChild("cube_r4",
                CubeListBuilder.create()
                        .texOffs(147, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 19.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -37.8F, 15.6F, -0.6109F, 0.0F, 0.0F));

        bb_main.addOrReplaceChild("cube_r5",
                CubeListBuilder.create()
                        .texOffs(141, 0).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 19.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-16.1F, -38.5F, 0.0F, 0.0F, 0.0F, -0.6109F));

        bb_main.addOrReplaceChild("cube_r6",
                CubeListBuilder.create()
                        .texOffs(135, 0).addBox(0.0F, 0.0F, -1.0F, 1.0F, 19.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(16.1F, -38.5F, 0.0F, 0.0F, 0.0F, 0.6109F));

        return LayerDefinition.create(meshdefinition, 256, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float swayAmount = 0.05F;
        float swaySpeed = 0.03F;

        bb_main.yRot = (float) Math.sin(ageInTicks * swaySpeed) * swayAmount;
        bb_main.xRot = (float) Math.cos(ageInTicks * swaySpeed * 0.7F) * swayAmount * 0.5F;
        bb_main.zRot = (float) Math.sin(ageInTicks * swaySpeed * 1.3F) * swayAmount * 0.3F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
