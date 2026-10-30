package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * The HUD. Since action plan 189 there is no text and no gauge during the ride: energy reads off
 * the aura in front of the rider, and the hit effect is the only other layer.
 */
public final class WarpRideHud {

    static final ResourceLocation BUZZ = ResourceLocation.fromNamespaceAndPath(
            CobbleSafari.MOD_ID, "textures/gui/warpride/buzz_effect.png");

    /** How long a pickup flourish stays on the aura, in nanoseconds. */
    private static final long FLOURISH_NANOS = 1_400_000_000L;

    /** At 100 % energy the aura is this many times the rider's on-screen size; linear down to 0. */
    private static final double AURA_MAX_FACTOR = 1.5;
    private static final double AURA_PICKUP_WHITE = 0.66;
    private static final double AURA_FROZEN_WHITE = 0.50;

    /** Kept low and single-pulse (no strobe) for photosensitivity safety. */
    private static final int FLASH_COLOR = 0x9AD5DB;
    private static final double FLASH_PEAK_ALPHA = 0.25;
    private static final double BUZZ_PEAK_ALPHA = 0.85;
    private static final long HIT_EFFECT_NANOS = 500_000_000L;   // 0.5 s smooth fade-out
    private static final int BUZZ_SIZE = 360;                    // canvas px, square
    private static final int BUZZ_MIN = 4;
    private static final int BUZZ_MAX = 6;

    private long hitEffectUntil;
    private long hitEffectStart;
    /** One flush-to-edge buzz sprite: {@code edge} 0 bottom / 1 top / 2 left / 3 right, {@code along} 0..1. */
    private record BuzzSprite(int edge, double along) {}
    private BuzzSprite[] buzzSprites = new BuzzSprite[0];
    private final RandomSource random = RandomSource.create();

    private long positiveFlourishUntil;
    private long negativeFlourishUntil;

    /** The pickup burst (action plan 200, section 4.1): a yellow half-transparent flash, 0.25 s. */
    private static final int PICKUP_FLASH_COLOR = 0xFFED0B;
    private static final double PICKUP_FLASH_PEAK_ALPHA = 0.5;
    private static final double PICKUP_FLASH_SECONDS = 0.25;
    /** 100 % of the burst sizes: the aura at full energy, the largest the ring ever gets. */
    private static final double PICKUP_FULL_PX = WarpRideRenderer.RIDER_SCREEN_PX * AURA_MAX_FACTOR;
    /** positive_get_short: one full turn while it fades out, at full size. */
    private static final double SHORT_SECONDS = 0.5;
    /**
     * The long rings' own animation (rise, hold, fade) is 30 % shorter than first specified; each
     * still appears at its own offset from the flash (revision 2 of section 4.1).
     */
    private static final double LONG_RING_TIME_SCALE = 0.7;
    private static final int MAX_PICKUPS = 6;

    /**
     * One positive_get_long ring, times in seconds from the flash: fades in while growing from
     * {@code startScale} to {@code fullScale}, holds, then grows to {@code endScale} while fading out.
     */
    private record Ring(double appear, double full, double fadeStart, double end,
                        double startScale, double fullScale, double endScale) {}

    private static final Ring[] LONG_RINGS = {
            ring(0.2, 0.4, 0.1, 0.2, 0.80, 1.00, 1.20),
            ring(0.4, 0.4, 0.1, 0.2, 0.85, 1.05, 1.25)
    };

    /** The whole burst is over once the last long ring has faded. */
    private static final double PICKUP_SECONDS = LONG_RINGS[LONG_RINGS.length - 1].end();

    /** A ring from its appearance offset and its three phase lengths, the latter time-scaled. */
    @SuppressWarnings("java:S107")
    private static Ring ring(double appear, double rise, double hold, double fade,
                             double startScale, double fullScale, double endScale) {
        double full = appear + rise * LONG_RING_TIME_SCALE;
        double fadeStart = full + hold * LONG_RING_TIME_SCALE;
        return new Ring(appear, full, fadeStart, fadeStart + fade * LONG_RING_TIME_SCALE,
                startScale, fullScale, endScale);
    }

    /** Start of every burst still playing: a quick run of bonuses overlaps them instead of cutting. */
    private final java.util.ArrayDeque<Long> pickups = new java.util.ArrayDeque<>();

    public void onPositive() {
        long now = System.nanoTime();
        positiveFlourishUntil = now + FLOURISH_NANOS;
        if (pickups.size() >= MAX_PICKUPS) {
            pickups.pollFirst();
        }
        pickups.addLast(now);
    }

