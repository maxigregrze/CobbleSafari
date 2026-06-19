package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mimic renderer: when the block is "solid" (active == reverse), draws the model of
 * the configured target block (copying its texture). Falls back to {@code distortion_stone_bricks}
 * when the id is empty/invalid. Renders nothing when invisible — survival invisibility relies on
 * this, the creative outline coming from {@code getShape}.
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

        BlockState renderState = resolveMimicState(blockEntity.getMimicBlockId());
        this.blockRenderDispatcher.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static BlockState resolveMimicState(String id) {
        if (id != null && !id.isBlank()) {
            ResourceLocation loc = ResourceLocation.tryParse(id.trim());
            if (loc != null) {
                Block block = BuiltInRegistries.BLOCK.getOptional(loc).orElse(null);
                if (block != null) {
                    return block.defaultBlockState();
                }
            }
        }
        return ModBlocks.DISTORTION_STONE_BRICKS.defaultBlockState();
    }
}
