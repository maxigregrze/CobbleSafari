package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.dungeon.UltraWormholeBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * Draws the sub-dungeon's exit portal (action plan 183, section 4.2).
 *
 * <p>The transformations are {@code DistortionPortalBlockEntityRenderer}'s. The geometry is not:
 * the fifteen tinted layers are emitted directly by {@link UltraWormholeVortex} (shared with the
 * warp ride screen since action plan 189) instead of through {@code renderSingleBlock}, because the
 * tint is carried by a <b>derived texture</b> and a derived texture can never live in the block
 * atlas a baked model reads from.</p>
 *
 * <p>{@code renderSingleBlock} would not have worked in any case: it resolves colour through
 * {@code blockColors.getColor(state, null, null, 0)} - no level, no position - so a colour provider
 * could never have read this block entity's {@code typeId}.</p>
 *
 * <p>The model file is therefore not shipped. It stays in {@code .private/} as the reference for
 * the layer table, which is tenable only because that geometry is degenerate: fifteen copies of one
 * rectangle at consecutive depths.</p>
 *
 * <p>Full brightness is kept by feeding {@link LightTexture#FULL_BRIGHT} instead of by using the
 * emissive shader: the portal is a glowing vortex, its block already emits light level 10, and
 * Ultra Space rooms are dark.</p>
 */
public class UltraWormholeBlockEntityRenderer implements BlockEntityRenderer<UltraWormholeBlockEntity> {

    /** Lifts the whole vortex, so it hangs above the block rather than sitting in it. */
    private static final float LIFT = 0.25f;

    /** Tips the disc forward so it looks down on whoever it faces. Negate to tilt the other way. */
    private static final float TILT_DEGREES = 15.0f;

    public UltraWormholeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(UltraWormholeBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float yaw = Mth.rotLerp(partialTick, blockEntity.getPreviousYaw(), blockEntity.getCurrentYaw());
        float spinZ = Mth.rotLerp(partialTick, blockEntity.getPreviousSpinZ(), blockEntity.getCurrentSpinZ());
        int tint = WarpRideClientTints.tintFor(blockEntity.getTypeId());

        poseStack.pushPose();
        poseStack.translate(0.0, LIFT, 0.0);
        // Pivot on the MODEL's own centre, not the distortion portal's (0.5, 1.0, 0.5). The
        // authored elements span -16..32 on both x and y, i.e. -1..2 in block units, so the rings
        // are centred on (0.5, 0.5). Spinning about y = 1.0 would swing them round a half-block
        // circle instead of turning them in place.
        poseStack.translate(0.5, 0.5, 0.5);
        // Yaw first, about the world vertical, so the vortex keeps facing the player. Then the fixed
        // tilt. The spin goes LAST, which is what makes it turn about the tilted facing axis rather
        // than about the authored one - the disc keeps looking down while it rotates.
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(TILT_DEGREES));
        poseStack.mulPose(Axis.ZP.rotationDegrees(spinZ));
        poseStack.translate(-0.5, -0.5, -0.5);

        UltraWormholeVortex.render(poseStack.last(), bufferSource, tint, 1.0f,
                UltraWormholeVortex.BLOCK_LAYER_SPACING, LightTexture.FULL_BRIGHT, false);
        poseStack.popPose();
    }

    /** The portal reaches well beyond its own block, so it must not be culled with it. */
    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen(UltraWormholeBlockEntity blockEntity) {
        return true;
    }
}
