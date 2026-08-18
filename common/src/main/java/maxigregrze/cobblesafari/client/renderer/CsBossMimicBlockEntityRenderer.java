package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mimic renderer: when the block is "solid" (active == reverse), draws the model of the configured
 * target block in the state the server resolved for this position - so a run of mimics shows the
 * same connected models a run of real blocks would. Renders nothing when invisible: survival
 * invisibility relies on this, the creative outline coming from {@code getShape}.
 *
 * <p>Purely a consumer of what the server sent. It must never resolve a state of its own: real
 * connected blocks do not recompute client-side (block updates arrive with UPDATE_KNOWN_SHAPE), so a
 * locally derived layer would never be corrected. Until the first update arrives it falls back to
 * the target's default state.
 */
public class CsBossMimicBlockEntityRenderer implements BlockEntityRenderer<CsBossMimicBlockEntity> {

    private final BlockRenderDispatcher blockRenderDispatcher;

    public CsBossMimicBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CsBossMimicBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(CsBossMimicBlock.ACTIVE) || !state.hasProperty(CsBossMimicBlock.REVERSE)) {
            return;
        }
        boolean solid = state.getValue(CsBossMimicBlock.ACTIVE) == state.getValue(CsBossMimicBlock.REVERSE);
        if (!solid) {
            return;
        }

        BlockState resolved = blockEntity.getResolvedMimicState();
        if (resolved == null) {
            resolved = CsBossMimicBlock.targetBlock(blockEntity.getMimicBlockId()).defaultBlockState();
        }
        this.blockRenderDispatcher.renderSingleBlock(resolved, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
