package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.block.ultraspace.UltraWarpDiveBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;

/**
 * The animated half of the Ultra Warp Dive props (action plan 189, section 4.6): the negative's core
 * and ring around its chunk-baked cage, and the rocks in full - model then blue glint, on the same
 * pose, because the glint's equal-depth test needs the exact matrices of what it covers. The drawing
 * itself lives in {@link UltraWarpDiveModels#renderPlacedParts}, shared with the CSBoss mimic.
 */
public class UltraWarpDiveBlockEntityRenderer implements BlockEntityRenderer<UltraWarpDiveBlockEntity> {

    public UltraWarpDiveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // Nothing to capture: every model comes from UltraWarpDiveModels.
    }

    @Override
    public void render(UltraWarpDiveBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        // Phase offset per block, so two neighbours never spin in lockstep.
        float age = level.getGameTime() + partialTick + (blockEntity.getBlockPos().hashCode() & 0xFF);
        UltraWarpDiveModels.renderPlacedParts(blockEntity.getBlockState(), poseStack, bufferSource, age);
    }

    /** Both models overflow their cell; culling them with it would pop them out at screen edges. */
    @Override
    public boolean shouldRenderOffScreen(UltraWarpDiveBlockEntity blockEntity) {
        return true;
    }
}
