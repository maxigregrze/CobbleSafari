package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The wormhole's fifteen tinted layers, drawn far to near (action plan 183, section 4.2). Shared by
 * the exit portal's block entity renderer and the warp ride screen's portals (action plan 189,
 * section 6 bis).
 *
 * <p><b>Depth writing.</b> The layers clipped each other at first, and the first fix was
 * {@code entityTranslucentEmissive}, whose write mask is {@code COLOR_WRITE} - it blends without
 * stamping depth. That cured the symptom and caused another: block entities are drawn <em>before</em>
 * the translucent terrain layer, so a portal writing no depth cannot reject the glass, signs and
 * clouds drawn after it, and they paint over it however far behind they are.</p>
 *
 * <p>The real cause of the original clipping was the draw order, fixed separately. The type is
 * therefore {@code entityTranslucent} again - depth-writing - and the order is now measured against
 * the camera every frame ({@link #farToNear}) rather than assumed, which is what makes depth writing
 * safe. Set {@link #DEPTH_SORTED} to false to return to the non-writing type if a viewing angle is
 * ever found where this misbehaves.</p>
 */
public final class UltraWormholeVortex {

    /** The authored spacing between two layers: one model pixel. */
    public static final float BLOCK_LAYER_SPACING = 1.0f / 16.0f;

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

    /** Authored extent: 3 x 3 blocks, i.e. -16..32 in model units, centred on (0.5, 0.5). */
    private static final float MIN = -1.0f;
    private static final float MAX = 2.0f;

    /**
     * Write depth, and sort the layers against the camera to keep that safe. False falls back to the
     * emissive, non-writing type: the layers stop clipping under any order, at the cost of glass and
     * clouds drawing over the portal from behind.
     */
    private static final boolean DEPTH_SORTED = true;

    private UltraWormholeVortex() {}

    private static ResourceLocation layer(int index) {
        return ResourceLocation.fromNamespaceAndPath(
                CobbleSafari.MOD_ID, "textures/block/ultra_wormhole_" + index + ".png");
    }

    /**
     * Draws the stack on the given pose, whose origin is the model origin: the layers span -1..2 on
     * x and y, and layer {@code i} sits at {@code z = i * layerSpacing}.
     *
     * @param tint destination colour, or {@link WarpRideClientTints#NO_TINT}
     * @param alpha whole-stack opacity; the colour itself is baked into the derived textures
     * @param normalTowardFront true to light the stack as facing out of its front (layer 0) side;
     *                          false keeps the authored +Z normal, which the block has always used
     */
    public static void render(PoseStack.Pose pose, MultiBufferSource buffers, int tint, float alpha,
                              float layerSpacing, int packedLight, boolean normalTowardFront) {
        int count = LAYER_TEXTURE.length;
        boolean ascending = farToNear(pose, (count - 1) * layerSpacing);
        float normalZ = normalTowardFront ? -1.0f : 1.0f;
        for (int i = 0; i < count; i++) {
            int layer = ascending ? i : count - 1 - i;
            ResourceLocation texture = UltraWormholeTintCache.tinted(LAYER_TEXTURE[layer], tint);
            VertexConsumer consumer = buffers.getBuffer(DEPTH_SORTED
                    ? RenderType.entityTranslucent(texture)
                    : RenderType.entityTranslucentEmissive(texture));
            emitLayer(consumer, pose, layer * layerSpacing, alpha, packedLight, normalZ);
        }
    }

    /**
     * Which end of the stack is farther from the camera, so the layers can be drawn far to near.
     *
     * <p>Alpha compositing is order-dependent and, once depth is written, so is visibility: drawn
     * near-first, a ring stamps depth and rejects everything behind it - the clipping this renderer
     * started with. A block entity's pose (and the warp ride's eye-space pose) is already relative to
     * the camera, so the length of a transformed point <em>is</em> its distance.</p>
     *
     * @return true when layer 0 is the far end and the loop should count up
     */
    private static boolean farToNear(PoseStack.Pose pose, float lastZ) {
        Matrix4f matrix = pose.pose();
        Vector3f first = matrix.transformPosition(new Vector3f(0.5f, 0.5f, 0.0f));
        Vector3f last = matrix.transformPosition(new Vector3f(0.5f, 0.5f, lastZ));
        return first.lengthSquared() > last.lengthSquared();
    }

    /**
     * One plane on the z slice, with the layer texture mapped across its <b>whole</b> extent.
     *
     * <p>A single quad, not two. The translucent entity types are no-cull - vanilla ships
     * {@code entityTranslucentCull} separately for exactly that reason - so the back face draws for
     * free. A second, mirrored quad blended every texel twice, which on a fifteen-layer stack is
     * thirty passes and a visibly denser vortex than the art intends.</p>
     */
    private static void emitLayer(VertexConsumer consumer, PoseStack.Pose pose, float z, float alpha,
                                  int packedLight, float normalZ) {
        // u runs 0 -> 1 across x, v runs 1 -> 0 up y (texture v grows downward).
        vertex(consumer, pose, MIN, MIN, z, 0.0f, 1.0f, alpha, packedLight, normalZ);
        vertex(consumer, pose, MAX, MIN, z, 1.0f, 1.0f, alpha, packedLight, normalZ);
        vertex(consumer, pose, MAX, MAX, z, 1.0f, 0.0f, alpha, packedLight, normalZ);
        vertex(consumer, pose, MIN, MAX, z, 0.0f, 0.0f, alpha, packedLight, normalZ);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float alpha, int packedLight, float normalZ) {
        consumer.addVertex(pose, x, y, z)
                // White: the colour is already baked into the derived texture, so the vertex
                // colour must not multiply it a second time. Only the alpha varies.
                .setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0f, 0.0f, normalZ);
    }
}
