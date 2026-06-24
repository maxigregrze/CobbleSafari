package maxigregrze.cobblesafari.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared base for CobbleSafari's plain (no custom texture) config GUIs.
 *
 * <p>It holds the single source of truth for the common layout so every config
 * screen has an identical feel: a 308-wide panel centered horizontally, a title
 * centered near the top, descriptive field labels in a consistent gray, and a
 * bottom-anchored row of action buttons.
 *
 * <h2>Scrolling</h2>
 * The area between the title and the bottom action row is a <em>scroll
 * viewport</em> that resizes with the window/GUI scale. In-flow content (edit
 * boxes, option buttons, labels, inline text) is registered with
 * {@link #addScroll(AbstractWidget)} (or created via {@link #makeEditBox}) and is
 * clipped to the viewport and scrollable when it does not fit — so it can never
 * overlap the fixed bottom buttons. The title and the action buttons added with
 * {@code addRenderableWidget} stay fixed.
 *
 * <p>Subclasses lay out their scroll widgets exactly as if there were no
 * scrolling (absolute Y from {@link #contentTopY()} downward), draw their labels
 * in {@link #renderScrollContent}, and add their fixed action buttons with
 * {@code addRenderableWidget}. They never override {@link #render} — the base
 * drives it.
 */
public abstract class CobbleSafariConfigScreen extends Screen {

    /** Half the panel width; left edge sits at {@code width/2 - PANEL_HALF_WIDTH}. */
    protected static final int PANEL_HALF_WIDTH = 154;
    /** Full panel / full-width field width. */
    protected static final int PANEL_WIDTH = 308;
    /** Width of a half-row field or button. */
    protected static final int COLUMN_WIDTH = 150;
    /** X offset of the second half-column (left + this). */
    protected static final int COLUMN_OFFSET = 158;

    protected static final int TITLE_Y = 16;
    protected static final int EDIT_HEIGHT = 20;
    protected static final int BUTTON_HEIGHT = 20;
    /** Gap between the bottom of a label and the top of its input field. */
    protected static final int LABEL_TO_FIELD_GAP = 4;
    /** Vertical gap below a field row before the next label. */
    protected static final int AFTER_FIELD_GAP = 12;
    /** Default Y of the first content row below the title. */
    protected static final int CONTENT_TOP_Y_DEFAULT = 48;

    protected static final int LABEL_COLOR = 0xA0A0A0;
    protected static final int TITLE_COLOR = 0xFFFFFF;

    private static final int SCROLL_STEP = 18;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_GAP = 4;
    private static final int SCROLLBAR_MIN_THUMB = 16;
    private static final int SCROLLBAR_TRACK_COLOR = 0x60000000;
    private static final int SCROLLBAR_THUMB_COLOR = 0xFFA0A0A0;
    private static final int SCROLLBAR_THUMB_HOVER_COLOR = 0xFFCFCFCF;

    /** In-flow widgets that live inside the scroll viewport (managed manually, not children). */
    private final List<AbstractWidget> scrollWidgets = new ArrayList<>();
    private double scrollY;
    private boolean draggingThumb;
    private double thumbGrabOffset;

    protected CobbleSafariConfigScreen(Component title) {
        super(title);
    }

    // -- layout -----------------------------------------------------------

    protected int panelLeft() {
        return this.width / 2 - PANEL_HALF_WIDTH;
    }

    /** Y of the first content row. Override to reserve room at the top. */
    protected int contentTopY() {
        return CONTENT_TOP_Y_DEFAULT;
    }

    protected int fieldStride() {
        return this.font.lineHeight + LABEL_TO_FIELD_GAP + EDIT_HEIGHT + AFTER_FIELD_GAP;
    }

    /** Y of the primary bottom button row (Save / Cancel·Reset). */
    protected int bottomRowY() {
        return this.height - 28;
    }

    /** Y of the secondary bottom row above the primary one (e.g. reset player list). */
    protected int secondaryRowY() {
        return this.bottomRowY() - 24;
    }

    /** Whether a secondary fixed footer row exists above the primary one. */
    protected boolean hasSecondaryFooterRow() {
        return false;
    }

    protected int labelBaselineY(EditBox box) {
        return box.getY() - LABEL_TO_FIELD_GAP - this.font.lineHeight;
    }

    // -- scroll viewport --------------------------------------------------

    /** Top of the scroll viewport, just below the title. */
    protected int scrollViewTop() {
        return TITLE_Y + this.font.lineHeight + 6;
    }

    /** Bottom of the scroll viewport, just above the fixed footer rows. */
    protected int scrollViewBottom() {
        int footerTop = this.hasSecondaryFooterRow() ? this.secondaryRowY() : this.bottomRowY();
        return Math.max(this.scrollViewTop(), footerTop - 4);
    }

    /**
     * Unscrolled Y of the bottom edge of the lowest scroll content. Defaults to the
     * lowest registered scroll widget; override to account for trailing text.
     */
    protected int scrollContentBottom() {
        int bottom = this.scrollViewTop();
        for (AbstractWidget w : this.scrollWidgets) {
            bottom = Math.max(bottom, w.getY() + w.getHeight());
        }
        return bottom + 2;
    }

    protected int maxScroll() {
        return Math.max(0, this.scrollContentBottom() - this.scrollViewBottom());
    }

    // -- factories / rendering --------------------------------------------

    /** Registers an in-flow widget that scrolls inside the viewport (not a child). */
    protected <T extends AbstractWidget> T addScroll(T widget) {
        this.scrollWidgets.add(widget);
        return widget;
    }

    protected EditBox makeEditBox(int x, int y, int width, int maxLength, String value) {
        EditBox box = new EditBox(this.font, x, y, width, EDIT_HEIGHT, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(value);
        return addScroll(box);
    }

    /** Sets keyboard focus to one scroll edit box (and clears it from the others). */
    protected void focusScroll(AbstractWidget widget) {
        for (AbstractWidget w : this.scrollWidgets) {
            if (w instanceof EditBox box) {
                box.setFocused(box == widget);
            }
        }
    }

    protected void drawTitle(GuiGraphics g) {
        g.drawCenteredString(this.font, this.title, this.width / 2, TITLE_Y, TITLE_COLOR);
    }

    /** Draws a field's descriptive label in the standard gray, skipping hidden boxes. */
    protected void drawFieldLabel(GuiGraphics g, EditBox box, String key) {
        if (box == null || !box.isVisible()) {
            return;
        }
        g.drawString(this.font, Component.translatable(key), box.getX(), labelBaselineY(box), LABEL_COLOR, false);
    }

    /** Hook: draw labels and other in-flow text. Drawn clipped and scrolled with the widgets. */
    protected void renderScrollContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // optional
    }

    /** Hook: draw fixed overlay text outside the viewport (rare). */
    protected void renderFixedExtras(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // optional
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        this.scrollY = Mth.clamp(this.scrollY, 0, this.maxScroll());

        super.render(g, mouseX, mouseY, partialTick); // fixed footer widgets
        this.drawTitle(g);

        int top = this.scrollViewTop();
        int bottom = this.scrollViewBottom();
        g.enableScissor(0, top, this.width, bottom);
        g.pose().pushPose();
        g.pose().translate(0, -this.scrollY, 0);
        int localMouseY = (int) (mouseY + this.scrollY);
        for (AbstractWidget w : this.scrollWidgets) {
            w.render(g, mouseX, localMouseY, partialTick);
        }
        this.renderScrollContent(g, mouseX, localMouseY, partialTick);
        g.pose().popPose();
        g.disableScissor();

        this.renderFixedExtras(g, mouseX, mouseY, partialTick);
        this.drawScrollbar(g, top, bottom, mouseX, mouseY);
    }

    private void drawScrollbar(GuiGraphics g, int top, int bottom, int mouseX, int mouseY) {
        int max = this.maxScroll();
        if (max <= 0) {
            return;
        }
        int trackX = this.panelLeft() + PANEL_WIDTH + SCROLLBAR_GAP;
        int viewportH = bottom - top;
        int contentH = this.scrollContentBottom() - this.scrollViewTop();
        int thumbH = Math.max(SCROLLBAR_MIN_THUMB, (int) ((long) viewportH * viewportH / contentH));
        int thumbY = top + (int) ((viewportH - thumbH) * (this.scrollY / max));
        boolean hover = mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH && mouseY >= top && mouseY < bottom;
        g.fill(trackX, top, trackX + SCROLLBAR_WIDTH, bottom, SCROLLBAR_TRACK_COLOR);
        g.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbH,
                hover || this.draggingThumb ? SCROLLBAR_THUMB_HOVER_COLOR : SCROLLBAR_THUMB_COLOR);
    }

    // -- scroll input -----------------------------------------------------

    private boolean inViewport(double mouseY) {
        return mouseY >= this.scrollViewTop() && mouseY < this.scrollViewBottom();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int max = this.maxScroll();
        if (max > 0 && this.inViewport(mouseY)) {
            this.scrollY = Mth.clamp(this.scrollY - deltaY * SCROLL_STEP, 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int max = this.maxScroll();
        if (button == 0 && max > 0) {
            int top = this.scrollViewTop();
            int bottom = this.scrollViewBottom();
            int trackX = this.panelLeft() + PANEL_WIDTH + SCROLLBAR_GAP;
            if (mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH && mouseY >= top && mouseY < bottom) {
                int viewportH = bottom - top;
                int contentH = this.scrollContentBottom() - this.scrollViewTop();
                int thumbH = Math.max(SCROLLBAR_MIN_THUMB, (int) ((long) viewportH * viewportH / contentH));
                int thumbY = top + (int) ((viewportH - thumbH) * (this.scrollY / max));
                this.draggingThumb = true;
                this.thumbGrabOffset = (mouseY >= thumbY && mouseY < thumbY + thumbH) ? mouseY - thumbY : thumbH / 2.0;
                this.dragThumbTo(mouseY, top, bottom);
                return true;
            }
        }
        if (this.inViewport(mouseY)) {
            double localY = mouseY + this.scrollY;
            for (AbstractWidget w : this.scrollWidgets) {
                if (w.mouseClicked(mouseX, localY, button)) {
                    this.focusScroll(w);
                    return true;
                }
            }
            this.focusScroll(null);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingThumb) {
            this.dragThumbTo(mouseY, this.scrollViewTop(), this.scrollViewBottom());
            return true;
        }
        for (AbstractWidget w : this.scrollWidgets) {
            if (w instanceof EditBox box && box.isFocused()
                    && box.mouseDragged(mouseX, mouseY + this.scrollY, button, dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void dragThumbTo(double mouseY, int top, int bottom) {
        int max = this.maxScroll();
        int viewportH = bottom - top;
        int contentH = this.scrollContentBottom() - this.scrollViewTop();
        int thumbH = Math.max(SCROLLBAR_MIN_THUMB, (int) ((long) viewportH * viewportH / contentH));
        int travel = viewportH - thumbH;
        if (travel <= 0) {
            return;
        }
        double thumbTop = Mth.clamp(mouseY - this.thumbGrabOffset, top, top + travel);
        this.scrollY = Mth.clamp((thumbTop - top) / travel * max, 0, max);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingThumb = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // -- shared keyboard routing -----------------------------------------

    /** The edit boxes that should receive keyboard input, in focus order. */
    protected abstract EditBox[] editBoxes();

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox box : editBoxes()) {
            if (box != null && box.isVisible() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox box : editBoxes()) {
            if (box != null && box.isVisible() && box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
