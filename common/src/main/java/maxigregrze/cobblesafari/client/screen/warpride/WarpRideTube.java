package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/**
 * The ride's tube (action plan 191, V1, revised after the first in-game test): a cloud texture
 * wrapped around the inside of the tube and streaming along it, plus a thin cloud ring every
 * {@link #RING_SPACING_LY} - the "combination" chosen from the previews. Both textures are built
 * in code ({@link WarpRideCloudTextures}). Straight on purpose (Decision 8.4): curvature would sweep
 * far objects across lanes.
 *
 * <p>Same depth law as the lane objects: lateral positions are unscaled, depth goes into eye-space
 * Z through {@link WarpRideScene3D#zFor}, and the projection does the shrinking. Texture coordinates
 * are tied to the scenery's odometer ({@link WarpRideClientState#decorOdometer()}), which runs at
 * the start tunnel's pace through the intro and eases to the ride's speed after it.</p>
 */
public final class WarpRideTube {

    private WarpRideTube() {}

    // --- wall ---
    /** How strongly the wall clouds show: atmosphere, never competing with the obstacles. */
    private static final float WALL_STRENGTH = 0.45f;
    /** The wall texture repeats along the track every this many light-years. */
    private static final double WALL_PERIOD_LY = 64.0;
    /** Violet to blue and back, cycling once per texture period. */
    private static final int[] WALL_COLORS = {0x9A78FF, 0x5A6EFF, 0xBE6EFF};
    /** The wall turns slowly about the axis, in texture widths per second. */
    private static final double WALL_SPIN = 0.004;
    private static final int WALL_SIDES = 48;
    private static final int WALL_STEPS = 24;

    // --- lane seams ---
    private static final int SEAM_COLOR = 0xC0D8FF;
    private static final double SEAM_PERIOD_LY = 4.0;
    private static final double SEAM_DASH_LY = 2.0;
    private static final float SEAM_HALF_WIDTH = 3.0f;
    private static final float SEAM_ALPHA = 0.4f;

    // --- rings ---
    /** road02: magenta, violet, blue, magenta, pink, red (reference section 3). */
    private static final int[] RING_COLORS = {0xFE11FA, 0x7E0FC9, 0x0E1CF3, 0xFD0EFD, 0xFE4DAE, 0xFE2D56};
    /** The texture's white is mixed back into the tint, so the ring reads bright, not muddy. */
    private static final double RING_WHITE = 0.3;
    private static final double RING_SPACING_LY = 16.0;
    private static final float RING_ALPHA = 0.75f;
    /** Ring quad size against the tube radius: the cloud band then lands on the lane floor. */
    private static final float RING_SIZE_FACTOR = 2.3f;
    /** Each ring turns slowly, odd and even ones in opposite directions, radians per second. */
    private static final double RING_SPIN = 0.15;
    /** Golden angle: neighbouring rings never show the same clouds in the same place. */
    private static final double RING_PHASE = 2.399;

