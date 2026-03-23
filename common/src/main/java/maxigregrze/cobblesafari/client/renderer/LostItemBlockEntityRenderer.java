package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.block.misc.LostItemBlock;
import maxigregrze.cobblesafari.block.misc.LostItemBlockEntity;
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
        if (!state.hasProperty(LostItemBlock.HAS_ITEM)) {
            return;
        }

        UUID localPlayerId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        boolean claimedByLocalPlayer = localPlayerId != null && blockEntity.hasClaimed(localPlayerId);
        boolean renderHasItem = state.getValue(LostItemBlock.HAS_ITEM) && !claimedByLocalPlayer;
        BlockState renderState = state.setValue(LostItemBlock.HAS_ITEM, renderHasItem);

        this.blockRenderDispatcher.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
