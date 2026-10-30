package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.network.SaveOnboardingPayload;
import maxigregrze.cobblesafari.onboarding.OnboardingChat;
import maxigregrze.cobblesafari.onboarding.OnboardingChoices;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Consumer;

/**
 * The admin onboarding screen (action plan 198, section 7): which species addons are installed, and
 * the main switches of the mod. Save sends the boxes to the server; Esc leaves without saving.
 */
public class OnboardingScreen extends CobbleSafariConfigScreen {

    private static final String KEY = "gui.cobblesafari.onboarding.";
    private static final EditBox[] NO_EDIT_BOXES = new EditBox[0];
    private static final int INTRO_LINE_SPACING = 1;
    private static final int SECTION_GAP = 12;
    private static final int HEADER_TO_LEAD = 12;
    private static final int HEADER_TO_BOXES = 14;
    private static final int CHECKBOX_INDENT = 8;
    private static final int CHECKBOX_STRIDE = 22;
    /** The last box's stride already leaves a gap, so the next header needs less than SECTION_GAP. */
    private static final int BOXES_TO_HEADER = 6;
    private static final int INTRO_COLOR = 0xE0E0E0;

    private boolean legendariesAndMythicals;
    private boolean ultraBeasts;
    private boolean paradox;
    private boolean missingRegular;
    private boolean portalSpawns;
    private boolean portalNotification;
    private boolean rewardBalloons;
    private boolean megaShowdown;

    // Text positions worked out by init(): the intro's height depends on the language.
    private List<FormattedCharSequence> introLines = List.of();
    private int introY;
    private int addonsHeaderY;
    private int addonsLeadY;
    private int mainHeaderY;

    public OnboardingScreen(OnboardingChoices initial) {
        super(Component.translatable(KEY + "title"));
        this.legendariesAndMythicals = initial.legendariesAndMythicals();
        this.ultraBeasts = initial.ultraBeasts();
        this.paradox = initial.paradox();
        this.missingRegular = initial.missingRegular();
        this.portalSpawns = initial.portalSpawns();
        this.portalNotification = initial.portalNotification();
        this.rewardBalloons = initial.rewardBalloons();
        this.megaShowdown = initial.megaShowdown();
    }

    @Override
    protected void init() {
        int left = this.panelLeft();
        int y = this.contentTopY();

        this.introLines = this.font.split(Component.translatable(KEY + "intro"), PANEL_WIDTH);
        this.introY = y;
        y += this.introLines.size() * (this.font.lineHeight + INTRO_LINE_SPACING) + SECTION_GAP;

        this.addonsHeaderY = y;
        this.addonsLeadY = y + HEADER_TO_LEAD;
        y = this.addonsLeadY + HEADER_TO_BOXES;
        y = this.checkbox(left, y, "addon.legendaries", this.legendariesAndMythicals, v -> this.legendariesAndMythicals = v);
        y = this.checkbox(left, y, "addon.ultra_beasts", this.ultraBeasts, v -> this.ultraBeasts = v);
        y = this.checkbox(left, y, "addon.paradox", this.paradox, v -> this.paradox = v);
        y = this.checkbox(left, y, "addon.missing_regular", this.missingRegular, v -> this.missingRegular = v);

        this.mainHeaderY = y + BOXES_TO_HEADER;
        y = this.mainHeaderY + HEADER_TO_BOXES;
        y = this.checkbox(left, y, "setting.portal_spawns", this.portalSpawns, v -> this.portalSpawns = v);
        y = this.checkbox(left, y, "setting.portal_notification", this.portalNotification, v -> this.portalNotification = v);
        y = this.checkbox(left, y, "setting.reward_balloons", this.rewardBalloons, v -> this.rewardBalloons = v);
        this.checkbox(left, y, "setting.mega_showdown", this.megaShowdown, v -> this.megaShowdown = v);

        this.addRenderableWidget(Button.builder(Component.translatable(KEY + "save"), b -> this.save())
                .bounds(left, this.bottomRowY(), PANEL_WIDTH, BUTTON_HEIGHT).build());
    }

    /** Adds one indented box with its tooltip; returns the Y of the next row. */
    private int checkbox(int left, int y, String key, boolean selected, Consumer<Boolean> onChange) {
        addScroll(Checkbox.builder(Component.translatable(KEY + key), this.font)
                .pos(left + CHECKBOX_INDENT, y)
                .maxWidth(PANEL_WIDTH - CHECKBOX_INDENT)
                .selected(selected)
                .tooltip(Tooltip.create(Component.translatable(KEY + key + ".tooltip")))
                .onValueChange((box, value) -> onChange.accept(value))
                .build());
        return y + CHECKBOX_STRIDE;
    }

    private OnboardingChoices currentChoices() {
        return new OnboardingChoices(this.legendariesAndMythicals, this.ultraBeasts, this.paradox,
                this.missingRegular, this.portalSpawns, this.portalNotification, this.rewardBalloons,
                this.megaShowdown);
    }

    /** The server writes the configs and confirms in chat (decision 7.4). */
    private void save() {
        Services.PLATFORM.sendPayloadToServer(new SaveOnboardingPayload(this.currentChoices()));
        // Not onClose(): that path is Esc's and would print the "not saved" reminder.
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    /** Esc: leaves without saving and reminds how to come back (section 7.4). */
    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    OnboardingChat.rerunHint("cobblesafari.onboarding.chat.rerun"), false);
        }
        super.onClose();
    }

    @Override
    protected void renderScrollContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int left = this.panelLeft();
        int y = this.introY;
        for (FormattedCharSequence line : this.introLines) {
            g.drawString(this.font, line, left, y, INTRO_COLOR, false);
            y += this.font.lineHeight + INTRO_LINE_SPACING;
        }
        g.drawString(this.font, Component.translatable(KEY + "section.addons"), left, this.addonsHeaderY, TITLE_COLOR, false);
        g.drawString(this.font, Component.translatable(KEY + "addons.lead"), left, this.addonsLeadY, LABEL_COLOR, false);
        g.drawString(this.font, Component.translatable(KEY + "section.main"), left, this.mainHeaderY, TITLE_COLOR, false);
    }

    @Override
    protected EditBox[] editBoxes() {
        return NO_EDIT_BOXES;
    }
}
