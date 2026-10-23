package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.client.renderer.UltraWarpDiveModels;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Arrays;

/**
 * The background layers. Entirely client-side and incapable of influencing the ride.
 *
 * <p>Since action plan 191 (V2) the space behind the tube is procedural - a radial sky gradient,
 * drifting nebulae and passing cloud bands, all vertex colours in the source's palette - instead of
 * the old {@code backdrop.png}. The stars and the 3D rocks are unchanged, and the rocks have their
 * glint back (G1).</p>
 */
public final class WarpRideBackground {

    private static final int STAR_COLOR = 0xDBD2F4;

    // --- sky (reference section 3: skybox average #121C33, highlights #314457) ---
    private static final int SKY_CENTER = 0x314457;
    private static final int SKY_MID = 0x121C33;
    private static final int SKY_EDGE = 0x04070F;
    private static final float SKY_MID_AT = 0.35f;
    private static final int FAN_SEGMENTS = 64;

    // --- nebulae: faint teal / green / violet, slowly drifting ---
    private static final int[] NEBULA_COLORS = {0x1DC0CF, 0x2F8F6A, 0x3A2A7A};
    private static final float[] NEBULA_ALPHA = {0.22f, 0.18f, 0.20f};
    private static final double[][] NEBULA_ANCHOR = {{0.24, 0.30}, {0.78, 0.70}, {0.70, 0.22}};
    private static final double[] NEBULA_SIZE = {0.42, 0.36, 0.30};
    private static final double NEBULA_DRIFT_PERIOD = 40.0;

    // --- cloud bands (black_clouds04 #022B53, black_clouds #123162) ---
    private static final int[] CLOUD_COLORS = {0x022B53, 0x123162};
    private static final float[] CLOUD_ALPHA = {0.35f, 0.30f};
    private static final int CLOUD_COUNT = 4;
    /** Light-years for a band to sweep from the centre out past the screen edge. */
    private static final double CLOUD_TRAVEL_LY = 160.0;

    /** Depth at which a star or rock is recycled to the far plane. */
    private static final double RECYCLE_DEPTH = -8.0;

    /** Rocks ride this many virtual pixels beyond the tube edge, on the screen's sides. */
    private static final double ROCK_EDGE_OFFSET = 200.0;
    /** Rocks recede at half the speed of the gameplay objects, to feel like distant scenery. */
    private static final double ROCK_SPEED_FACTOR = 0.5;

    private static final int PROP_COUNT = 3;   // ~75% fewer rocks than the original pool of 10

    /** The rock model's height in model pixels: a rock of {@code size} px is size/32 px per pixel. */
    private static final float ROCK_MODEL_PX = 32.0f;

    private static final class Particle {
        double angleDeg;
        double radiusFactor;
        double depth;
        float brightness;
        int size;
        /** Rocks only: a uniformly random 3D orientation, drawn once and then frozen. */
        final Quaternionf orientation = new Quaternionf();
    }

    /** One cloud band: its direction from the centre, its progress outward, and its look. */
    private static final class CloudBand {
        double angle;
        double progress;
        double tilt;
        int color;
        float alpha;
    }

    private final RandomSource random = RandomSource.create();
    private Particle[] stars = new Particle[0];
    private final Particle[] props = new Particle[PROP_COUNT];
    private final CloudBand[] clouds = new CloudBand[CLOUD_COUNT];

    private double spawnDepth = 64.0;
    private double time;

    public void init(WarpRideClientState state) {
        this.spawnDepth = state.visibleDepthLy;

        stars = new Particle[Math.max(0, state.starCount)];
        for (int i = 0; i < stars.length; i++) {
            stars[i] = newStar(random.nextDouble() * spawnDepth);
        }
        for (int i = 0; i < props.length; i++) {
            props[i] = newProp(random.nextDouble() * spawnDepth);
        }
        for (int i = 0; i < clouds.length; i++) {
            clouds[i] = new CloudBand();
            resetCloud(clouds[i], (i + random.nextDouble()) / clouds.length);
        }
    }

    private void resetCloud(CloudBand band, double progress) {
        band.angle = random.nextDouble() * Math.PI * 2.0;
        band.progress = progress;
        band.tilt = (random.nextDouble() - 0.5) * 0.6;
        int pick = random.nextInt(CLOUD_COLORS.length);
        band.color = CLOUD_COLORS[pick];
        band.alpha = CLOUD_ALPHA[pick];
    }

