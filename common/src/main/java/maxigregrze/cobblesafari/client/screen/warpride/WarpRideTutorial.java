package maxigregrze.cobblesafari.client.screen.warpride;

import com.mojang.blaze3d.platform.InputConstants;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * The tutorial slideshow and the fades around it (action plan 200, section 5).
 *
 * <p>Thirteen pages, each a screenshot and a line of text in a box, advanced by a click. The last
 * one fades to black, the ready goes out, and the ride fades in. A player who has already read it
 * skips straight to that last part: black, ready, fade in - no input at all.</p>
 */
public final class WarpRideTutorial {

    private enum Stage { SLIDES, FADE_OUT, FADE_IN, DONE }

    /** One page: the picture it shows and how many key names its text takes (left, right, forward). */
    private record Page(int picture, int keys) {}

    private static final Page[] PAGES = {
            new Page(1, 0), new Page(1, 0),
            new Page(2, 3), new Page(2, 2),
            new Page(3, -1),                  // forward only
            new Page(4, 0), new Page(5, 0), new Page(6, 0),
            new Page(7, 0), new Page(7, 0),
            new Page(8, 0), new Page(8, 0),
            new Page(4, 0)
    };

    private static final int PICTURE_COUNT = 8;
    private static final ResourceLocation[] PICTURES = new ResourceLocation[PICTURE_COUNT + 1];
    private static final ResourceLocation TEXTBOX = tex("textbox");

    static {
        for (int i = 1; i <= PICTURE_COUNT; i++) {
            PICTURES[i] = tex("picture_" + i);
        }
    }

    // Layout on the 1920x1080 canvas: 10 % side padding, 2 % top and bottom, 3/4 picture, 1/4 text.
    private static final int AREA_X = 192;
    private static final int AREA_W = 1536;
    private static final int PICTURE_Y = 22;
    private static final int PICTURE_H = 777;
    private static final int TEXTBOX_Y = PICTURE_Y + PICTURE_H;
    private static final int TEXTBOX_H = 259;
    private static final int TEXT_PAD_X = 112;
    private static final float TEXT_SCALE = 4.0f;
    private static final int TEXT_LINE_SPACING = 2;

    /** Same length as the warp's own fade to black. */
    private static final long FADE_NANOS = 400_000_000L;

    private final Runnable sendReady;
    private Stage stage;
    private int page;
    private long fadeStartNanos;

    /**
     * @param show      false when the player has already read it: black, then straight into the ride
     * @param sendReady sends the ready that starts the intro; called once, when the black is full
     */
    public WarpRideTutorial(boolean show, Runnable sendReady) {
        this.sendReady = sendReady;
        if (show) {
            this.stage = Stage.SLIDES;
        } else {
            startFadeIn();
        }
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID,
                "textures/gui/warpride/tutorial/" + name + ".png");
    }

    /** True while the pages are up: the ride's own inputs are ignored. */
    public boolean blocksInput() {
        return stage == Stage.SLIDES || stage == Stage.FADE_OUT;
    }

    public boolean onMouseClicked(int button) {
        if (stage != Stage.SLIDES) {
            return blocksInput();
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            advance();
        }
        return true;
    }

    public boolean onKeyPressed(int keyCode) {
        if (stage != Stage.SLIDES) {
            return blocksInput();
        }
        if (keyCode == InputConstants.KEY_SPACE || keyCode == InputConstants.KEY_RETURN
                || keyCode == InputConstants.KEY_NUMPADENTER) {
            advance();
        }
        return true;
    }

    private void advance() {
        page++;
        if (page >= PAGES.length) {
            page = PAGES.length - 1;
            stage = Stage.FADE_OUT;
            fadeStartNanos = System.nanoTime();
        }
    }

    private void startFadeIn() {
        stage = Stage.FADE_IN;
        fadeStartNanos = System.nanoTime();
        sendReady.run();
    }

    /** Drawn last, over the scene and the HUD, inside the canvas transform. */
    public void render(GuiGraphics graphics, Font font, long now) {
        switch (stage) {
            case SLIDES -> renderPage(graphics, font);
            case FADE_OUT -> {
                renderPage(graphics, font);
                double t = (now - fadeStartNanos) / (double) FADE_NANOS;
                WarpRideRenderer.fillViewport(graphics, alpha(t) << 24);
                if (t >= 1.0) {
                    startFadeIn();
                }
            }
            case FADE_IN -> {
                double t = (now - fadeStartNanos) / (double) FADE_NANOS;
                WarpRideRenderer.fillViewport(graphics, alpha(1.0 - t) << 24);
                if (t >= 1.0) {
                    stage = Stage.DONE;
                }
            }
            default -> {
                // DONE: the ride is on its own.
            }
        }
    }

    private static int alpha(double opacity) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, opacity)) * 255.0);
    }

    private void renderPage(GuiGraphics graphics, Font font) {
        WarpRideRenderer.fillViewport(graphics, 0xFF000000);
        Page current = PAGES[page];
        WarpRideRenderer.beginAlphaBlit();
        graphics.blit(PICTURES[current.picture()], AREA_X, PICTURE_Y, 0.0f, 0.0f,
                AREA_W, PICTURE_H, AREA_W, PICTURE_H);
        WarpRideRenderer.beginAlphaBlit();
        graphics.blit(TEXTBOX, AREA_X, TEXTBOX_Y, 0.0f, 0.0f, AREA_W, TEXTBOX_H, AREA_W, TEXTBOX_H);

        int wrapWidth = (int) ((AREA_W - 2 * TEXT_PAD_X) / TEXT_SCALE);
        List<FormattedCharSequence> lines = font.split(pageText(current), wrapWidth);
        int lineHeight = font.lineHeight + TEXT_LINE_SPACING;
        float blockHeight = (lines.size() * lineHeight - TEXT_LINE_SPACING) * TEXT_SCALE;
        graphics.pose().pushPose();
        graphics.pose().translate(AREA_X + TEXT_PAD_X, TEXTBOX_Y + (TEXTBOX_H - blockHeight) / 2.0f, 0.0f);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), 0, i * lineHeight, 0xFFFFFFFF, true);
        }
        graphics.pose().popPose();
    }

    /** The page's text, with the player's own bindings for the keys it names. */
    private Component pageText(Page current) {
        String key = "gui.cobblesafari.warpride.tutorial.page" + (page + 1);
        Options options = Minecraft.getInstance().options;
        Component left = options.keyLeft.getTranslatedKeyMessage();
        Component right = options.keyRight.getTranslatedKeyMessage();
        Component forward = options.keyUp.getTranslatedKeyMessage();
        return switch (current.keys()) {
            case 3 -> Component.translatable(key, left, right, forward);
            case 2 -> Component.translatable(key, left, right);
            case -1 -> Component.translatable(key, forward);
            default -> Component.translatable(key);
        };
    }
}
