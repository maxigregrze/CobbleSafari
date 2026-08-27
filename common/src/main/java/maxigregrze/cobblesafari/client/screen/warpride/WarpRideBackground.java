package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** The three background layers. Entirely client-side and incapable of influencing the ride. */
public final class WarpRideBackground {

    public static final ResourceLocation BACKDROP =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/warpride/backdrop.png");
    public static final ResourceLocation ROCK =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/gui/warpride/rock.png");

    private static final int STAR_COLOR = 0xDBD2F4;
    private static final int GROUND_COLOR = 0xFF141224;

    /** Depth at which a star or rock is recycled to the far plane. */
    private static final double RECYCLE_DEPTH = -8.0;

    /** Rocks ride this many virtual pixels beyond the octagon edge, on the screen's sides. */
    private static final double ROCK_EDGE_OFFSET = 200.0;
    /** Rocks recede at half the speed of the gameplay objects, to feel like distant scenery. */
    private static final double ROCK_SPEED_FACTOR = 0.5;

    private static final int PROP_COUNT = 3;   // ~75% fewer rocks than the original pool of 10

    private static final class Particle {
        double angleDeg;
        double radiusFactor;
        double depth;
        float brightness;
        int size;
        /** Rocks only: 0-3, a quarter-turn applied to the texture so it is not always upright. */
        int rotationQuarter;
    }

    private final RandomSource random = RandomSource.create();
    private Particle[] stars = new Particle[0];
    private final Particle[] props = new Particle[PROP_COUNT];

    private double spawnDepth = 64.0;

    public void init(WarpRideClientState state) {
        this.spawnDepth = state.visibleDepthLy;

        stars = new Particle[Math.max(0, state.starCount)];
        for (int i = 0; i < stars.length; i++) {
            stars[i] = newStar(random.nextDouble() * spawnDepth);
        }
        for (int i = 0; i < props.length; i++) {
            props[i] = newProp(random.nextDouble() * spawnDepth);
        }

        this.tubeActive = false;
        this.tubeTime = 0.0;
        this.tubeNext = TUBE_GAP_MIN + random.nextDouble() * (TUBE_GAP_MAX - TUBE_GAP_MIN);
    }

    private Particle newStar(double depth) {
        Particle p = new Particle();
        p.angleDeg = random.nextDouble() * 360.0;
        // Deliberately spans inside and outside the octagon, so stars fill the whole frame.
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
        p.brightness = 0.25f + random.nextFloat() * 0.2f;    // desaturated and dark, on purpose
        p.size = 128 + random.nextInt(129);                  // 128-256 virtual px rocks
        p.rotationQuarter = random.nextInt(4);               // normal / 90 / 180 / 270, equiprobable
        return p;
    }

