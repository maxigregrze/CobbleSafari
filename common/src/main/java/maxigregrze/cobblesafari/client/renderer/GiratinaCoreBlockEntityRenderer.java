package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.distortion.GiratinaCoreBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

public class GiratinaCoreBlockEntityRenderer implements BlockEntityRenderer<GiratinaCoreBlockEntity> {

    private final BlockRenderDispatcher blockRenderDispatcher;

    public GiratinaCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(GiratinaCoreBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float yaw = Mth.rotLerp(partialTick, blockEntity.getPreviousYaw(), blockEntity.getCurrentYaw());

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-0.5, -0.5, -0.5);

        blockRenderDispatcher.renderSingleBlock(
                ModBlocks.GIRATINA_CORE_MOVING.defaultBlockState(),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }
}
