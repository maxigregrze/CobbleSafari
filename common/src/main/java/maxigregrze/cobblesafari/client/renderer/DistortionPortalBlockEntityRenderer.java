package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.distortion.DistortionPortalBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

public class DistortionPortalBlockEntityRenderer implements BlockEntityRenderer<DistortionPortalBlockEntity> {
    private final BlockRenderDispatcher blockRenderDispatcher;

    public DistortionPortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(DistortionPortalBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float yaw = Mth.rotLerp(partialTick, blockEntity.getPreviousYaw(), blockEntity.getCurrentYaw());
        float spinZ = Mth.rotLerp(partialTick, blockEntity.getPreviousSpinZ(), blockEntity.getCurrentSpinZ());

        poseStack.pushPose();
        poseStack.translate(0.5, 1.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(spinZ));
        poseStack.translate(-0.5, -1.0, -0.5);
        blockRenderDispatcher.renderSingleBlock(
                ModBlocks.DISTORTION_PORTAL_MOVING.defaultBlockState(),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }
}
