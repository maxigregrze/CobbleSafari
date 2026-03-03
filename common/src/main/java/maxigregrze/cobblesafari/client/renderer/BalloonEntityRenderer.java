package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.BalloonEntity;
import maxigregrze.cobblesafari.client.model.BalloonEntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BalloonEntityRenderer extends MobRenderer<BalloonEntity, BalloonEntityModel<BalloonEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/balloon.png");
    private static final ResourceLocation TEXTURE_OPEN =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/balloon_open.png");

    public BalloonEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new BalloonEntityModel<>(context.bakeLayer(BalloonEntityModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BalloonEntity entity) {
        return entity.hasDroppedLoot() ? TEXTURE_OPEN : TEXTURE;
    }

    @Override
    public void render(BalloonEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.7F, 0.7F, 0.7F);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