    private Particle newStar(double depth) {
        Particle p = new Particle();
        p.angleDeg = random.nextDouble() * 360.0;
        // Deliberately spans inside and outside the tube, so stars fill the whole frame.
        p.radiusFactor = 0.15 + random.nextDouble() * 1.25;
        p.depth = depth;
        p.brightness = 0.35f + random.nextFloat() * 0.65f;
        p.size = 2 + random.nextInt(3);
        return p;
    }

    /** Angle slots per side: 9 lanes at 7.5° spacing, spanning ±30° around each side base. */
    private static final double ROCK_ANGLE_STEP = 7.5;
    private static final int ROCK_SLOT_SPAN = 4;             // slot in [-4, +4] -> ±30°, 9 lanes

    private Particle newProp(double depth) {
        Particle p = new Particle();
        // Side lanes only (base 0° / 180°), quantised into discrete lanes so rocks do not clump.
        double base = random.nextBoolean() ? 0.0 : 180.0;
        int slot = random.nextInt(2 * ROCK_SLOT_SPAN + 1) - ROCK_SLOT_SPAN;
        p.angleDeg = base + slot * ROCK_ANGLE_STEP;
        p.radiusFactor = 0.9 + random.nextDouble() * 0.35;   // small spread around the edge offset
        p.depth = depth;
        p.size = 128 + random.nextInt(129);                  // 128-256 virtual px rocks
        randomOrientation(p.orientation);
        return p;
    }

    /** Shoemake's uniform random rotation: every orientation equally likely. */
    private void randomOrientation(Quaternionf out) {
        double u1 = random.nextDouble();
        double u2 = random.nextDouble() * 2.0 * Math.PI;
        double u3 = random.nextDouble() * 2.0 * Math.PI;
        double a = Math.sqrt(1.0 - u1);
        double b = Math.sqrt(u1);
        out.set((float) (a * Math.sin(u2)), (float) (a * Math.cos(u2)),
                (float) (b * Math.sin(u3)), (float) (b * Math.cos(u3)));
    }

    public void advance(WarpRideClientState state, double deltaSeconds) {
        time += deltaSeconds;
        // The scenery's speed: the start tunnel's pace through the intro, easing to the ride's after.
        double travelled = state.decorSpeed() * deltaSeconds;
        for (Particle p : stars) {
            p.depth -= travelled;
            if (p.depth < RECYCLE_DEPTH) {
                Particle fresh = newStar(spawnDepth);
                p.angleDeg = fresh.angleDeg;
                p.radiusFactor = fresh.radiusFactor;
                p.depth = fresh.depth;
                p.brightness = fresh.brightness;
                p.size = fresh.size;
            }
        }
        for (Particle p : props) {
            p.depth -= travelled * ROCK_SPEED_FACTOR;   // rocks recede at half speed
            if (p.depth < RECYCLE_DEPTH) {
                Particle fresh = newProp(spawnDepth);
                p.angleDeg = fresh.angleDeg;
                p.radiusFactor = fresh.radiusFactor;
                p.depth = fresh.depth;
                p.size = fresh.size;
                p.orientation.set(fresh.orientation);
            }
        }
        for (CloudBand band : clouds) {
            band.progress += travelled / CLOUD_TRAVEL_LY;
            if (band.progress >= 1.0) {
                resetCloud(band, band.progress - 1.0);
            }
        }
    }

