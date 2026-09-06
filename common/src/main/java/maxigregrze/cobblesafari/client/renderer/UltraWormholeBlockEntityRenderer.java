package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.dungeon.UltraWormholeBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws the sub-dungeon's exit portal (action plan 183, section 4.2).
 *
 * <p>The transformations are {@code DistortionPortalBlockEntityRenderer}'s. The geometry is not:
 * this renderer emits its fifteen quads directly instead of calling {@code renderSingleBlock},
 * because the tint is carried by a <b>derived texture</b> and a derived texture can never live in
 * the block atlas a baked model reads from.</p>
 *
 * <p>{@code renderSingleBlock} would not have worked in any case: it resolves colour through
 * {@code blockColors.getColor(state, null, null, 0)} - no level, no position - so a colour provider
 * could never have read this block entity's {@code typeId}.</p>
 *
 * <p>The model file is therefore not shipped. It stays in {@code .private/} as the reference for
 * the table below, which is tenable only because that geometry is degenerate: fifteen copies of one
 * rectangle at consecutive depths.</p>
 *
 * <p><b>Depth writing, and why it came back.</b> The layers clipped each other at first, and the
 * first fix was {@code entityTranslucentEmissive}, whose write mask is {@code COLOR_WRITE} - it
 * blends without stamping depth. That cured the symptom and caused another: block entities are
 * drawn <em>before</em> the translucent terrain layer, so a portal writing no depth cannot reject
 * the glass, signs and clouds drawn after it, and they paint over it however far behind they are.
 *
 * <p>The real cause of the original clipping was the draw order, fixed separately. The type is
 * therefore {@code entityTranslucent} again - depth-writing - and the order is now measured against
 * the camera every frame ({@link #farToNear}) rather than assumed, which is what makes depth writing
 * safe. Set {@link #DEPTH_SORTED} to false to return to the non-writing type if a viewing angle is
 * ever found where this misbehaves.</p>
 *
 * <p>Full brightness is kept by feeding {@link LightTexture#FULL_BRIGHT} instead of by using the
 * emissive shader: the portal is a glowing vortex, its block already emits light level 10, and
 * Ultra Space rooms are dark.</p>
 */
public class UltraWormholeBlockEntityRenderer implements BlockEntityRenderer<UltraWormholeBlockEntity> {

    /**
     * Layer to texture, front (z = 0) to back (z = 14). The authored ramp runs from the sparse,
     * near-white speck at the front to the dense grey ring crossed by white spokes at the back;
     * layers 8 and 9 share {@code _5}, exactly as the source model did.
     */
    private static final ResourceLocation[] LAYER_TEXTURE = {
            layer(13), layer(12), layer(11), layer(10), layer(9),
            layer(8), layer(7), layer(6), layer(5), layer(5),
            layer(4), layer(3), layer(2), layer(1), layer(0)
    };

    /** Authored extent: 3 x 3 blocks, i.e. -16..32 in model units. */
    private static final float MIN = -1.0f;
    private static final float MAX = 2.0f;

    /**
     * Write depth, and sort the layers against the camera to keep that safe. False falls back to the
     * emissive, non-writing type: the layers stop clipping under any order, at the cost of glass and
     * clouds drawing over the portal from behind.
     */
    private static final boolean DEPTH_SORTED = true;

    /** Lifts the whole vortex, so it hangs above the block rather than sitting in it. */
    private static final float LIFT = 0.25f;

    /** Tips the disc forward so it looks down on whoever it faces. Negate to tilt the other way. */
    private static final float TILT_DEGREES = 15.0f;

    private static final float LAST_Z = (LAYER_TEXTURE.length - 1) / 16.0f;

    private static ResourceLocation layer(int index) {
        return ResourceLocation.fromNamespaceAndPath(
                CobbleSafari.MOD_ID, "textures/block/ultra_wormhole_" + index + ".png");
    }

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

        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;
        boolean ascending = farToNear(pose);
        for (int i = 0; i < LAYER_TEXTURE.length; i++) {
            int layer = ascending ? i : LAYER_TEXTURE.length - 1 - i;
            ResourceLocation texture = UltraWormholeTintCache.tinted(LAYER_TEXTURE[layer], tint);
            VertexConsumer consumer = bufferSource.getBuffer(DEPTH_SORTED
                    ? RenderType.entityTranslucent(texture)
                    : RenderType.entityTranslucentEmissive(texture));
            emitLayer(consumer, pose, layer / 16.0f, light);
        }
        poseStack.popPose();
    }

    /**
     * Which end of the stack is farther from the camera, so the layers can be drawn far to near.
     *
     * <p>Alpha compositing is order-dependent and, once depth is written, so is visibility: drawn
     * near-first, a ring stamps depth and rejects everything behind it - the clipping this renderer
     * started with. The portal yaws to face the player so the answer is stable in practice, but it
     * is measured rather than assumed: a block entity's pose is already relative to the camera, so
     * the length of a transformed point <em>is</em> its distance.</p>
     *
     * @return true when layer 0 is the far end and the loop should count up
     */
    private static boolean farToNear(PoseStack.Pose pose) {
        Matrix4f matrix = pose.pose();
        Vector3f first = matrix.transformPosition(new Vector3f(0.5f, 0.5f, 0.0f));
        Vector3f last = matrix.transformPosition(new Vector3f(0.5f, 0.5f, LAST_Z));
        return first.lengthSquared() > last.lengthSquared();
    }

    /**
     * One plane on the z slice, with the layer texture mapped across its <b>whole</b> extent.
     *
     * <p>A single quad, not two. The emissive translucent type is no-cull - vanilla ships
     * {@code entityTranslucentCull} separately for exactly that reason - so the back face draws for
     * free. A second, mirrored quad blended every texel twice, which on a fifteen-layer stack is
     * thirty passes and a visibly denser vortex than the art intends.</p>
     */
    private static void emitLayer(VertexConsumer consumer, PoseStack.Pose pose, float z, int packedLight) {
        // u runs 0 -> 1 across x, v runs 1 -> 0 up y (texture v grows downward).
        vertex(consumer, pose, MIN, MIN, z, 0.0f, 1.0f, packedLight);
        vertex(consumer, pose, MAX, MIN, z, 1.0f, 1.0f, packedLight);
        vertex(consumer, pose, MAX, MAX, z, 1.0f, 0.0f, packedLight);
        vertex(consumer, pose, MIN, MAX, z, 0.0f, 0.0f, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int packedLight) {
        consumer.addVertex(pose, x, y, z)
                // White: the colour is already baked into the derived texture, so the vertex
                // colour must not multiply it a second time.
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0f, 0.0f, 1.0f);
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
