package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.hyperspace.HyperspaceTrashcanBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the trashcan lid ({@code hyperspace_trash_lid_display}) hinged at (8/16, 14/16, 3/16) on the
 * X axis. On open (event {@link maxigregrze.cobblesafari.block.hyperspace.HyperspaceTrashcanBlock#EVENT_OPEN})
 * the lid swings up ~90° (ease-out), holds ~1 s, then falls back with increasing speed.
 */
public class HyperspaceTrashcanBlockEntityRenderer implements BlockEntityRenderer<HyperspaceTrashcanBlockEntity> {

    private static final float PIVOT_X = 8.0f / 16.0f;
    private static final float PIVOT_Y = 14.0f / 16.0f;
    private static final float PIVOT_Z = 3.0f / 16.0f;

    private static final float OPEN_ANGLE = 90.0f;
    private static final float OPEN_TICKS = 4.0f;
    private static final float HOLD_TICKS = 20.0f;
    private static final float FALL_TICKS = 12.0f;

    private final BlockRenderDispatcher blockRenderDispatcher;

    public HyperspaceTrashcanBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(HyperspaceTrashcanBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        double now = blockEntity.getLevel().getGameTime() + (double) partialTick;
        float elapsed = (float) (now - blockEntity.getLastOpenGameTime());
        float angle = lidAngle(elapsed);

        poseStack.pushPose();
        // Orient the lid model with the block facing (matches the blockstate y rotation).
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRotation(facing)));
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        // Hinge rotation around the lid pivot.
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(Axis.XN.rotationDegrees(angle));
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        BlockState lid = ModBlocks.HYPERSPACE_TRASHCAN_LID_DISPLAY.defaultBlockState();
        this.blockRenderDispatcher.renderSingleBlock(lid, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static float yRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
    }

    private static float lidAngle(float t) {
        if (t < 0.0f) {
            return 0.0f;
        }
        if (t < OPEN_TICKS) {
            float p = t / OPEN_TICKS;
            return OPEN_ANGLE * (1.0f - (1.0f - p) * (1.0f - p)); // ease-out
        }
        if (t < OPEN_TICKS + HOLD_TICKS) {
            return OPEN_ANGLE;
        }
        float fall = t - OPEN_TICKS - HOLD_TICKS;
        if (fall < FALL_TICKS) {
            float p = fall / FALL_TICKS;
            return OPEN_ANGLE * (1.0f - p * p); // accelerating fall
        }
        return 0.0f;
    }
}
