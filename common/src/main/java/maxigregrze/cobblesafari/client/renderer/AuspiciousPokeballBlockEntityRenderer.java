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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class AuspiciousPokeballBlockEntityRenderer implements BlockEntityRenderer<AuspiciousPokeballBlockEntity> {
    private static final float ROTATION_DEGREES_PER_TICK = 2.0f;
    /** Amplitude du bob vertical (blocs) ; periode lente */
    private static final float BOB_PHASE_PER_TICK = 0.04f;

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
        float t = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() + partialTick : partialTick;
        float yaw = t * ROTATION_DEGREES_PER_TICK;
        float bob = Mth.sin(t * BOB_PHASE_PER_TICK) * 0.5f;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-0.5, -0.5, -0.5);
        this.blockRenderDispatcher.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