    /**
     * @param minDepth nothing nearer than this is drawn: during the intro it is the start tunnel's
     *                 exit, so the tube shows only beyond it, as the tunnel's continuation; use
     *                 {@code Double.NEGATIVE_INFINITY} once the rider is out
     */
    public static void render(MultiBufferSource buffers, WarpRideClientState state, double minDepth) {
        double near = Math.max(state.fifthPlaneDepth, minDepth);
        if (near >= state.visibleDepthLy) {
            return;
        }
        Matrix4f m = new Matrix4f();
        float radius = (float) state.ringFourRadius;
        double seconds = (System.nanoTime() % 3_600_000_000_000L) / 1.0e9;
        // The scenery's own odometer: it streams through the intro at the start tunnel's pace.
        double odometer = state.decorOdometer();

        drawWall(buffers.getBuffer(WarpRideRenderTypes.CLOUD_WALL), m, state, odometer, near, radius, seconds);
        drawSeams(buffers.getBuffer(WarpRideRenderTypes.ADDITIVE), m, state, odometer, near, radius);

        VertexConsumer rings = buffers.getBuffer(WarpRideRenderTypes.CLOUD_RING);
        double first = Math.ceil((odometer + near) / RING_SPACING_LY);
        double last = Math.floor((odometer + state.visibleDepthLy) / RING_SPACING_LY);
        for (double k = last; k >= first; k--) {
            double depth = k * RING_SPACING_LY - odometer;
            float alpha = (float) (WarpRideRenderer.opacity(state, depth) * RING_ALPHA);
            if (alpha <= 0.005f) {
                continue;
            }
            long index = (long) k;
            int color = WarpRideRenderTypes.lerpRgb(
                    RING_COLORS[(int) Math.floorMod(index, (long) RING_COLORS.length)], 0xFFFFFF, RING_WHITE);
            // Its own slow spin, plus the tunnel's swing, so the rings turn with the lanes.
            double spin = seconds * RING_SPIN * ((index & 1L) == 0L ? -1.0 : 1.0)
                    + state.displayLane * Math.PI * 2.0 / WarpRideRenderer.LANE_COUNT;
            drawRing(rings, m, state, depth, radius * RING_SIZE_FACTOR / 2.0f, index * RING_PHASE + spin,
                    color, alpha);
        }
    }

    /** The cloud wall from {@code near} out to the visible depth, faded by the opacity curve. */
    @SuppressWarnings("java:S107")
    private static void drawWall(VertexConsumer vc, Matrix4f m, WarpRideClientState state, double odometer,
                                 double near, float radius, double seconds) {
        double far = state.visibleDepthLy;
        // A slow spin, plus the tunnel's own swing on turns and jumps: one lane is an eighth of a turn.
        float uShift = (float) ((seconds * WALL_SPIN - state.displayLane / WarpRideRenderer.LANE_COUNT) % 1.0);
        for (int step = 0; step < WALL_STEPS; step++) {
            double d0 = near + (far - near) * step / WALL_STEPS;
            double d1 = near + (far - near) * (step + 1) / WALL_STEPS;
            float z0 = WarpRideScene3D.zFor(WarpRideRenderer.depthScale(state, d0));
            float z1 = WarpRideScene3D.zFor(WarpRideRenderer.depthScale(state, d1));
            float a0 = (float) (WarpRideRenderer.opacity(state, d0) * WALL_STRENGTH);
            float a1 = (float) (WarpRideRenderer.opacity(state, d1) * WALL_STRENGTH);
            if (a0 <= 0.002f && a1 <= 0.002f) {
                continue;
            }
            // V runs with the track, so the clouds are fixed to it and stream past with speed.
            float v0 = (float) ((d0 + odometer) / WALL_PERIOD_LY);
            float v1 = (float) ((d1 + odometer) / WALL_PERIOD_LY);
            int c0 = WarpRideRenderTypes.ramp(WALL_COLORS, v0);
            int c1 = WarpRideRenderTypes.ramp(WALL_COLORS, v1);
            for (int i = 0; i < WALL_SIDES; i++) {
                double ang0 = i * Math.PI * 2.0 / WALL_SIDES;
                double ang1 = (i + 1) * Math.PI * 2.0 / WALL_SIDES;
                float x0 = (float) Math.cos(ang0) * radius;
                float y0 = (float) Math.sin(ang0) * radius;
                float x1 = (float) Math.cos(ang1) * radius;
                float y1 = (float) Math.sin(ang1) * radius;
                float u0 = i / (float) WALL_SIDES + uShift;
                float u1 = (i + 1) / (float) WALL_SIDES + uShift;
                WarpRideRenderTypes.vertex(vc, m, x0, y0, z0, u0, v0, c0, a0);
                WarpRideRenderTypes.vertex(vc, m, x1, y1, z0, u1, v0, c0, a0);
                WarpRideRenderTypes.vertex(vc, m, x1, y1, z1, u1, v1, c1, a1);
                WarpRideRenderTypes.vertex(vc, m, x0, y0, z1, u0, v1, c1, a1);
            }
        }
    }

