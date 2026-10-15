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

    public void onPositive() {
        positiveFlourishUntil = System.nanoTime() + FLOURISH_NANOS;
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

    public void render(GuiGraphics graphics, WarpRideClientState state) {
        renderEnergyAura(graphics, state);
        renderHitEffect(graphics);
    }

    /**
     * The energy aura (plan 189, section 9.2): the flat positive strip centred on the rider, so it
     * follows jumps and drift, sized by energy. The old gauge's feedback moves onto it with the same
     * priorities: red on a hit, a white flash on a pickup, a white veil while decay is frozen.
     */
    private void renderEnergyAura(GuiGraphics graphics, WarpRideClientState state) {
        double ratio = Math.max(0.0, Math.min(1.0, state.energy / 100.0));
        int size = (int) Math.round(WarpRideRenderer.RIDER_SCREEN_PX * AURA_MAX_FACTOR * ratio);
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
