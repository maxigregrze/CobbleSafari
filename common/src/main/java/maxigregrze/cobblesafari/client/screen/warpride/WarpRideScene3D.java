package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

/**
 * One perspective pass inside the GUI (action plan 189, section 3.2). World units are canvas
 * pixels; the eye sits at the origin and looks down -Z, +Y up.
 *
 * <p>The projection is built so that a point at eye-space depth {@code zFor(s)} lands exactly
 * {@code s} times its lateral offset from the centre - the 2D renderer's {@code k / (d + k)} law,
 * to the pixel. Only the models' own thickness gains real perspective.</p>
 */
public final class WarpRideScene3D {

    /** Distance from the eye to the rider plane, in canvas px: ~70° vertical FOV on a 1080 canvas. */
    public static final float FOCAL_PX = 771.0f;
    private static final float NEAR = 1.0f;
    private static final float FAR = 20_000.0f;

    /**
     * Eye-space light directions (towards the light): above and in front, from either side. The
     * inventory lights vanilla offers are authored for the GUI's flipped Y and leave anything facing
     * the eye near the 40 % ambient floor. With these, a face turned to the eye or up is fully lit
     * and the sides keep enough shade to read as volumes.
     */
    private static final Vector3f LIGHT_0 = new Vector3f(0.3f, 1.0f, 0.8f).normalize();
    private static final Vector3f LIGHT_1 = new Vector3f(-0.3f, 1.0f, 0.8f).normalize();

    private WarpRideScene3D() {}

    /**
     * Perspective projection, identity model-view, fresh depth buffer. The caller must flush its
     * {@code GuiGraphics} first: both share the same buffer source.
     */
    public static void begin(int physicalWidth, int physicalHeight, double fit) {
        RenderSystem.backupProjectionMatrix();
        float visibleCanvasHeight = (float) (physicalHeight / fit);
        float fovY = 2.0f * (float) Math.atan((visibleCanvasHeight / 2.0f) / FOCAL_PX);
        Matrix4f projection = new Matrix4f().setPerspective(
                fovY, physicalWidth / (float) Math.max(1, physicalHeight), NEAR, FAR);
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        FogRenderer.setupNoFog();
        RenderSystem.setShaderLights(LIGHT_0, LIGHT_1);
    }

    /** Flush, restore the GUI matrices, and clear depth again so later 2D is never occluded. */
    public static void end(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        Lighting.setupFor3DItems();
    }

    /** Eye-space Z of something drawn at a given depth scale (1 at the rider plane). */
    public static float zFor(double depthScale) {
        return (float) (-FOCAL_PX / Math.max(1.0E-3, depthScale));
    }
}