    public void onNegative() {
        long now = System.nanoTime();
        negativeFlourishUntil = now + FLOURISH_NANOS;
        hitEffectStart = now;
        hitEffectUntil = now + HIT_EFFECT_NANOS;
        int count = BUZZ_MIN + random.nextInt(BUZZ_MAX - BUZZ_MIN + 1);
        buzzSprites = new BuzzSprite[count];
        for (int i = 0; i < count; i++) {
            // Keep the sprite mostly on-screen along its edge.
            buzzSprites[i] = new BuzzSprite(random.nextInt(4), 0.15 + random.nextDouble() * 0.70);
        }
    }

    /**
     * @param camera the camera feedback, which the aura follows so it stays on the rider
     * @param now    wall clock, for the aura's growth at launch (action plan 191, I1)
     */
    public void render(GuiGraphics graphics, WarpRideClientState state, WarpRideCamera camera, long now) {
        // The aura appears with the launch: from nothing to its size in the first 0.3 s.
        double growth = Math.min(1.0, state.worldAlpha(now) * 2.0);
        if (growth > 0.0) {
            graphics.pose().pushPose();
            camera.apply2D(graphics.pose(), now);
            renderEnergyAura(graphics, state, growth);
            graphics.pose().popPose();
        }
        renderPickup(graphics, state, camera, now);
        renderHitEffect(graphics);
    }

