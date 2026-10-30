package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mimic renderer: when the block is "solid" ({@link CsBossMimicBlock#isSolid}), draws the model of the configured
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
        if (!(state.getBlock() instanceof CsBossMimicBlock) || !CsBossMimicBlock.isSolid(state)) {
            return;
        }

        BlockState resolved = blockEntity.getResolvedMimicState();
        if (resolved == null) {
            resolved = CsBossMimicBlock.targetBlock(blockEntity.getTargetId(state)).defaultBlockState();
        }
        // The Ultra Warp Dive rocks have no baked render at all (their block entity renderer draws
        // them, glint included), so renderSingleBlock would show nothing; every other target keeps
        // the ordinary path, block-entity-backed vanilla blocks included.
        if (!resolved.is(ModBlocks.ULTRAWARPDIVE_ROCKS)) {
            this.blockRenderDispatcher.renderSingleBlock(resolved, poseStack, bufferSource, packedLight, packedOverlay);
        }
        // The props' animated parts (negative core and ring, rocks + glint); no-op for other blocks.
        if (blockEntity.getLevel() != null) {
            float age = blockEntity.getLevel().getGameTime() + partialTick
                    + (blockEntity.getBlockPos().hashCode() & 0xFF);
            UltraWarpDiveModels.renderPlacedParts(resolved, poseStack, bufferSource, age);
        }
    }

    /**
     * A mimicked Ultra Warp Dive prop overflows its cell, like the real one. Only those opt out of
     * culling: mimics come in whole walls, and every other target fits its cell.
     */
    @Override
    public boolean shouldRenderOffScreen(CsBossMimicBlockEntity blockEntity) {
        BlockState resolved = blockEntity.getResolvedMimicState();
        return resolved != null
                && (resolved.is(ModBlocks.ULTRAWARPDIVE_ROCKS) || resolved.is(ModBlocks.ULTRAWARPDIVE_NEGATIVE));
    }
}
