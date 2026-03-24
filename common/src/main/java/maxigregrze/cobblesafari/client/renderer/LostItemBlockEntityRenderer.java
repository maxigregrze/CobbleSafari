package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import maxigregrze.cobblesafari.block.misc.LostItemBlock;
import maxigregrze.cobblesafari.block.misc.LostItemBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.UUID;

public class LostItemBlockEntityRenderer implements BlockEntityRenderer<LostItemBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockRenderDispatcher blockRenderDispatcher;

    public LostItemBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(LostItemBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(LostItemBlock.HAS_ITEM)) {
            LOGGER.warn("[LostItemRenderer] State missing HAS_ITEM property!");
            return;
        }

        UUID localPlayerId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        boolean claimedByLocalPlayer = localPlayerId != null && blockEntity.hasClaimed(localPlayerId);
        boolean renderHasItem = !claimedByLocalPlayer;
        
        LOGGER.info("[LostItemRenderer] Rendering for local player - claimed: {}, renderHasItem: {}", claimedByLocalPlayer, renderHasItem);
        
        BlockState renderState = state.setValue(LostItemBlock.HAS_ITEM, renderHasItem);
        
        this.blockRenderDispatcher.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