    /**
     * The pickup burst, centred on the rider like the aura so it follows jumps, the drift and the
     * camera feedback: the flash, then positive_get_short turning once as it fades, then two
     * positive_get_long rings swelling in and out, the second 0.2 s behind the first.
     */
    private void renderPickup(GuiGraphics graphics, WarpRideClientState state, WarpRideCamera camera, long now) {
        while (!pickups.isEmpty() && seconds(now, pickups.peekFirst()) >= PICKUP_SECONDS) {
            pickups.pollFirst();
        }
        if (pickups.isEmpty()) {
            return;
        }
        // One flash, from the latest pickup: stacked flashes would strobe.
        double latest = seconds(now, pickups.peekLast());
        if (latest < PICKUP_FLASH_SECONDS) {
            int flashA = (int) Math.round(PICKUP_FLASH_PEAK_ALPHA * (1.0 - latest / PICKUP_FLASH_SECONDS) * 255.0);
            if (flashA > 0) {
                WarpRideRenderer.fillViewport(graphics, (flashA << 24) | PICKUP_FLASH_COLOR);
            }
        }
        int centerY = WarpRideRenderer.CENTER_Y + (int) Math.round(WarpRideRenderer.riderRadius(state));
        graphics.pose().pushPose();
        camera.apply2D(graphics.pose(), now);
        for (long start : pickups) {
            double t = seconds(now, start);
            if (t < SHORT_SECONDS) {
                double p = t / SHORT_SECONDS;
                drawBurst(graphics, WarpRideRenderer.POSITIVE_GET_SHORT, centerY, 1.0, 360.0 * p, 1.0 - p);
            }
            for (Ring ring : LONG_RINGS) {
                drawRing(graphics, ring, t, centerY);
            }
        }
        graphics.pose().popPose();
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void drawRing(GuiGraphics graphics, Ring ring, double t, int centerY) {
        if (t < ring.appear() || t >= ring.end()) {
            return;
        }
        double scale;
        double alpha;
        if (t < ring.full()) {
            double p = (t - ring.appear()) / (ring.full() - ring.appear());
            scale = ring.startScale() + (ring.fullScale() - ring.startScale()) * p;
            alpha = p;
        } else if (t < ring.fadeStart()) {
            scale = ring.fullScale();
            alpha = 1.0;
        } else {
            double p = (t - ring.fadeStart()) / (ring.end() - ring.fadeStart());
            scale = ring.fullScale() + (ring.endScale() - ring.fullScale()) * p;
            alpha = 1.0 - p;
        }
        drawBurst(graphics, WarpRideRenderer.POSITIVE_GET_LONG, centerY, scale, 0.0, alpha);
    }

    /** One square image centred on the rider, {@code scale} times the full aura, turned and faded. */
    private static void drawBurst(GuiGraphics graphics, ResourceLocation texture, int centerY,
                                  double scale, double degrees, double alpha) {
        int size = (int) Math.round(PICKUP_FULL_PX * scale);
        if (size <= 0 || alpha <= 0.0) {
            return;
        }
        WarpRideRenderer.beginAlphaBlit();
        graphics.pose().pushPose();
        graphics.pose().translate(WarpRideRenderer.CENTER_X, centerY, 0.0f);
        if (degrees != 0.0) {
            graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) degrees));
        }
        graphics.setColor(1.0f, 1.0f, 1.0f, (float) Math.min(1.0, alpha));
        graphics.blit(texture, -size / 2, -size / 2, 0.0f, 0.0f, size, size, size, size);
        graphics.pose().popPose();
    }

    private static double seconds(long now, long start) {
        return (now - start) / 1_000_000_000.0;
    }

    /**
     * The energy aura (plan 189, section 9.2): the flat positive strip centred on the rider, so it
     * follows jumps and drift, sized by energy. The old gauge's feedback moves onto it with the same
     * priorities: red on a hit, a white flash on a pickup, a white veil while decay is frozen.
     */
    private void renderEnergyAura(GuiGraphics graphics, WarpRideClientState state, double growth) {
        double ratio = Math.max(0.0, Math.min(1.0, state.energy / 100.0));
        int size = (int) Math.round(WarpRideRenderer.RIDER_SCREEN_PX * AURA_MAX_FACTOR * ratio * growth);
        int centerY = WarpRideRenderer.CENTER_Y + (int) Math.round(WarpRideRenderer.riderRadius(state));

        long now = System.nanoTime();
        boolean neg = now < negativeFlourishUntil;
        boolean pos = !neg && now < positiveFlourishUntil;
        double white = 0.0;
        if (pos) {
            white = AURA_PICKUP_WHITE;
        } else if (!neg && state.frozen) {
            white = AURA_FROZEN_WHITE;
        }
        if (neg) {
            WarpRideRenderer.drawAura(graphics, centerY, size, 1.0f, 0.55f, 0.5f, white);
        } else {
            WarpRideRenderer.drawAura(graphics, centerY, size, 1.0f, 1.0f, 1.0f, white);
        }
    }

    /** The malus hit effect: a green wash plus 4-6 edge buzz sprites. One fade-out, no strobe. */
    private void renderHitEffect(GuiGraphics graphics) {
        long now = System.nanoTime();
        if (now >= hitEffectUntil) {
            return;
        }
        double t = (double) (now - hitEffectStart) / (double) HIT_EFFECT_NANOS;
        double fade = Math.max(0.0, 1.0 - t);   // linear single pulse

        int flashA = (int) Math.round(FLASH_PEAK_ALPHA * fade * 255.0);
        if (flashA > 0) {
            WarpRideRenderer.fillViewport(graphics, (flashA << 24) | FLASH_COLOR);
        }
        float buzzA = (float) (BUZZ_PEAK_ALPHA * fade);
        if (buzzA > 0.01f) {
            for (BuzzSprite b : buzzSprites) {
                drawBuzz(graphics, b, buzzA);
            }
        }
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void drawBuzz(GuiGraphics graphics, BuzzSprite b, float alpha) {
        // The window's own edges, not the canvas box: on a window that is not 16:9 the canvas edge
        // sits inside the visible area, which left the sprites floating in from nowhere.
        int l = WarpRideRenderer.viewLeft();
        int t = WarpRideRenderer.viewTop();
        int r = WarpRideRenderer.viewRight();
        int bo = WarpRideRenderer.viewBottom();
        double ax;
        double ay;
        float rot;
        switch (b.edge()) {
            case 1 -> { ax = l + b.along() * (r - l); ay = t;                      rot = 180.0f; }  // top
            case 2 -> { ax = l;                       ay = t + b.along() * (bo - t); rot = 90.0f; }  // left
            case 3 -> { ax = r;                       ay = t + b.along() * (bo - t); rot = -90.0f; } // right
            default -> { ax = l + b.along() * (r - l); ay = bo;                     rot = 0.0f; }    // bottom
        }
        // Re-armed per sprite: the green flash is a fill, and a fill's flush turns blending back off.
        WarpRideRenderer.beginAlphaBlit();
        graphics.pose().pushPose();
        graphics.pose().translate((float) ax, (float) ay, 0.0f);
        if (rot != 0.0f) {
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(rot));
        }
        graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        // Bottom-centre of the texture at the origin, so it sits on the border and grows inward.
        graphics.blit(BUZZ, -BUZZ_SIZE / 2, -BUZZ_SIZE, 0.0f, 0.0f, BUZZ_SIZE, BUZZ_SIZE, BUZZ_SIZE, BUZZ_SIZE);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.pose().popPose();
    }
}
