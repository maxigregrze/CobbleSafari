package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** The HUD. The layout is fully symmetric on x = 960; every future addition should keep it that way. */
public final class WarpRideHud {

    private static final int BAR_X = 480;
    private static final int BAR_Y = 13;
    private static final int BAR_W = 960;
    private static final int BAR_H = 96;

    private static final int KEY_Y = 971;
    private static final int KEY_W = 192;
    private static final int KEY_H = 96;
    private static final int KEY_CW_X = 639;
    private static final int KEY_JUMP_X = 864;
    private static final int KEY_CCW_X = 1089;

    private static final int ODOMETER_Y = 124;

    static final ResourceLocation BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobbleSafari.MOD_ID, "textures/gui/warpride/energy_bar.png");
    // One plate per key rather than one shared plate: each carries its own arrow, so the row reads
    // as clockwise / jump / counter-clockwise without relying on the labels alone.
    static final ResourceLocation KEY_CLOCKWISE = ResourceLocation.fromNamespaceAndPath(
            CobbleSafari.MOD_ID, "textures/gui/warpride/key_l.png");
    static final ResourceLocation KEY_JUMP = ResourceLocation.fromNamespaceAndPath(
            CobbleSafari.MOD_ID, "textures/gui/warpride/key_t.png");
    static final ResourceLocation KEY_COUNTER_CLOCKWISE = ResourceLocation.fromNamespaceAndPath(
            CobbleSafari.MOD_ID, "textures/gui/warpride/key_r.png");
    static final ResourceLocation BUZZ = ResourceLocation.fromNamespaceAndPath(
            CobbleSafari.MOD_ID, "textures/gui/warpride/buzz_effect.png");

    /** How long a pickup flourish stays on the bar, in nanoseconds. */
    private static final long FLOURISH_NANOS = 1_400_000_000L;

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

    private WarpRideHudLabels labels = WarpRideHudLabels.EMPTY;

    /** The three key names, read from the player's own bindings. */
    public record WarpRideHudLabels(String clockwise, String jump, String counterClockwise) {
        public static final WarpRideHudLabels EMPTY = new WarpRideHudLabels("A", "W", "D");
    }

    public void setLabels(WarpRideHudLabels labels) {
        this.labels = labels;
    }

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

    public void render(GuiGraphics graphics, Font font, WarpRideClientState state) {
        renderHitEffect(graphics, state);   // under the bar/keys so the HUD stays readable
        renderEnergyBar(graphics, font, state);
        renderOdometer(graphics, font, state);
        renderKeys(graphics, font, state);
    }

    /** The malus hit effect: a green wash plus 4-6 edge buzz sprites. One fade-out, no strobe. */
    private void renderHitEffect(GuiGraphics graphics, WarpRideClientState state) {
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

    /** The bar drains from both ends: a centred slice of energy_bar.png, cropped in as it empties. */
    private void renderEnergyBar(GuiGraphics graphics, Font font, WarpRideClientState state) {
        double ratio = Math.max(0.0, Math.min(1.0, state.energy / 100.0));
        int fillW = (int) Math.round(BAR_W * ratio);

        graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + BAR_H, 0x88101024);

        if (fillW > 0) {
            int uOff = (BAR_W - fillW) / 2;
            long now = System.nanoTime();
            boolean neg = now < negativeFlourishUntil;
            boolean pos = !neg && now < positiveFlourishUntil;

            // The bar texture, red-tinted on a hit: the centred fillW-wide slice, drawn at that width.
            if (neg) {
                graphics.setColor(1.0f, 0.55f, 0.5f, 1.0f);
            }
            WarpRideRenderer.beginAlphaBlit();   // the empty-track fill above turned blending off
            graphics.blit(BAR_TEXTURE, BAR_X + uOff, BAR_Y, (float) uOff, 0.0f,
                    fillW, BAR_H, BAR_W, BAR_H);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

            // A white overlay: 66 % just after gaining energy, 50 % while frozen, never over a hit's red.
            double whiteAlpha = pos ? 0.66 : (!neg && state.frozen ? 0.50 : 0.0);
            if (whiteAlpha > 0.0) {
                int wa = ((int) Math.round(whiteAlpha * 255.0) << 24) | 0x00FFFFFF;
                graphics.fill(BAR_X + uOff, BAR_Y, BAR_X + uOff + fillW, BAR_Y + BAR_H, wa);
            }
        }

        String percent = Math.round(state.energy) + "%";
        drawScaledCentered(graphics, font, percent, WarpRideRenderer.CENTER_X,
                BAR_Y + BAR_H / 2 - 12, 3.0f, 0xFFFFFFFF);

        RenderSystem.disableBlend();
    }

    private void renderOdometer(GuiGraphics graphics, Font font, WarpRideClientState state) {
        String text = Component.translatable("gui.cobblesafari.warpride.hud.distance",
                Long.toString(Math.round(state.odometer))).getString();
        drawScaledCentered(graphics, font, text, WarpRideRenderer.CENTER_X, ODOMETER_Y, 2.0f, 0xFFBFA8FF);
    }

    private void renderKey(GuiGraphics graphics, Font font, ResourceLocation plate,
                           int x, String label, boolean dimmed) {
        // The label is drawn with drawString, which flushes and disables blending, so re-arm per plate.
        WarpRideRenderer.beginAlphaBlit();
        float alpha = dimmed ? 0.35f : 1.0f;
        graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        graphics.blit(plate, x, KEY_Y, 0.0f, 0.0f, KEY_W, KEY_H, KEY_W, KEY_H);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        drawFittedCentered(graphics, font, label, x + KEY_W / 2, KEY_Y + KEY_H / 2 - 8,
                KEY_W - 24, dimmed ? 0xFF6A6A7A : 0xFFFFFFFF);
        RenderSystem.disableBlend();
    }

    private void renderKeys(GuiGraphics graphics, Font font, WarpRideClientState state) {
        // Left to right the row reads as the keyboard does: keyLeft / keyUp / keyRight, i.e. A-W-D.
        boolean dimmed = state.drifting || state.isOnCooldown()
                || state.phase != WarpRideClientState.PHASE_RIDING;
        renderKey(graphics, font, KEY_CLOCKWISE, KEY_CW_X, labels.clockwise(), dimmed);
        renderKey(graphics, font, KEY_JUMP, KEY_JUMP_X, labels.jump(), dimmed);
        renderKey(graphics, font, KEY_COUNTER_CLOCKWISE, KEY_CCW_X, labels.counterClockwise(), dimmed);
    }

    private static void drawScaledCentered(GuiGraphics graphics, Font font, String text,
                                           int centerX, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, -font.width(text) / 2, 0, color, true);
        graphics.pose().popPose();
    }

    /** Shrink-to-fit, then truncate: a label must never spill out of its 192 px plate. */
    private static void drawFittedCentered(GuiGraphics graphics, Font font, String text,
                                           int centerX, int y, int maxWidth, int color) {
        String shown = text;
        float scale = 2.0f;
        while (scale > 0.55f && font.width(shown) * scale > maxWidth) {
            scale -= 0.1f;
        }
        if (font.width(shown) * scale > maxWidth) {
            shown = font.plainSubstrByWidth(shown, (int) (maxWidth / scale)) + "…";
        }
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, shown, -font.width(shown) / 2, 0, color, true);
        graphics.pose().popPose();
    }
}
