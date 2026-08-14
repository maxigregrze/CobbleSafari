package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.block.pokestatue.AbstractPokeStatueBlockEntity;
import maxigregrze.cobblesafari.block.pokestatue.PokeStatueVariant;
import maxigregrze.cobblesafari.client.renderer.PokeStatueModelRenderer;
import maxigregrze.cobblesafari.network.OpenPokeStatueConfigPayload;
import maxigregrze.cobblesafari.network.SavePokeStatueConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Config GUI shared by both statue blocks. The {@link PokeStatueVariant} carried by the open
 * payload decides the mode row's labels, how many modes it cycles through, and the scale cap;
 * everything else is identical, so there is one screen rather than two that would drift apart.
 *
 * <p>Reset restores the values the screen was opened with; Confirm saves and closes; Esc closes
 * without saving (inherited from {@code Screen}).</p>
 */
public class PokeStatueConfigScreen extends CobbleSafariConfigScreen {

    private static final int ARROW_WIDTH = 20;
    private static final int STEP_GAP = 4;
    /** Progress steps: single arrows nudge, double arrows jump. */
    private static final float STEP_FINE = 0.01f;
    private static final float STEP_COARSE = 0.10f;

    private final BlockPos pos;
    private final PokeStatueVariant variant;
    private final String initialSpecies;
    private final int initialMode;
    private final float initialScale;
    private final int initialColor;
    private final String initialAnimation;
    private final float initialProgress;

    private int mode;
    /** Selectable animations for the species currently typed; index 0 is always the default pose. */
    private List<String> animations = new ArrayList<>(List.of(""));
    private int animationIndex;
    private final Map<String, PokeStatueModelRenderer.StatueSpecies> speciesCache = new HashMap<>();

    private EditBox speciesBox;
    private EditBox scaleBox;
    private EditBox colorBox;
    private EditBox progressBox;
    private int modeRowY;
    private int animationRowY;

    public PokeStatueConfigScreen(OpenPokeStatueConfigPayload initial) {
        super(Component.translatable("gui.cobblesafari.pokestatue.title"));
        this.pos = initial.pos();
        this.variant = PokeStatueVariant.byId(initial.variant());
        this.initialSpecies = initial.species();
        this.initialMode = initial.mode();
        this.initialScale = initial.scale();
        this.initialColor = initial.color();
        this.initialAnimation = initial.animation();
        this.initialProgress = initial.progress();
        this.mode = initial.mode();
    }