    /**
     * Layer 0, over the whole framebuffer, so an ultrawide window gets space, not black bars: the
     * sky gradient, then the nebulae, then the cloud bands.
     */
    public void renderBackdrop(GuiGraphics graphics, int physicalWidth, int physicalHeight,
                               WarpRideClientState state) {
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        Matrix4f m = graphics.pose().last().pose();
        float cx = physicalWidth / 2.0f;
        float cy = physicalHeight / 2.0f;
        // A slow radial breathe indexed on speed, so the sky moves with the ride.
        float zoom = 1.0f + (float) ((state.speed - state.minSpeed)
                / Math.max(1.0, state.maxSpeed - state.minSpeed)) * 0.15f;
        float reach = (float) (0.5 * Math.hypot(physicalWidth, physicalHeight)) * 1.05f;

        VertexConsumer sky = buffers.getBuffer(WarpRideRenderTypes.TRANSLUCENT);
        float midRadius = reach * SKY_MID_AT * zoom;
        fan(sky, m, cx, cy, 0.0f, midRadius, 1.0f, SKY_CENTER, 1.0f, SKY_MID, 1.0f);
        fan(sky, m, cx, cy, midRadius, reach * 1.6f, 1.0f, SKY_MID, 1.0f, SKY_EDGE, 1.0f);

        VertexConsumer glow = buffers.getBuffer(WarpRideRenderTypes.ADDITIVE);
        double span = Math.max(physicalWidth, physicalHeight);
        for (int i = 0; i < NEBULA_COLORS.length; i++) {
            double phase = time / NEBULA_DRIFT_PERIOD * Math.PI * 2.0 + i * 2.1;
            float nx = (float) (physicalWidth * (NEBULA_ANCHOR[i][0] + 0.06 * Math.sin(phase)));
            float ny = (float) (physicalHeight * (NEBULA_ANCHOR[i][1] + 0.05 * Math.cos(phase * 0.8)));
            float radius = (float) (span * NEBULA_SIZE[i] * (1.0 + 0.08 * Math.sin(phase * 1.3)));
            fan(glow, m, nx, ny, 0.0f, radius, 1.0f, NEBULA_COLORS[i], NEBULA_ALPHA[i], NEBULA_COLORS[i], 0.0f);
        }

        VertexConsumer veil = buffers.getBuffer(WarpRideRenderTypes.TRANSLUCENT);
        for (CloudBand band : clouds) {
            double p = band.progress;
            float distance = (float) (p * p * reach * 1.3);
            float bx = cx + (float) Math.cos(band.angle) * distance;
            float by = cy + (float) Math.sin(band.angle) * distance;
            float length = (float) (reach * (0.25 + 0.9 * p));
            float alpha = (float) (band.alpha * Math.sin(Math.PI * p));
            // Elongated across the radial direction, like a cloud layer swept past the camera.
            float stretch = (float) (band.angle + Math.PI / 2.0 + band.tilt);
            fan(veil, m, bx, by, 0.0f, length, 0.18f, band.color, alpha, band.color, 0.0f, stretch);
        }
        graphics.flush();
    }

    /** A radial gradient between two radii; {@code squash} flattens it into an ellipse. */
    @SuppressWarnings("java:S107")
    private static void fan(VertexConsumer vc, Matrix4f m, float cx, float cy, float r0, float r1, float squash,
                            int innerColor, float innerAlpha, int outerColor, float outerAlpha) {
        fan(vc, m, cx, cy, r0, r1, squash, innerColor, innerAlpha, outerColor, outerAlpha, 0.0f);
    }

    @SuppressWarnings("java:S107")
    private static void fan(VertexConsumer vc, Matrix4f m, float cx, float cy, float r0, float r1, float squash,
                            int innerColor, float innerAlpha, int outerColor, float outerAlpha, float rotation) {
        double cosR = Math.cos(rotation);
        double sinR = Math.sin(rotation);
        for (int i = 0; i < FAN_SEGMENTS; i++) {
            double a0 = i * Math.PI * 2.0 / FAN_SEGMENTS;
            double a1 = (i + 1) * Math.PI * 2.0 / FAN_SEGMENTS;
            float[] i0 = point(cx, cy, r0, squash, a0, cosR, sinR);
            float[] i1 = point(cx, cy, r0, squash, a1, cosR, sinR);
            float[] o1 = point(cx, cy, r1, squash, a1, cosR, sinR);
            float[] o0 = point(cx, cy, r1, squash, a0, cosR, sinR);
            WarpRideRenderTypes.vertex(vc, m, i0[0], i0[1], 0.0f, innerColor, innerAlpha);
            WarpRideRenderTypes.vertex(vc, m, i1[0], i1[1], 0.0f, innerColor, innerAlpha);
            WarpRideRenderTypes.vertex(vc, m, o1[0], o1[1], 0.0f, outerColor, outerAlpha);
            WarpRideRenderTypes.vertex(vc, m, o0[0], o0[1], 0.0f, outerColor, outerAlpha);
        }
    }

    @SuppressWarnings("java:S107")
    private static float[] point(float cx, float cy, float radius, float squash, double angle,
                                 double cosR, double sinR) {
        double x = Math.cos(angle) * radius;
        double y = Math.sin(angle) * radius * squash;
        return new float[]{(float) (cx + x * cosR - y * sinR), (float) (cy + x * sinR + y * cosR)};
    }

    /** Layer 1, the starfield: physical space centred on the framebuffer, so ultrawide fills too. */
    public void renderStars(GuiGraphics graphics, WarpRideClientState state, int pw, int ph, float worldAlpha) {
        if (worldAlpha <= 0.01f) {
            return;
        }
        // Capped: through the intro the scenery outruns the ride's top speed several times over.
        double speedFactor = Math.max(0.0, Math.min(1.5, (state.decorSpeed() - state.minSpeed)
                / Math.max(1.0, state.maxSpeed - state.minSpeed)));
        int cx = pw / 2;
        int cy = ph / 2;
        double ref = 0.5 * Math.hypot(pw, ph);
        double unit = ph / 1080.0;   // physical pixels per virtual unit, to keep star sizes stable
        for (Particle p : stars) {
            renderStar(graphics, state, p, cx, cy, ref, unit, speedFactor, worldAlpha);
        }
    }

