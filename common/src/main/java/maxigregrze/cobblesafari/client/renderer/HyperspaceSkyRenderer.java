package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.HyperspaceDimensionEffects;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Sky of Hyperspace Lumiose (action plan 197, section 4). One full-screen quad drawn with the
 * {@code cobblesafari:hyperspace_sky} core shader, which rebuilds each pixel's view direction and
 * computes the whole sky from it.
 *
 * <p>Two ways in, never both in one frame:</p>
 * <ul>
 *   <li>no shader pack: in place of the vanilla sky ({@link #renderInSkyPass});</li>
 *   <li>Iris with a shader pack: after the pipeline, over the pack's final image
 *       ({@link #renderAfterShaderPipeline}). Two copies of the scene depth then tell the shader what
 *       lies under each pixel: the one taken just before translucent terrain
 *       ({@link #capturePreTranslucentDepth}) and the final one. Sky in both is open sky; sky only in
 *       the first is sky seen through water or glass, where the pack has already painted its own sky
 *       behind the surface, so ours is blended there instead of replacing it.</li>
 * </ul>
 */
public final class HyperspaceSkyRenderer {

    public static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "hyperspace_sky");

    /** Every animation in the shader is periodic over this span, so wrapping the clock shows no jump. */
    private static final long TIME_LOOP_MS = 3_600_000L;

    private static final HyperspaceDepthSnapshot PRE_TRANSLUCENT_DEPTH = new HyperspaceDepthSnapshot();
    private static final HyperspaceDepthSnapshot SCENE_DEPTH = new HyperspaceDepthSnapshot();

    private static ShaderInstance shader;
    /** Set by the pre-translucent copy, consumed by the overlay: never draw against last frame's depth. */
    private static boolean preTranslucentCaptured;

    private HyperspaceSkyRenderer() {}

    /** Called by the loader's core shader registration, again on every resource reload. */
    public static void setShader(ShaderInstance instance) {
        shader = instance;
    }

    /**
     * Draws the sky in the vanilla sky slot. Returns {@code true} when it did, so the caller skips the
     * vanilla sky; under a shader pack it leaves the slot to vanilla (which draws nothing here).
     */
    public static boolean renderInSkyPass(Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        if (!isHyperspaceSky() || ShaderCompatHelper.isIrisShaderActive()) {
            return false;
        }
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        draw(frustumMatrix, projectionMatrix, false);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        return true;
    }

    /**
     * Copies the depth of everything opaque, solid hand included: Iris draws it right before the
     * translucent terrain layer, which is where this runs.
     */
    public static void capturePreTranslucentDepth() {
        if (!isOverlayActive()) {
            return;
        }
        PRE_TRANSLUCENT_DEPTH.copyFrom(Minecraft.getInstance().getMainRenderTarget());
        preTranslucentCaptured = true;
    }

    /** Draws the sky once Iris has composited the shader pack's image into the main target. */
    public static void renderAfterShaderPipeline(Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        boolean captured = preTranslucentCaptured;
        preTranslucentCaptured = false;
        if (!captured || !isOverlayActive()) {
            return;
        }

        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        SCENE_DEPTH.copyFrom(main);
        main.bindWrite(false);

        RenderSystem.setShaderTexture(1, PRE_TRANSLUCENT_DEPTH.textureId());
        RenderSystem.setShaderTexture(2, SCENE_DEPTH.textureId());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        draw(frustumMatrix, projectionMatrix, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void draw(Matrix4f frustumMatrix, Matrix4f projectionMatrix, boolean overlay) {
        Matrix4f invViewProj = new Matrix4f(projectionMatrix).mul(frustumMatrix).invert();
        int mapMode = HyperspaceSkyMapTexture.bind(0);

        RenderSystem.setShader(() -> shader);
        shader.safeGetUniform("InvViewProj").set(invViewProj);
        shader.safeGetUniform("CsTime").set((Util.getMillis() % TIME_LOOP_MS) / 1000.0F);
        shader.safeGetUniform("MapMode").set(mapMode);
        shader.safeGetUniform("OverlayMode").set(overlay ? 1 : 0);

        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(-1.0F, -1.0F, 0.0F);
        buffer.addVertex(1.0F, -1.0F, 0.0F);
        buffer.addVertex(1.0F, 1.0F, 0.0F);
        buffer.addVertex(-1.0F, 1.0F, 0.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
    }

    private static boolean isHyperspaceSky() {
        ClientLevel level = Minecraft.getInstance().level;
        return shader != null && level != null && level.effects() instanceof HyperspaceDimensionEffects;
    }

    private static boolean isOverlayActive() {
        return isHyperspaceSky() && ShaderCompatHelper.isIrisShaderActive();
    }
}
