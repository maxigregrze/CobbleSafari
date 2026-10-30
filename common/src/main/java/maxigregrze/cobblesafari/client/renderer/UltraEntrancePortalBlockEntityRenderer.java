package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.block.dungeon.UltraEntrancePortalBlock;
import maxigregrze.cobblesafari.block.dungeon.UltraEntrancePortalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Draws the device's two authored models (the blockstate's own baked models, rotation included)
 * half a block towards the right cell, so the frame sits on the seam of the two columns
 * (action plan 201 § 13). Translucent, like the block's former render layer.
 */
public class UltraEntrancePortalBlockEntityRenderer implements BlockEntityRenderer<UltraEntrancePortalBlockEntity> {

    public UltraEntrancePortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // no state
    }

    @Override
    public void render(UltraEntrancePortalBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof UltraEntrancePortalBlock) || blockEntity.getLevel() == null) {
            return;
        }
        Direction right = UltraEntrancePortalBlock.rightOf(state.getValue(UltraEntrancePortalBlock.FACING));
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockState upper = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        BlockPos pos = blockEntity.getBlockPos();

        poseStack.pushPose();
        poseStack.translate(right.getStepX() * 0.5, 0.0, right.getStepZ() * 0.5);
        renderCell(dispatcher, poseStack, buffer, state,
                LevelRenderer.getLightColor(blockEntity.getLevel(), pos), packedOverlay);
        poseStack.translate(0.0, 1.0, 0.0);
        renderCell(dispatcher, poseStack, buffer, upper,
                LevelRenderer.getLightColor(blockEntity.getLevel(), pos.above()), packedOverlay);
        poseStack.popPose();
    }

    private static void renderCell(BlockRenderDispatcher dispatcher, PoseStack poseStack, MultiBufferSource buffer,
                                   BlockState state, int light, int overlay) {
        dispatcher.getModelRenderer().renderModel(poseStack.last(), buffer.getBuffer(RenderType.translucent()),
                state, dispatcher.getBlockModel(state), 1.0F, 1.0F, 1.0F, light, overlay);
    }

    /** The frame spans three blocks: never cull it on the main cell alone. */
    @Override
    public boolean shouldRenderOffScreen(UltraEntrancePortalBlockEntity blockEntity) {
        return true;
    }
}
