package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Untextured, vertex-coloured geometry for the ride's procedural visuals - the start tunnel, the
 * tube and the space background (action plan 191, section 7.3). No texture is involved anywhere:
 * every gradient is the GPU interpolating between vertex colours.
 *
 * <p>Both types draw both faces, test depth without writing it, and work in the 2D pass and in the
 * perspective passes alike.</p>
 */
public final class WarpRideRenderTypes {

    private WarpRideRenderTypes() {}

    /** Standard alpha blending. */
    public static final RenderType TRANSLUCENT = colorType("cobblesafari_warpride_translucent", false);

    /** Additive blending: light adds up, so a glow on black stays a glow and never darkens. */
    public static final RenderType ADDITIVE = colorType("cobblesafari_warpride_additive", true);

    /** The tube's cloud wall: the generated texture, tinted by vertex colour, added on top. */
    public static final RenderType CLOUD_WALL = texturedAdditive("cobblesafari_warpride_cloud_wall",
            WarpRideCloudTextures::wall);

    /** The tube's flat cloud rings, also used in the intro's start tunnel. */
    public static final RenderType CLOUD_RING = texturedAdditive("cobblesafari_warpride_cloud_ring",
            WarpRideCloudTextures::ring);

    /** The texture is read at draw time: authored or generated, whichever is in use. */
    // RenderType is abstract without abstract members: an empty anonymous subclass is enough.
    private static RenderType texturedAdditive(String name,
                                               java.util.function.Supplier<net.minecraft.resources.ResourceLocation> texture) {
        return new RenderType(name, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS,
                1 << 16, false, false,
                () -> {
                    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                    RenderSystem.setShaderTexture(0, texture.get());
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    RenderSystem.disableCull();
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(GL11.GL_LEQUAL);
                    RenderSystem.depthMask(false);
                },
                () -> {
                    RenderSystem.depthMask(true);
                    RenderSystem.enableCull();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableBlend();
                }) {
        };
    }

    /** One textured vertex: position through {@code m}, texture coordinates, then a tint. */
    @SuppressWarnings("java:S107")
    public static void vertex(VertexConsumer consumer, Matrix4f m, float x, float y, float z,
                              float u, float v, int rgb, float a) {
        consumer.addVertex(m, x, y, z).setUv(u, v).setColor(
                ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f, a);
    }

    // RenderType is abstract without abstract members: an empty anonymous subclass is enough.
    private static RenderType colorType(String name, boolean additive) {
        return new RenderType(name, DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
                1 << 16, false, false,
                () -> {
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    RenderSystem.enableBlend();
                    if (additive) {
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    } else {
                        RenderSystem.defaultBlendFunc();
                    }
                    RenderSystem.disableCull();
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(GL11.GL_LEQUAL);
                    RenderSystem.depthMask(false);
                },
                () -> {
                    RenderSystem.depthMask(true);
                    RenderSystem.enableCull();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableBlend();
                }) {
        };
    }

    /** One vertex: position through {@code m}, then a straight-alpha colour. */
    public static void vertex(VertexConsumer consumer, Matrix4f m, float x, float y, float z,
                              float r, float g, float b, float a) {
        consumer.addVertex(m, x, y, z).setColor(r, g, b, a);
    }

    /** {@code argb}'s colour channels with an explicit alpha. */
    public static void vertex(VertexConsumer consumer, Matrix4f m, float x, float y, float z, int rgb, float a) {
        consumer.addVertex(m, x, y, z).setColor(
                ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f, a);
    }

    /** Linear blend of two RGB colours, {@code t} in 0..1. */
    public static int lerpRgb(int from, int to, double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * c);
        int g = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * c);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * c);
        return (r << 16) | (g << 8) | b;
    }

    /** A cyclic colour ramp: {@code t} wraps, and the last colour blends back into the first. */
    public static int ramp(int[] colors, double t) {
        double f = (t - Math.floor(t)) * colors.length;
        int i = (int) Math.floor(f) % colors.length;
        return lerpRgb(colors[i], colors[(i + 1) % colors.length], f - Math.floor(f));
    }
}