    /**
     * Dashed seams on the lane boundaries, turning with the tunnel and scrolling with the track.
     * They fall between lanes, never on one. Removed with the first cloud revision and brought back
     * at the user's request (plan 191, section 8.5): they keep the lanes readable over the clouds.
     */
    private static void drawSeams(VertexConsumer vc, Matrix4f m, WarpRideClientState state, double odometer,
                                  double near, float radius) {
        double visible = state.visibleDepthLy - SEAM_DASH_LY;
        // Past the rider's plane too, out to the fifth plane, fading like the objects flying by:
        // the lines then run right to the edge of the screen instead of stopping at the rider.
        double first = Math.ceil((odometer + near) / SEAM_PERIOD_LY);
        double last = Math.floor((odometer + visible) / SEAM_PERIOD_LY);
        for (int boundary = 0; boundary < WarpRideRenderer.LANE_COUNT; boundary++) {
            double angle = Math.toRadians(90.0 + 45.0 * (boundary - state.displayLane) + 22.5);
            float cx = (float) (Math.cos(angle) * radius);
            float cy = (float) (-Math.sin(angle) * radius);
            float tx = (float) (-Math.sin(angle) * SEAM_HALF_WIDTH);
            float ty = (float) (-Math.cos(angle) * SEAM_HALF_WIDTH);
            for (double k = last; k >= first; k--) {
                double d1 = k * SEAM_PERIOD_LY - odometer + SEAM_DASH_LY;
                // A dash straddling the exit is cut at it, never drawn inside the start tunnel.
                double d0 = Math.max(d1 - SEAM_DASH_LY, near);
                if (d1 <= d0) {
                    continue;
                }
                float alpha = (float) (WarpRideRenderer.opacity(state, (d0 + d1) / 2.0) * SEAM_ALPHA);
                if (alpha <= 0.005f) {
                    continue;
                }
                float z0 = WarpRideScene3D.zFor(WarpRideRenderer.depthScale(state, d0));
                float z1 = WarpRideScene3D.zFor(WarpRideRenderer.depthScale(state, d1));
                WarpRideRenderTypes.vertex(vc, m, cx - tx, cy - ty, z0, SEAM_COLOR, alpha);
                WarpRideRenderTypes.vertex(vc, m, cx + tx, cy + ty, z0, SEAM_COLOR, alpha);
                WarpRideRenderTypes.vertex(vc, m, cx + tx, cy + ty, z1, SEAM_COLOR, alpha);
                WarpRideRenderTypes.vertex(vc, m, cx - tx, cy - ty, z1, SEAM_COLOR, alpha);
            }
        }
    }

    /** One flat cloud ring facing the rider, turned by {@code angle}. */
    @SuppressWarnings("java:S107")
    private static void drawRing(VertexConsumer vc, Matrix4f m, WarpRideClientState state, double depth,
                                 float halfSize, double angle, int color, float alpha) {
        float z = WarpRideScene3D.zFor(WarpRideRenderer.depthScale(state, depth));
        float cos = (float) Math.cos(angle) * halfSize;
        float sin = (float) Math.sin(angle) * halfSize;
        // Corners of the square, rotated: (-1,-1), (1,-1), (1,1), (-1,1).
        WarpRideRenderTypes.vertex(vc, m, -cos + sin, -sin - cos, z, 0.0f, 0.0f, color, alpha);
        WarpRideRenderTypes.vertex(vc, m, cos + sin, sin - cos, z, 1.0f, 0.0f, color, alpha);
        WarpRideRenderTypes.vertex(vc, m, cos - sin, sin + cos, z, 1.0f, 1.0f, color, alpha);
        WarpRideRenderTypes.vertex(vc, m, -cos - sin, -sin + cos, z, 0.0f, 1.0f, color, alpha);
    }
}