    @SuppressWarnings("java:S107")
    private void renderStar(GuiGraphics graphics, WarpRideClientState state, Particle p,
                            int cx, int cy, double refRadius, double unitScale, double speedFactor,
                            float worldAlpha) {
        if (p.depth < 0.0) {
            return;
        }
        double scale = WarpRideRenderer.depthScale(state, p.depth);
        double radius = refRadius * p.radiusFactor * scale;
        double alpha = Math.min(1.0, scale * 2.5) * p.brightness * worldAlpha;
        if (alpha <= 0.02) {
            return;
        }
        int size = Math.max(1, (int) Math.round(p.size * unitScale * scale));
        // The streak: length grows with speed and proximity — this turns a starfield into hyperspace.
        int length = Math.max(size, (int) Math.round(size * (1.0 + speedFactor * 14.0 * scale)));
        int argb = packAlpha(STAR_COLOR, alpha);
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0f);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) p.angleDeg));
        graphics.fill((int) Math.round(radius - length), -size / 2,
                (int) Math.round(radius), -size / 2 + size, argb);
        graphics.pose().popPose();
    }

    /**
     * Layer 2, the rocks, in their own perspective pass (plan 189, section 7): side lanes only, half
     * recede speed, a frozen random orientation, and the blue glint - full brightness, they glow.
     */
    /**
     * @param minDepth rocks nearer than this are not drawn: during the intro it is the start
     *                 tunnel's exit, so only the rocks beyond it - outside the tunnel - show
     */
    public void renderRocks(GuiGraphics graphics, WarpRideClientState state, double minDepth) {
        if (minDepth >= state.visibleDepthLy) {
            return;
        }
        MultiBufferSource.BufferSource buffers = WarpRideRenderer.beginScene(graphics);
        PoseStack pose = new PoseStack();
        // Painter's order: the farthest rock first.
        Particle[] order = props.clone();
        Arrays.sort(order, (a, b) -> Double.compare(b.depth, a.depth));
        for (Particle p : order) {
            if (p.depth >= minDepth) {
                renderRock(pose, buffers, state, p);
            }
        }
        UltraWarpDiveModels.setGlintAlpha(1.0f);
        WarpRideRenderer.endScene(buffers);
    }

    private void renderRock(PoseStack pose, MultiBufferSource.BufferSource buffers, WarpRideClientState state,
                            Particle p) {
        if (p.depth < state.fifthPlaneDepth) {
            return;
        }
        double scale = WarpRideRenderer.depthScale(state, p.depth);
        // The lane objects' opacity curve: 0 at the horizon, so a rock fades in instead of popping,
        // and back to 0 at the fifth plane, so it fades out past the rider instead of vanishing.
        float alpha = (float) (WarpRideRenderer.opacity(state, p.depth) * 0.85);
        if (alpha <= 0.02f) {
            return;
        }
        // Unscaled position at the rider plane; the eye-space depth does the shrinking.
        double radius = (state.ringFourRadius + ROCK_EDGE_OFFSET) * p.radiusFactor;
        double angle = Math.toRadians(p.angleDeg);
        float blockPx = p.size / ROCK_MODEL_PX * 16.0f * WarpRideRenderer.PROP_SCALE;

        pose.pushPose();
        pose.translate((float) (Math.cos(angle) * radius), (float) (-Math.sin(angle) * radius),
                WarpRideScene3D.zFor(scale));
        pose.mulPose(p.orientation);
        pose.scale(blockPx, blockPx, blockPx);
        pose.translate(-0.5f, -0.5f, -0.5f);
        UltraWarpDiveModels.putModel(pose.last(), WarpRideRenderer.translucentBuffer(buffers),
                WarpRideRenderer.rocksModel(), alpha);
        // The glint colour is a shader uniform read at flush: each rock flushes its own glint.
        UltraWarpDiveModels.setGlintAlpha(alpha);
        UltraWarpDiveModels.putModel(pose.last(), buffers.getBuffer(UltraWarpDiveModels.rockGlint()),
                WarpRideRenderer.rocksModel(), 1.0f);
        buffers.endBatch(UltraWarpDiveModels.rockGlint());
        pose.popPose();
    }

    private static int packAlpha(int rgb, double alpha) {
        int a = (int) Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255.0);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
