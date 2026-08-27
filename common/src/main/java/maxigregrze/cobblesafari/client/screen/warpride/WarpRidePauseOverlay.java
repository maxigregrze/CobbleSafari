package maxigregrze.cobblesafari.client.screen.warpride;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The in-GUI pause layer. Replacing the screen with vanilla's PauseScreen would kill the renderer. */
public final class WarpRidePauseOverlay {

    private static final int PANEL_W = 640;
    private static final int PANEL_H = 320;

    private static final int BUTTON_W = 480;
    private static final int BUTTON_H = 72;

    private boolean open;

    public boolean isOpen() {
        return open;
    }

    public void toggle() {
        open = !open;
    }

    public void close() {
        open = false;
    }

    public void render(GuiGraphics graphics, Font font, int canvasWidth, int canvasHeight) {
        if (!open) {
            return;
        }
        WarpRideRenderer.fillViewport(graphics, 0xB0000000);

        int x = (canvasWidth - PANEL_W) / 2;
        int y = (canvasHeight - PANEL_H) / 2;
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xF0161230);
        graphics.fill(x, y, x + PANEL_W, y + 4, 0xFF7A5CFF);

        drawButton(graphics, font, canvasWidth, resumeY(canvasHeight),
                Component.translatable("gui.cobblesafari.warpride.pause.resume").getString());
        drawButton(graphics, font, canvasWidth, quitY(canvasHeight),
                Component.translatable("gui.cobblesafari.warpride.pause.quit").getString());
    }

    private static int resumeY(int canvasHeight) {
        return (canvasHeight - PANEL_H) / 2 + 90;
    }

    private static int quitY(int canvasHeight) {
        return (canvasHeight - PANEL_H) / 2 + 190;
    }

    private static void drawButton(GuiGraphics graphics, Font font, int canvasWidth, int y, String label) {
        int x = (canvasWidth - BUTTON_W) / 2;
        graphics.fill(x, y, x + BUTTON_W, y + BUTTON_H, 0xFF2A2350);
        graphics.pose().pushPose();
        graphics.pose().translate(canvasWidth / 2.0f, y + BUTTON_H / 2.0f - 12.0f, 0.0f);
        graphics.pose().scale(2.0f, 2.0f, 1.0f);
        graphics.drawString(font, label, -font.width(label) / 2, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    /** @return 1 for Resume, 2 for Quit, 0 for neither. Coordinates are in canvas units. */
    public int hit(double canvasX, double canvasY, int canvasWidth, int canvasHeight) {
        if (!open) {
            return 0;
        }
        int x = (canvasWidth - BUTTON_W) / 2;
        if (canvasX < x || canvasX > x + BUTTON_W) {
            return 0;
        }
        int resume = resumeY(canvasHeight);
        if (canvasY >= resume && canvasY <= resume + BUTTON_H) {
            return 1;
        }
        int quit = quitY(canvasHeight);
        if (canvasY >= quit && canvasY <= quit + BUTTON_H) {
            return 2;
        }
        return 0;
    }
}
