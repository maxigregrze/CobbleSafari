package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * The start tunnel the rider flies out of during the intro (action plan 191, T1): the source's
 * four nested open cylinders ({@code Rail::InitialPart}), rebuilt from colour values only. No
 * texture: the flowing walls, clouds, grid and sparkles are vertex colours computed each frame.
 *
 * <p>Tunnel space runs along its axis, +forward; the rider advances at {@link #INTRO_SPEED_PX}.
 * World Z is {@code riderZ - (tunnelZ - riderPos)}.</p>
 *
 * <p>The tunnel is <b>endless</b> in both directions: the intro camera looks back past the rider,
 * and the source's finite tube left an open end - an empty void - behind it. Everything here is
 * tunnel-anchored and periodic, so it is simply drawn out to the far plane each way, and each end
 * is closed by a glowing cap so the cut never shows.</p>
 */
public final class WarpRideStartTunnel {

    /** Source units onto canvas pixels: the same factor as the intro camera. */
    private static final float PX_PER_UNIT = WarpRideScene3D.FOCAL_PX / 1250.0f;

    private static final float RADIUS = 3000.0f * PX_PER_UNIT;
    /**
     * How fast the tunnel streams past: the source's speed at full energy (190 units a frame at
     * 30 fps), tripled after the second in-game test - at the source's own pace the grid tube still
     * read as drifting. The wall's colour ramp and cloud cells are stretched by the same factor
     * along the tunnel, so only the motion speeds up, never the colour flicker.
     */
    private static final float SPEED_BOOST = 3.0f;
    private static final float INTRO_SPEED_PX = 190.0f * 30.0f * PX_PER_UNIT * SPEED_BOOST;

    /**
     * The intro's speed in light-years per second for the ride's depth law, which is linear in Z:
     * one light-year is {@code FOCAL / k} px. The main tube scrolls at this speed during the intro,
     * so it continues the start tunnel's motion seamlessly at the exit.
     */
    public static double introSpeedLy(double projectionK) {
        return INTRO_SPEED_PX / (WarpRideScene3D.FOCAL_PX / projectionK);
    }

    /**
     * Depth, in light-years in front of the rider, of the tunnel's exit at intro time
     * {@code seconds}: everything of the ride's world nearer than this is still inside the
     * tunnel and must not be drawn; negative once the rider is out.
     */
    public static double exitDepthLy(double seconds, double exitSeconds, double projectionK) {
        return (exitSeconds - seconds) * introSpeedLy(projectionK);
    }

    /**
     * World Z reach each way. The intro eye ranges over Z = -1 200 .. +1 700, so +/-17 500 stays
     * inside the projection's 20 000 far plane from every camera position.
     */
    private static final float WORLD_Z_NEAR = 17_500.0f;
    private static final float WORLD_Z_FAR = -17_500.0f;
    /** The end caps: brighter at the centre, like light at the end of the tunnel. */
    private static final double CAP_CENTER_WHITE = 0.45;
    /**
     * Over this last stretch before the exit the tunnel narrows, every layer converging on the ride
     * tube's radius, so its mouth lines up with the cloud tube it opens onto instead of leaving a
     * wide empty ring of space around it.
     */
    private static final float TAPER_LENGTH = 6000.0f;
    /** Length over which pieces fade at the edges of the drawn range, and the far cap gives way. */
    private static final float EDGE_FADE = 4000.0f;
    /**
     * The grid's lines are turned half a line spacing (11.25°), so the rider - at the bottom of the
     * tunnel - flies between two lines rather than along one.
     */
    private static final double GRID_LINE_OFFSET = Math.PI / 16.0;

    private static final int SIDES = 48;
    private static final float RING_STEP = 470.0f;
    /** Wide enough apart that, at the boosted speed, the eye sees rings rush by rather than strobe. */
    private static final float GRID_RING_STEP = 1200.0f;
    private static final float GRID_RING_LENGTH = 14.0f;
    /** Cloud rings in the hue wheel's colours, the main ride's rings in the intro's palette. */
    private static final float CLOUD_RING_STEP = 4000.0f;
    private static final float CLOUD_RING_FACTOR = 0.7f;
    private static final float CLOUD_RING_ALPHA = 0.6f;
    private static final double CLOUD_RING_WHITE = 0.3;
    private static final double CLOUD_RING_SPIN = 0.15;
    private static final double CLOUD_RING_PHASE = 2.399;

    /** runasoru_ultra01: lavender, sky blue, teal, blue, purple (reference section 2.1). */
    private static final int[] WALL = {0x7C7CF0, 0x589AE5, 0x61C4B3, 0x5BB6BF, 0x7475E8, 0x9961ED};
    /** black_clouds03: the full hue wheel. */
    private static final int[] RAINBOW = {
            0x02E3E7, 0x03FE30, 0x8FFB02, 0xFDAF08, 0xFD0105, 0xFC00AF, 0x8504FC, 0x0529FC
    };
    private static final int GRID_COLOR = 0xC4E4FF;

    /** Sparkles repeat every {@link #SPARKLE_PERIOD} along the tunnel, which is endless. */
    private static final int SPARKLE_COUNT = 80;
    private static final float SPARKLE_PERIOD = 12_000.0f;
    private static final float SPARKLE_SIZE = 14.0f;
    private final float[] sparkleAngle = new float[SPARKLE_COUNT];
    private final float[] sparkleZ = new float[SPARKLE_COUNT];
    private final float[] sparklePhase = new float[SPARKLE_COUNT];

    /** This frame's exit (tunnel space) and the ride tube's radius it narrows to; see radiusAt. */
    private float exitZ = Float.MAX_VALUE;
    private float tubeRadius = RADIUS;

    public WarpRideStartTunnel() {
        Random random = new Random(191L);
        for (int i = 0; i < SPARKLE_COUNT; i++) {
            sparkleAngle[i] = (float) (random.nextDouble() * Math.PI * 2.0);
            sparkleZ[i] = random.nextFloat() * SPARKLE_PERIOD;
            sparklePhase[i] = random.nextFloat() * 6.2831855f;
        }
    }

    /**
     * @param seconds     time since the intro started; drives the rider's advance and the flow
     * @param exitSeconds the moment the rider leaves the tunnel through its open front end - the
     *                    launch - so the intro ends by flying out of the tube, not by a fade
     * @param riderZ      the rider's world Z (its plane)
     * @param rideTubeRadius the cloud tube's radius, which the tunnel's mouth narrows to
     * @return false once the whole tunnel is behind the drawn range: the caller can stop drawing it
     */
    public boolean render(MultiBufferSource buffers, Matrix4f m, double seconds, double exitSeconds, float riderZ,
                          float rideTubeRadius) {
        float riderPos = (float) (seconds * INTRO_SPEED_PX);
        float exit = (float) (exitSeconds * INTRO_SPEED_PX);
        this.exitZ = exit;
        this.tubeRadius = rideTubeRadius;
        // Tunnel-space bounds of what can be seen: out to the reach limit on either side, but the
        // tunnel stops at its exit, through which open space shows.
        float zFrom = riderPos - (WORLD_Z_NEAR - riderZ);
        float reachAhead = riderPos + (riderZ - WORLD_Z_FAR);
        float zTo = Math.min(reachAhead, exit);
        if (zTo <= zFrom) {
            return false;
        }
        Mapping map = new Mapping(riderZ, riderPos);
        float time = (float) seconds;

        VertexConsumer translucent = buffers.getBuffer(WarpRideRenderTypes.TRANSLUCENT);
        drawCap(translucent, m, map, zFrom, time, 1.0f);
        // The far cap only stands in for the tunnel going on: it fades out as the real exit comes
        // into range, so the glowing disc never snaps into an open hole.
        float capAlpha = clamp01((exit - reachAhead) / EDGE_FADE);
        if (capAlpha > 0.0f) {
            drawCap(translucent, m, map, reachAhead, time, capAlpha);
        }
        drawWall(translucent, m, map, zFrom, zTo, time, 1.0f);
        drawClouds(translucent, m, map, zFrom, zTo, time, 1.0f);
        VertexConsumer additive = buffers.getBuffer(WarpRideRenderTypes.ADDITIVE);
        drawGrid(additive, m, map, zFrom, zTo, 1.0f);
        drawSparkles(additive, m, map, zFrom, zTo, time, 1.0f);
        drawCloudRings(buffers.getBuffer(WarpRideRenderTypes.CLOUD_RING), m, map, zFrom, zTo, time, 1.0f);
        return true;
    }

    private record Mapping(float riderZ, float riderPos) {
        float worldZ(float tunnelZ) {
            return riderZ - (tunnelZ - riderPos);
        }
    }

    /**
     * Fades a discrete piece (ring, sparkle) in and out over the last {@link #EDGE_FADE} of the
     * drawn range, so nothing pops in ahead of the rider or out behind them.
     */
    private static float edgeFade(float worldZ) {
        float ahead = clamp01((worldZ - WORLD_Z_FAR) / EDGE_FADE);
        float behind = clamp01((WORLD_Z_NEAR - worldZ) / EDGE_FADE);
        return smoothstep(ahead) * smoothstep(behind);
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    private static float smoothstep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    /**
     * The radius of a layer at tunnel depth {@code z}: {@code factor} of the tunnel's own radius,
     * eased over {@link #TAPER_LENGTH} onto the ride tube's radius, which it reaches at the exit.
     */
    private float radiusAt(float z, float factor) {
        float t = smoothstep(clamp01((z - (exitZ - TAPER_LENGTH)) / TAPER_LENGTH));
        return RADIUS * factor + (tubeRadius - RADIUS * factor) * t;
    }

    /**
     * Closes one end of the drawn stretch: a disc in the wall's colour at that depth, brightening
     * towards the centre. Far away it is a small glowing hole, so the tunnel reads as going on.
     */
    private void drawCap(VertexConsumer vc, Matrix4f m, Mapping map, float z, float time, float alpha) {
        float wz = map.worldZ(z);
        float r = radiusAt(z, 1.0f);
        int center = WarpRideRenderTypes.lerpRgb(wallColor(0, z, time), 0xFFFFFF, CAP_CENTER_WHITE);
        for (int i = 0; i < SIDES; i++) {
            int j = i + 1;
            double angA = i * Math.PI * 2.0 / SIDES;
            double angB = j * Math.PI * 2.0 / SIDES;
            WarpRideRenderTypes.vertex(vc, m, 0.0f, 0.0f, wz, center, alpha);
            WarpRideRenderTypes.vertex(vc, m, 0.0f, 0.0f, wz, center, alpha);
            WarpRideRenderTypes.vertex(vc, m, (float) (Math.cos(angB) * r), (float) (Math.sin(angB) * r),
                    wz, wallColor(j, z, time), alpha);
            WarpRideRenderTypes.vertex(vc, m, (float) (Math.cos(angA) * r), (float) (Math.sin(angA) * r),
                    wz, wallColor(i, z, time), alpha);
        }
    }

    /** Layer 1: the flowing gradient wall. The ramp moves along the tunnel, so the walls stream. */
    private void drawWall(VertexConsumer vc, Matrix4f m, Mapping map, float zFrom, float zTo,
                          float time, float alpha) {
        for (float z0 = snap(zFrom, RING_STEP); z0 < zTo; z0 += RING_STEP) {
            // Never past the exit: the last segment stops exactly at the open end.
            float z1 = Math.min(z0 + RING_STEP, zTo);
            float r0 = radiusAt(z0, 1.0f);
            float r1 = radiusAt(z1, 1.0f);
            for (int i = 0; i < SIDES; i++) {
                int j = i + 1;
                int c00 = wallColor(i, z0, time);
                int c10 = wallColor(j, z0, time);
                int c11 = wallColor(j, z1, time);
                int c01 = wallColor(i, z1, time);
                quadOnWall(vc, m, map, r0, r1, i, j, z0, z1, c00, c10, c11, c01, alpha, alpha, alpha, alpha);
            }
        }
    }

    private static int wallColor(int side, float z, float time) {
        double t = z / (9000.0 * SPEED_BOOST) + time * 0.12;
        int base = WarpRideRenderTypes.ramp(WALL, t);
        // A slow swirl of brightness around the circumference keeps the wall from reading flat.
        double swirl = 0.85 + 0.15 * Math.sin(side * Math.PI * 2.0 * 3.0 / SIDES + time * 1.3 + z / 2500.0);
        return scale(base, swirl);
    }

    /** Layer 2: grey-white cloud wisps from a value noise, drifting slower than the wall. */
    private void drawClouds(VertexConsumer vc, Matrix4f m, Mapping map, float zFrom, float zTo,
                            float time, float alpha) {
        for (float z0 = snap(zFrom, RING_STEP); z0 < zTo; z0 += RING_STEP) {
            float z1 = Math.min(z0 + RING_STEP, zTo);
            float r0 = radiusAt(z0, 0.9f);
            float r1 = radiusAt(z1, 0.9f);
            for (int i = 0; i < SIDES; i++) {
                int j = i + 1;
                float a00 = cloud(i, z0, time) * alpha;
                float a10 = cloud(j, z0, time) * alpha;
                float a11 = cloud(j, z1, time) * alpha;
                float a01 = cloud(i, z1, time) * alpha;
                if (a00 + a10 + a11 + a01 < 0.01f) {
                    continue;
                }
                quadOnWall(vc, m, map, r0, r1, i, j, z0, z1, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
                        a00, a10, a11, a01);
            }
        }
    }

    private static float cloud(int side, float z, float time) {
        double n = valueNoise(side / (double) SIDES * 8.0, z / (700.0 * SPEED_BOOST) + time * 0.6, 8);
        double shaped = Math.max(0.0, (n - 0.45) / 0.55);
        return (float) (0.35 * shaped * shaped);
    }

    /** Layer 3: the glowing grid - 16 lines along the tunnel, and thin rings that move with it. */
    private void drawGrid(VertexConsumer vc, Matrix4f m, Mapping map, float zFrom, float zTo, float alpha) {
        float a = 0.45f * alpha;
        float halfWidth = 5.0f;
        for (int line = 0; line < 16; line++) {
            double angle = line * Math.PI / 8.0 + GRID_LINE_OFFSET;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float tx = -sin * halfWidth;
            float ty = cos * halfWidth;
            // In segments, so the lines follow the narrowing towards the exit.
            for (float z0 = snap(zFrom, RING_STEP); z0 < zTo; z0 += RING_STEP) {
                float z1 = Math.min(z0 + RING_STEP, zTo);
                float r0 = radiusAt(z0, 0.8f);
                float r1 = radiusAt(z1, 0.8f);
                float w0 = map.worldZ(z0);
                float w1 = map.worldZ(z1);
                WarpRideRenderTypes.vertex(vc, m, cos * r0 - tx, sin * r0 - ty, w0, GRID_COLOR, a);
                WarpRideRenderTypes.vertex(vc, m, cos * r0 + tx, sin * r0 + ty, w0, GRID_COLOR, a);
                WarpRideRenderTypes.vertex(vc, m, cos * r1 + tx, sin * r1 + ty, w1, GRID_COLOR, a);
                WarpRideRenderTypes.vertex(vc, m, cos * r1 - tx, sin * r1 - ty, w1, GRID_COLOR, a);
            }
        }
        for (float z = snap(zFrom, GRID_RING_STEP); z < zTo; z += GRID_RING_STEP) {
            float ra = a * edgeFade(map.worldZ(z));
            if (ra <= 0.005f) {
                continue;
            }
            float r = radiusAt(z, 0.8f);
            for (int i = 0; i < SIDES; i++) {
                quadOnWall(vc, m, map, r, r, i, i + 1, z, Math.min(z + GRID_RING_LENGTH, zTo),
                        GRID_COLOR, GRID_COLOR, GRID_COLOR, GRID_COLOR, ra, ra, ra, ra);
            }
        }
    }

    /**
     * Flat cloud rings, the same texture as the main ride's (plan 191, section 7.2 revised), one
     * colour of the hue wheel each. They replace the intro's rainbow bands.
     */
    @SuppressWarnings("java:S107")
    private void drawCloudRings(VertexConsumer vc, Matrix4f m, Mapping map, float zFrom, float zTo,
                                float time, float alpha) {
        for (float z = snap(zFrom, CLOUD_RING_STEP); z < zTo; z += CLOUD_RING_STEP) {
            // The texture's cloud band sits at RING_RADIUS of its half-width; the ring narrows
            // with the tunnel near the exit.
            float halfSize = radiusAt(z, CLOUD_RING_FACTOR) / WarpRideCloudTextures.RING_RADIUS;
            // Faded at both ends of the drawn range: rings no longer pop in ahead or out behind.
            float a = CLOUD_RING_ALPHA * alpha * edgeFade(map.worldZ(z));
            if (a <= 0.005f) {
                continue;
            }
            long index = (long) Math.floor(z / CLOUD_RING_STEP);
            int color = WarpRideRenderTypes.lerpRgb(RAINBOW[(int) Math.floorMod(index, (long) RAINBOW.length)],
                    0xFFFFFF, CLOUD_RING_WHITE);
            double angle = index * CLOUD_RING_PHASE + time * CLOUD_RING_SPIN * ((index & 1L) == 0L ? -1.0 : 1.0);
            float cos = (float) Math.cos(angle) * halfSize;
            float sin = (float) Math.sin(angle) * halfSize;
            float wz = map.worldZ(z);
            WarpRideRenderTypes.vertex(vc, m, -cos + sin, -sin - cos, wz, 0.0f, 0.0f, color, a);
            WarpRideRenderTypes.vertex(vc, m, cos + sin, sin - cos, wz, 1.0f, 0.0f, color, a);
            WarpRideRenderTypes.vertex(vc, m, cos - sin, sin + cos, wz, 1.0f, 1.0f, color, a);
            WarpRideRenderTypes.vertex(vc, m, -cos - sin, -sin + cos, wz, 0.0f, 1.0f, color, a);
        }
    }

    /** Layer 4: white glints that twinkle on the inner shell. */
    private void drawSparkles(VertexConsumer vc, Matrix4f m, Mapping map, float zFrom, float zTo,
                              float time, float alpha) {
        for (int i = 0; i < SPARKLE_COUNT; i++) {
            float twinkle = 0.5f + 0.5f * (float) Math.sin(time * 6.0f + sparklePhase[i]);
            float a = 0.8f * twinkle * alpha;
            float s = SPARKLE_SIZE;
            // Every repeat of this sparkle that falls inside the drawn stretch.
            for (float z = sparkleZ[i] + snap(zFrom - sparkleZ[i], SPARKLE_PERIOD); z <= zTo; z += SPARKLE_PERIOD) {
                if (z < zFrom) {
                    continue;
                }
                float wz = map.worldZ(z);
                float fa = a * edgeFade(wz);
                float r = radiusAt(z, 0.7f);
                float x = (float) (Math.cos(sparkleAngle[i]) * r);
                float y = (float) (Math.sin(sparkleAngle[i]) * r);
                WarpRideRenderTypes.vertex(vc, m, x - s, y - s, wz, 0xFFFFFF, fa);
                WarpRideRenderTypes.vertex(vc, m, x + s, y - s, wz, 0xFFFFFF, fa);
                WarpRideRenderTypes.vertex(vc, m, x + s, y + s, wz, 0xFFFFFF, fa);
                WarpRideRenderTypes.vertex(vc, m, x - s, y + s, wz, 0xFFFFFF, fa);
            }
        }
    }

    /**
     * One quad of a cylinder - or of the cone it becomes near the exit - between two sides and two
     * tunnel depths, of radius {@code r0} at {@code z0} and {@code r1} at {@code z1}.
     */
    @SuppressWarnings("java:S107")
    private static void quadOnWall(VertexConsumer vc, Matrix4f m, Mapping map, float r0, float r1,
                                   int sideA, int sideB, float z0, float z1, int c00, int c10, int c11, int c01,
                                   float a00, float a10, float a11, float a01) {
        double angA = sideA * Math.PI * 2.0 / SIDES;
        double angB = sideB * Math.PI * 2.0 / SIDES;
        float ca = (float) Math.cos(angA);
        float sa = (float) Math.sin(angA);
        float cb = (float) Math.cos(angB);
        float sb = (float) Math.sin(angB);
        float w0 = map.worldZ(z0);
        float w1 = map.worldZ(z1);
        WarpRideRenderTypes.vertex(vc, m, ca * r0, sa * r0, w0, c00, a00);
        WarpRideRenderTypes.vertex(vc, m, cb * r0, sb * r0, w0, c10, a10);
        WarpRideRenderTypes.vertex(vc, m, cb * r1, sb * r1, w1, c11, a11);
        WarpRideRenderTypes.vertex(vc, m, ca * r1, sa * r1, w1, c01, a01);
    }

    /** The largest multiple of {@code step} at or below {@code z}, so geometry stays tunnel-anchored. */
    private static float snap(float z, float step) {
        return (float) (Math.floor(z / step) * step);
    }

    private static int scale(int rgb, double factor) {
        int r = (int) Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = (int) Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = (int) Math.min(255, Math.round((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    /** Smooth 2D value noise in 0..1, wrapping on {@code x} every {@code periodX}. */
    private static double valueNoise(double x, double y, int periodX) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double v00 = hash(Math.floorMod(x0, periodX), y0);
        double v10 = hash(Math.floorMod(x0 + 1, periodX), y0);
        double v01 = hash(Math.floorMod(x0, periodX), y0 + 1);
        double v11 = hash(Math.floorMod(x0 + 1, periodX), y0 + 1);
        double top = v00 + (v10 - v00) * fx;
        double bottom = v01 + (v11 - v01) * fx;
        return top + (bottom - top) * fy;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double hash(int x, int y) {
        int h = x * 374_761_393 + y * 668_265_263;
        h = (h ^ (h >>> 13)) * 1_274_126_177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
    }
}
