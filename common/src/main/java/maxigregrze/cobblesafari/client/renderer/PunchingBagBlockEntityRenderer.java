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
 * Rend le sac (modèle {@code punchingbag_bag}) suspendu sous le bras du sac de frappe.
 * Au clic droit (event {@link maxigregrze.cobblesafari.block.misc.PunchingBagBlock#EVENT_SWING}),
 * il effectue une oscillation amortie en va‑et‑vient autour du point (0.5, 0.375, 0.5),
 * sur l'axe horizontal perpendiculaire à la direction de face du bloc.
 */
public class PunchingBagBlockEntityRenderer implements BlockEntityRenderer<PunchingBagBlockEntity> {

    // Pivot demandé par la spec (coordonnées bloc).
    private static final float PIVOT_X = 0.5f;
    private static final float PIVOT_Y = 0.375f;
    private static final float PIVOT_Z = 0.5f;
    // Le sac « pend » : il est descendu d'un bloc de sorte que le repère 32 px du modèle
    // s'aligne sur le haut de la moitié supérieure (à calibrer en jeu).
    private static final float BAG_Y_OFFSET = -1.0f;

    private static final float MAX_ANGLE_DEG = 22.0f;
    private static final float OMEGA = 0.45f;     // rad/tick
    private static final float TAU = 7.0f;        // amortissement (ticks)
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
        Axis swingAxis = facing.getAxis() == Direction.Axis.Z ? Axis.XP : Axis.ZP;

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
