package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class AuspiciousPokeballBlockEntityRenderer implements BlockEntityRenderer<AuspiciousPokeballBlockEntity> {
    /** Amplitude du bob vertical (blocs) ; vitesse et periode dans FloatingBallAnimation */
    private static final float BOB_AMPLITUDE = 0.5f;

    private final BlockRenderDispatcher blockRenderDispatcher;

    public AuspiciousPokeballBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(AuspiciousPokeballBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && blockEntity.shouldHideWorldModelForLocalPlayer(mc.player)) {
            return;
        }

        BlockState renderState = blockEntity.getBlockState().is(ModBlocks.AUSPICIOUS_POKEBALL_SMALL)
                ? ModBlocks.AUSPICIOUS_POKEBALL_SMALL_DISPLAY.defaultBlockState()
                : blockEntity.displayRenderBlock().defaultBlockState();
        double t = FloatingBallAnimation.time(blockEntity, partialTick);
        float yaw = FloatingBallAnimation.yawDegrees(t);
        float bob = FloatingBallAnimation.bobWave(t) * BOB_AMPLITUDE;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-0.5, -0.5, -0.5);
        this.blockRenderDispatcher.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
