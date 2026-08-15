package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.misc.BelltowerBellBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the moving part of the belltower bell ({@code belltower_bell_moving}) under its static
 * yoke, swinging with a damped oscillation after a strike.
 *
 * <p>Same recipe as {@code PunchingBagBlockEntityRenderer}. The swing axis is derived from the
 * struck face rather than from FACING, which is why the block entity stores a Direction and not
 * just a sign.</p>
 */
public class BelltowerBellBlockEntityRenderer implements BlockEntityRenderer<BelltowerBellBlockEntity> {

    /**
     * Pivot imposed by the design: centred in X and Z, 2 pixels below the top of the block - which
     * is exactly the top of the bell body. Not the yoke, despite the yoke sitting lower.
     */
    private static final float PIVOT_X = 0.5f;
    private static final float PIVOT_Y = 0.875f;
    private static final float PIVOT_Z = 0.5f;

    private static final float MAX_ANGLE_DEG = 18.0f;
    private static final float OMEGA = 0.5f; // rad/tick
    private static final float TAU = 9.0f; // damping (ticks)
    private static final float DURATION_TICKS = 60.0f;

    private final BlockRenderDispatcher blockRenderDispatcher;

    public BelltowerBellBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(BelltowerBellBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Direction struck = blockEntity.getRingFace();

        double time = blockEntity.getLevel().getGameTime() + (double) partialTick;
        float angle = swingAngle((float) (time - blockEntity.getLastRingGameTime()));

        // The bell hangs below its pivot, so it must swing *away* from the struck face. Working
        // through the right-hand rule on a point at (0, -h, 0): a strike from the north wants XN
        // positive, from the south XN negative, from the east ZP negative, from the west ZP
        // positive.
        Axis swingAxis = struck.getAxis() == Direction.Axis.Z ? Axis.XN : Axis.ZP;
        if (struck == Direction.SOUTH || struck == Direction.EAST) {
            angle = -angle;
        }

        poseStack.pushPose();

        // Order matters, and it is the reverse of what reads naturally: mulPose right-multiplies,
        // so the *first* transform pushed is the outermost one. The swing therefore goes first, in
        // world space, and the FACING rotation second - otherwise the swing axis would itself be
        // turned by FACING and the bell would tip sideways on half the orientations.
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(swingAxis.rotationDegrees(angle));
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        // Keeps the moving part locked to the static one, which the blockstate turns by FACING.
        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(modelYaw(facing)));
        poseStack.translate(-0.5f, 0.0f, -0.5f);

        BlockState moving = ModBlocks.BELLTOWER_BELL_MOVING.defaultBlockState();
        this.blockRenderDispatcher.renderSingleBlock(moving, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    /**
     * Rotation applied to the moving model for a given FACING. Kept in lockstep with the
     * {@code belltower_bell} blockstate: north = 0, east = 90, south = 180, west = 270 - the same
     * table, and the same {@code Axis.YP} sign, as {@code PokeStatueBlockEntityRenderer}.
     */
    private static float modelYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
    }

    private static float swingAngle(float elapsedTicks) {
        if (elapsedTicks < 0.0f || elapsedTicks > DURATION_TICKS) {
            return 0.0f;
        }
        return (float) (MAX_ANGLE_DEG * Math.sin(elapsedTicks * OMEGA) * Math.exp(-elapsedTicks / TAU));
    }
}