    public void advance(WarpRideClientState state, double deltaSeconds) {
        double travelled = state.speed * deltaSeconds;
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
                p.brightness = fresh.brightness;
                p.size = fresh.size;
                p.rotationQuarter = fresh.rotationQuarter;
            }
        }
        tickTube(deltaSeconds);
    }

    /** Schedules the on/off cycle of the background colour tube. */
    private void tickTube(double deltaSeconds) {
        tubeTime += deltaSeconds;
        if (tubeActive) {
            if (tubeTime - tubeStart >= tubeDuration) {
                tubeActive = false;
                tubeNext = tubeTime + TUBE_GAP_MIN + random.nextDouble() * (TUBE_GAP_MAX - TUBE_GAP_MIN);
            }
        } else if (tubeTime >= tubeNext) {
            tubeActive = true;
            tubeStart = tubeTime;
            tubeDuration = TUBE_DUR_MIN + random.nextDouble() * (TUBE_DUR_MAX - TUBE_DUR_MIN);
            tubeColor = random.nextBoolean() ? TUBE_PURPLE : TUBE_CYAN;
        }
    }

    /** Layer 0, over the whole framebuffer, so an ultrawide window gets space, not black bars. */
    public void renderBackdrop(GuiGraphics graphics, int physicalWidth, int physicalHeight,
                               WarpRideClientState state) {
        // Deep-space ground colour, so nothing is ever transparent behind the playfield.
        graphics.fill(0, 0, physicalWidth, physicalHeight, GROUND_COLOR);

        WarpRideRenderer.beginAlphaBlit();
        // A slow radial breathe indexed on speed, so the backdrop moves with the ride.
        float zoom = 1.0f + (float) ((state.speed - state.minSpeed)
                / Math.max(1.0, state.maxSpeed - state.minSpeed)) * 0.15f;
        int size = (int) (Math.max(physicalWidth, physicalHeight) * zoom);
        int x = (physicalWidth - size) / 2;
        int y = (physicalHeight - size) / 2;
        graphics.setColor(1.0f, 1.0f, 1.0f, 0.55f);
        graphics.blit(BACKDROP, x, y, 0.0f, 0.0f, size, size, size, size);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    /** Layer 1, the starfield: physical space centred on the framebuffer, so ultrawide fills too. */
    public void renderStars(GuiGraphics graphics, WarpRideClientState state, int pw, int ph) {
        double speedFactor = Math.max(0.0, (state.speed - state.minSpeed)
                / Math.max(1.0, state.maxSpeed - state.minSpeed));
        int cx = pw / 2;
        int cy = ph / 2;
        double ref = 0.5 * Math.hypot(pw, ph);
        double unit = ph / 1080.0;   // physical pixels per virtual unit, to keep star sizes stable
        for (Particle p : stars) {
            renderStar(graphics, state, p, cx, cy, ref, unit, speedFactor);
        }
    }

    private void renderStar(GuiGraphics graphics, WarpRideClientState state, Particle p,
                            int cx, int cy, double refRadius, double unitScale, double speedFactor) {
        if (p.depth < 0.0) {
            return;
        }
        double scale = WarpRideRenderer.depthScale(state, p.depth);
        double radius = refRadius * p.radiusFactor * scale;
        double alpha = Math.min(1.0, scale * 2.5) * p.brightness;
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

    /** Layer 2, the rocks: side lanes only, half recede speed, desaturated so they read as scenery. */
    public void renderProps(GuiGraphics graphics, WarpRideClientState state, int cx, int cy) {
        for (Particle p : props) {
            renderRock(graphics, state, p, cx, cy);
        }
    }

    private void renderRock(GuiGraphics graphics, WarpRideClientState state, Particle p, int cx, int cy) {
        if (p.depth < 0.0) {
            return;
        }
        double scale = WarpRideRenderer.depthScale(state, p.depth);
        double radius = (state.ringFourRadius + ROCK_EDGE_OFFSET) * p.radiusFactor * scale;
        double alpha = Math.min(1.0, scale * 2.5) * 0.85;
        if (alpha <= 0.02) {
            return;
        }
        int size = Math.max(1, (int) Math.round(p.size * scale));
        double angle = Math.toRadians(p.angleDeg);
        int x = cx + (int) Math.round(Math.cos(angle) * radius);
        int y = cy + (int) Math.round(Math.sin(angle) * radius);
        float shade = p.brightness;   // 0.25-0.45: desaturated and dark
        WarpRideRenderer.beginAlphaBlit();
        graphics.setColor(shade, shade, shade * 1.15f, (float) alpha);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        if (p.rotationQuarter != 0) {
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(p.rotationQuarter * 90.0f));
        }
        graphics.blit(ROCK, -size / 2, -size / 2, 0.0f, 0.0f, size, size, size, size);
        graphics.pose().popPose();
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static final int TUBE_PURPLE = 0x9A9AE0;
    private static final int TUBE_CYAN = 0x2E43C0;
    private static final double TUBE_GAP_MIN = 1.5;    // seconds between tubes (gap halved: ~50% more often)
    private static final double TUBE_GAP_MAX = 3.5;
    private static final double TUBE_DUR_MIN = 5.0;    // seconds a tube lasts (twice as long)
    private static final double TUBE_DUR_MAX = 9.0;

    private boolean tubeActive;
    private double tubeTime;
    private double tubeStart;
    private double tubeDuration;
    private double tubeNext;
    private int tubeColor;

    /** An occasional soft-edged colour ring, drawn as concentric bands with alpha falling off. */
    public void renderTube(GuiGraphics graphics, int pw, int ph, WarpRideClientState state) {
        if (!tubeActive || tubeDuration <= 0.0) {
            return;
        }
        double t = (tubeTime - tubeStart) / tubeDuration;   // 0..1 across its life
        if (t <= 0.0 || t >= 1.0) {
            return;
        }
        double env = Math.sin(Math.PI * t);                 // fade in then out over the life
        double ref = 0.5 * Math.hypot(pw, ph);
        double peakR = ref * (0.15 + 1.4 * t);              // grows from the centre outward
        double halfW = ref * 0.18;
        drawSoftRing(graphics, pw / 2, ph / 2, peakR, halfW, tubeColor, 0.40 * env);
    }

    private void drawSoftRing(GuiGraphics graphics, int cx, int cy, double peakR, double halfW,
                              int rgb, double peakAlpha) {
        int bands = 12;
        int sides = 48;
        for (int b = 0; b < bands; b++) {
            double tt = (b + 0.5) / bands;
            double r = peakR - halfW + 2.0 * halfW * tt;
            if (r <= 1.0) {
                continue;
            }
            double fall = 1.0 - Math.abs(2.0 * tt - 1.0);   // triangular: 0 at both edges
            fall = fall * fall * (3.0 - 2.0 * fall);         // smoothstep, for soft gradient borders
            int argb = packAlpha(rgb, peakAlpha * fall);
            double bandThick = (2.0 * halfW / bands) * 1.8;  // overlap so the bands merge
            drawRingBand(graphics, cx, cy, r, sides, bandThick, argb);
        }
    }

    private void drawRingBand(GuiGraphics graphics, int cx, int cy, double r, int sides,
                              double thickness, int argb) {
        double tang = 2.0 * r * Math.sin(Math.PI / sides) * 1.1;   // slight overlap between sides
        int x1 = (int) Math.round(r - thickness / 2.0);
        int x2 = (int) Math.round(r + thickness / 2.0);
        int y1 = (int) Math.round(-tang / 2.0);
        int y2 = (int) Math.round(tang / 2.0);
        for (int i = 0; i < sides; i++) {
            graphics.pose().pushPose();
            graphics.pose().translate(cx, cy, 0.0f);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) (360.0 * i / sides)));
            graphics.fill(x1, y1, x2, y2, argb);
            graphics.pose().popPose();
        }
    }

    private static int packAlpha(int rgb, double alpha) {
        int a = (int) Math.round(Math.max(0.0, Math.min(1.0, alpha)) * 255.0);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
