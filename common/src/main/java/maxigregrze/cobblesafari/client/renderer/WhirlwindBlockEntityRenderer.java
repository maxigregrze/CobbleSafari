package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.block.misc.WhirlwindBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class WhirlwindBlockEntityRenderer implements BlockEntityRenderer<WhirlwindBlockEntity> {

    private final BlockRenderDispatcher blockRenderDispatcher;

    public WhirlwindBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(WhirlwindBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && blockEntity.shouldHideWorldModelForLocalPlayer(mc.player)) {
            return;
        }

        BlockState renderState = ModBlocks.WHIRLWIND_DISPLAY.defaultBlockState();
        this.blockRenderDispatcher.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