    @Override
    protected void init() {
        int left = this.panelLeft();
        int y = this.contentTopY();

        this.speciesBox = makeEditBox(left, y, PANEL_WIDTH,
                AbstractPokeStatueBlockEntity.MAX_SPECIES_LENGTH, this.initialSpecies);
        this.speciesBox.setResponder(s -> refreshAnimations(false));
        y += this.fieldStride();

        this.modeRowY = y;
        addScroll(Button.builder(Component.literal("<"), b -> this.mode = this.variant.cycleMode(this.mode, -1))
                .bounds(left, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        addScroll(Button.builder(Component.literal(">"), b -> this.mode = this.variant.cycleMode(this.mode, 1))
                .bounds(left + PANEL_WIDTH - ARROW_WIDTH, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        y += rowStride();

        this.animationRowY = y;
        addScroll(Button.builder(Component.literal("<"), b -> cycleAnimation(-1))
                .bounds(left, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        addScroll(Button.builder(Component.literal(">"), b -> cycleAnimation(1))
                .bounds(left + PANEL_WIDTH - ARROW_WIDTH, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        y += rowStride();

        // Progress stepper: « ‹ [field] › »  -- 20+20+4+220+4+20+20 = 308.
        addScroll(Button.builder(Component.literal("«"), b -> nudgeProgress(-STEP_COARSE))
                .bounds(left, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        addScroll(Button.builder(Component.literal("‹"), b -> nudgeProgress(-STEP_FINE))
                .bounds(left + ARROW_WIDTH, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        int fieldX = left + 2 * ARROW_WIDTH + STEP_GAP;
        int fieldWidth = PANEL_WIDTH - 4 * ARROW_WIDTH - 2 * STEP_GAP;
        this.progressBox = makeEditBox(fieldX, y, fieldWidth, 4, formatProgress(this.initialProgress));
        this.progressBox.setFilter(PokeStatueConfigScreen::isProgressDraft);
        addScroll(Button.builder(Component.literal("›"), b -> nudgeProgress(STEP_FINE))
                .bounds(fieldX + fieldWidth + STEP_GAP, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        addScroll(Button.builder(Component.literal("»"), b -> nudgeProgress(STEP_COARSE))
                .bounds(left + PANEL_WIDTH - ARROW_WIDTH, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        y += rowStride();

        this.scaleBox = makeEditBox(left, y, COLUMN_WIDTH, 4, formatScale(this.initialScale));
        this.scaleBox.setFilter(this::isScaleDraft);
        this.colorBox = makeEditBox(left + COLUMN_OFFSET, y, COLUMN_WIDTH, 7, formatColor(this.initialColor));
        this.colorBox.setFilter(s -> s.isEmpty() || s.matches("#?[0-9a-fA-F]{0,6}"));

        int duoY = this.bottomRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.pokestatue.reset"),
                b -> reset()).bounds(left, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.pokestatue.confirm"),
                b -> confirm()).bounds(left + COLUMN_OFFSET, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        refreshAnimations(true);
        this.focusScroll(this.speciesBox);
    }

    /** Height of a label-less button row plus the gap before the next label. */
    private int rowStride() {
        return BUTTON_HEIGHT + AFTER_FIELD_GAP + this.font.lineHeight + LABEL_TO_FIELD_GAP;
    }

    // -- animation list -----------------------------------------------------

    /**
     * Recomputes the selectable animations for the species currently typed. Called on every
     * keystroke in the species field: resolving the poser is a cached lookup and the two animation
     * maps are small, so this is cheap enough not to need debouncing.
     *
     * @param keepInitial on first build, select the stored animation if it is still available.
     */
    private void refreshAnimations(boolean keepInitial) {
        String wanted = keepInitial ? this.initialAnimation : currentAnimation();
        PokeStatueModelRenderer.StatueSpecies info =
                PokeStatueModelRenderer.resolve(this.speciesBox.getValue().trim(), this.speciesCache);
        List<String> names = new ArrayList<>();
        names.add("");
        names.addAll(PokeStatueModelRenderer.listAnimations(info));
        this.animations = names;
        int found = names.indexOf(wanted);
        // A stored animation the new species does not have falls back to the default pose here;
        // the value itself is only overwritten if the player then confirms.
        this.animationIndex = found >= 0 ? found : 0;
    }

    private String currentAnimation() {
        return this.animationIndex >= 0 && this.animationIndex < this.animations.size()
                ? this.animations.get(this.animationIndex) : "";
    }

    private void cycleAnimation(int direction) {
        int count = this.animations.size();
        this.animationIndex = ((this.animationIndex + direction) % count + count) % count;
    }

    // -- progress -----------------------------------------------------------

    /** Accepts partial input on the way to a valid value ("", "0", "0.", "0.4", "1"). */
    private static boolean isProgressDraft(String s) {
        return s.isEmpty() || s.matches("[01](\\.\\d{0,2})?");
    }

    private void nudgeProgress(float delta) {
        float next = AbstractPokeStatueBlockEntity.sanitizeProgress(parseProgress() + delta);
        this.progressBox.setValue(formatProgress(next));
    }

    private float parseProgress() {
        try {
            return AbstractPokeStatueBlockEntity.sanitizeProgress(Float.parseFloat(this.progressBox.getValue().trim()));
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    private static String formatProgress(float value) {
        return trimTrailingZeros(String.format(Locale.ROOT, "%.2f", value));
    }

    // -- scale / colour -----------------------------------------------------

    private boolean isScaleDraft(String s) {
        return s.isEmpty() || s.matches("\\d?(\\.\\d{0,2})?");
    }

    private float parseScale() {
        try {
            return AbstractPokeStatueBlockEntity.sanitizeScale(
                    Float.parseFloat(this.scaleBox.getValue().trim()), this.variant.getMaxScale());
        } catch (NumberFormatException e) {
            return this.initialScale;
        }
    }

    private static String formatScale(float value) {
        return trimTrailingZeros(String.format(Locale.ROOT, "%.2f", value));
    }

    private static String trimTrailingZeros(String s) {
        String t = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return t.isEmpty() ? "0" : t;
    }

    private static String formatColor(int color) {
        return color < 0 ? "" : String.format("#%06X", color & 0xFFFFFF);
    }

    /** Returns {@code -1} for an empty or malformed field, matching the "no colour" sentinel. */
    private static int parseColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return AbstractPokeStatueBlockEntity.NO_COLOR;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (!s.matches("[0-9a-fA-F]{6}")) {
            return AbstractPokeStatueBlockEntity.NO_COLOR;
        }
        return Integer.parseInt(s, 16);
    }

    // -- actions ------------------------------------------------------------

    private void reset() {
        this.speciesBox.setValue(this.initialSpecies);
        this.mode = this.initialMode;
        this.scaleBox.setValue(formatScale(this.initialScale));
        this.colorBox.setValue(formatColor(this.initialColor));
        this.progressBox.setValue(formatProgress(this.initialProgress));
        refreshAnimations(true);
    }

    private void confirm() {
        Services.PLATFORM.sendPayloadToServer(new SavePokeStatueConfigPayload(
                this.pos,
                this.variant.getId(),
                this.speciesBox.getValue().trim(),
                this.mode,
                parseScale(),
                parseColor(this.colorBox.getValue()),
                currentAnimation(),
                parseProgress()));
        this.onClose();
    }

    @Override
    protected EditBox[] editBoxes() {
        return new EditBox[]{this.speciesBox, this.scaleBox, this.colorBox, this.progressBox};
    }

    @Override
    protected void renderScrollContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        drawFieldLabel(g, this.speciesBox, "gui.cobblesafari.pokestatue.species");
        drawFieldLabel(g, this.scaleBox, "gui.cobblesafari.pokestatue.scale");
        drawFieldLabel(g, this.colorBox, "gui.cobblesafari.pokestatue.color");
        drawFieldLabel(g, this.progressBox, "gui.cobblesafari.pokestatue.progress");

        int left = this.panelLeft();
        drawSelector(g, left, this.modeRowY, this.variant.modeLabelKey(),
                Component.translatable(this.variant.modeValueKey(this.mode)));
        String animation = currentAnimation();
        drawSelector(g, left, this.animationRowY, "gui.cobblesafari.pokestatue.animation",
                animation.isEmpty()
                        ? Component.translatable("gui.cobblesafari.pokestatue.animation.default")
                        // Animation ids are Cobblemon identifiers: shown raw, they are not translatable.
                        : Component.literal(animation));
    }

    private void drawSelector(GuiGraphics g, int left, int rowY, String labelKey, Component value) {
        g.drawString(this.font, Component.translatable(labelKey),
                left, rowY - LABEL_TO_FIELD_GAP - this.font.lineHeight, LABEL_COLOR, false);
        g.drawCenteredString(this.font, value,
                left + PANEL_HALF_WIDTH, rowY + (BUTTON_HEIGHT - this.font.lineHeight) / 2, TITLE_COLOR);
    }
}
