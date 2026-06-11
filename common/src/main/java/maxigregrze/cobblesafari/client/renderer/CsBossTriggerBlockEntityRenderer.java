package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.csboss.CsBossTriggerBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossTriggerBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class CsBossTriggerBlockEntityRenderer implements BlockEntityRenderer<CsBossTriggerBlockEntity> {
    private static final float ROTATION_DEGREES_PER_TICK = 2.0f;

    private final BlockRenderDispatcher blockRenderDispatcher;

    public CsBossTriggerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CsBossTriggerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getBlockState().getValue(CsBossTriggerBlock.ACTIVE)) {
            return;
        }

        BlockState renderState = ModBlocks.BOSSANCHOR_MOVING.defaultBlockState();
        float t = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() + partialTick : partialTick;
        float yaw = t * ROTATION_DEGREES_PER_TICK;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-0.5, -0.5, -0.5);
        this.blockRenderDispatcher.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
