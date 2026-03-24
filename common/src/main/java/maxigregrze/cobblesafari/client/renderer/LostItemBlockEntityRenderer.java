package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.block.misc.LostItemBlock;
import maxigregrze.cobblesafari.block.misc.LostItemBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class LostItemBlockEntityRenderer implements BlockEntityRenderer<LostItemBlockEntity> {
    private final BlockRenderDispatcher blockRenderDispatcher;

    public LostItemBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(LostItemBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(LostItemBlock.FACE) || !state.hasProperty(LostItemBlock.FACING)) {
            return;
        }

        UUID localPlayerId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        boolean claimed = localPlayerId != null && blockEntity.hasClaimed(localPlayerId);

        BlockState renderState = ModBlocks.LOST_ITEM_VISUAL.defaultBlockState()
                .setValue(LostItemBlock.FACE, state.getValue(LostItemBlock.FACE))
                .setValue(LostItemBlock.FACING, state.getValue(LostItemBlock.FACING))
                .setValue(LostItemBlock.HAS_ITEM, !claimed);

        poseStack.pushPose();
        blockRenderDispatcher.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
