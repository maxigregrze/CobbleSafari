package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.block.trap.TrapBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

public class TrapBlockEntityRenderer implements BlockEntityRenderer<TrapBlockEntity> {

    private final BlockRenderDispatcher blockRenderDispatcher;

    public TrapBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(TrapBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && blockEntity.shouldHideWorldModelForLocalPlayer(mc.player)) {
            return;
        }
        // The block is RenderShape.INVISIBLE, so renderSingleBlock() skips it. Render the baked
        // model directly instead, which honors the blockstate (facing) but ignores the render shape.
        BlockState state = blockEntity.getBlockState();
        BakedModel model = this.blockRenderDispatcher.getBlockModel(state);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
        this.blockRenderDispatcher.getModelRenderer().renderModel(
                poseStack.last(), consumer, state, model, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
    }
}
