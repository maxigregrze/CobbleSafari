package maxigregrze.cobblesafari.client.screen.warpride;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * "Press Esc again to leave" (action plan 189, section 10): armed by a first Esc, confirmed by a
 * second one within three seconds. It replaces the pause overlay - the ride never pauses any more,
 * single-player included - and only ever changes the interface: leaving still goes through the
 * screen's ordinary close, which ends the session aborted and keeps the spent attempt.
 */
public final class WarpRideExitPrompt {

    private static final long WINDOW_NANOS = 3_000_000_000L;

    private static final int BAND_TOP = 24;
    private static final int BAND_HEIGHT = 64;
    private static final int BAND_COLOR = 0xB0000000;
    private static final float TEXT_SCALE = 2.5f;

    private long armedUntil;

    /** @return true when this Esc confirms the exit, false when it only armed the prompt */
    public boolean onEscape(long now) {
        if (now < armedUntil) {
            armedUntil = 0L;
            return true;
        }
        armedUntil = now + WINDOW_NANOS;
        return false;
    }

    /** Drawn in canvas units; the band spans the whole window, letterbox bands included. */
    public void render(GuiGraphics graphics, Font font, int canvasWidth, long now) {
        if (now >= armedUntil) {
            return;
        }
        graphics.fill(WarpRideRenderer.viewLeft(), BAND_TOP, WarpRideRenderer.viewRight(),
                BAND_TOP + BAND_HEIGHT, BAND_COLOR);
        String text = Component.translatable("gui.cobblesafari.warpride.exit_prompt").getString();
        graphics.pose().pushPose();
        graphics.pose().translate(canvasWidth / 2.0f, BAND_TOP + (BAND_HEIGHT - font.lineHeight * TEXT_SCALE) / 2.0f, 0.0f);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        graphics.drawString(font, text, -font.width(text) / 2, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }
}
