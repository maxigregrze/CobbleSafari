package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.misc.UnionRoomColoredBlock;
import maxigregrze.cobblesafari.block.misc.UnionRoomSpotlightBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class UnionRoomSpotlightBlockEntityRenderer implements BlockEntityRenderer<UnionRoomSpotlightBlockEntity> {

    private static final float YAW_HALF_PERIOD_TICKS = 180f;
    /** One full pitch cycle (up → down → up) per quarter of a complete Y rotation cycle. */
    private static final float PITCH_PERIOD_TICKS = (2f * YAW_HALF_PERIOD_TICKS) / 4f;

    private static final float PITCH_MIN_DEG = 50f;
    private static final float PITCH_MAX_DEG = 60f;

    /** Bottom-center of the block cell (half X/Z, floor Y). */
    private static final float PIVOT_X = 0.5f;
    private static final float PIVOT_Y = 0f;
    private static final float PIVOT_Z = 0.5f;

    private final BlockRenderDispatcher blockRenderDispatcher;

    public UnionRoomSpotlightBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(UnionRoomSpotlightBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        float t = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() + partialTick : partialTick;

        float halfPeriod = YAW_HALF_PERIOD_TICKS;
        float cycleT = t % (2f * halfPeriod);
        float yawDeg;
        if (cycleT < halfPeriod) {
            yawDeg = (cycleT / halfPeriod) * 360f;
        } else {
            float u = (cycleT - halfPeriod) / halfPeriod;
            yawDeg = 360f - u * 360f;
        }

        float pitchPhase = (t % PITCH_PERIOD_TICKS) / PITCH_PERIOD_TICKS;
        float parabolic = 1.0f - 4.0f * (pitchPhase - 0.5f) * (pitchPhase - 0.5f);
        float pitchDeg = PITCH_MIN_DEG + (PITCH_MAX_DEG - PITCH_MIN_DEG) * parabolic;

        BlockState lightState = ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT.defaultBlockState()
                .setValue(UnionRoomColoredBlock.COLOR, state.getValue(UnionRoomColoredBlock.COLOR));

        poseStack.pushPose();

        // Yaw around world Y through bottom center (0.5, 0, 0.5).
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        // Pitch around world X through the same bottom center.
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg));
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        this.blockRenderDispatcher.renderSingleBlock(lightState, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }
}
