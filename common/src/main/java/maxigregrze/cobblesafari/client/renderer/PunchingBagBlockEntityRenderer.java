package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.misc.PunchingBagBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the bag ({@code punchingbag_bag} model) suspended under the punching bag arm.
 * On right-click (event {@link maxigregrze.cobblesafari.block.misc.PunchingBagBlock#EVENT_SWING}),
 * it performs a damped back-and-forth swing around point (0.5, 0.375, 0.5),
 * on the horizontal axis perpendicular to the block's facing direction.
 */
public class PunchingBagBlockEntityRenderer implements BlockEntityRenderer<PunchingBagBlockEntity> {

    // Pivot specified by the spec (block coordinates).
    private static final float PIVOT_X = 0.5f;
    private static final float PIVOT_Y = 0.375f;
    private static final float PIVOT_Z = 0.5f;
    // The bag "hangs": lowered by one block so the model's 32 px reference frame
    // aligns with the top of the upper half (tune in-game).
    private static final float BAG_Y_OFFSET = -1.0f;

    private static final float MAX_ANGLE_DEG = 22.0f;
    private static final float OMEGA = 0.45f;     // rad/tick
    private static final float TAU = 7.0f;        // damping (ticks)
    private static final float DURATION_TICKS = 50.0f;

    private final BlockRenderDispatcher blockRenderDispatcher;

    public PunchingBagBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(PunchingBagBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        // X and Z rotations are opposite-handed in world space; XN matches ZP swing for N/S facings.
        Axis swingAxis = facing.getAxis() == Direction.Axis.Z ? Axis.XN : Axis.ZP;

        float t = blockEntity.getLevel().getGameTime() + partialTick;
        float angle = swingAngle(t - blockEntity.getLastSwingGameTime());
        if (!blockEntity.isSwingPositive()) {
            angle = -angle;
        }

        poseStack.pushPose();
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(swingAxis.rotationDegrees(angle));
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
        poseStack.translate(0.0f, BAG_Y_OFFSET, 0.0f);

        BlockState bag = ModBlocks.PUNCHINGBAG_BAG_DISPLAY.defaultBlockState();
        this.blockRenderDispatcher.renderSingleBlock(bag, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private static float swingAngle(float elapsedTicks) {
        if (elapsedTicks < 0.0f || elapsedTicks > DURATION_TICKS) {
            return 0.0f;
        }
        return (float) (MAX_ANGLE_DEG * Math.sin(elapsedTicks * OMEGA) * Math.exp(-elapsedTicks / TAU));
    }
}
