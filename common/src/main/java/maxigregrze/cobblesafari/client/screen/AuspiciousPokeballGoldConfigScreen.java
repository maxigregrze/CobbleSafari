package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.network.AuspiciousPokeballResetClaimsPayload;
import maxigregrze.cobblesafari.network.OpenAuspiciousPokeballGoldConfigPayload;
import maxigregrze.cobblesafari.network.SaveAuspiciousPokeballGoldConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AuspiciousPokeballGoldConfigScreen extends CobbleSafariConfigScreen {

    // All laid out within the scroll viewport (top to bottom).
    private static final int EARNERS_LABEL_Y = 34;
    private static final int EARNABLE_LABEL_Y = 66;
    private static final int EARNABLE_TOGGLE_Y = 78;
    private static final int CONTENT_TOP_Y = 114;

    private final OpenAuspiciousPokeballGoldConfigPayload initial;
    private boolean earnableEditing;

    private EditBox poolBerryBox;
    private EditBox poolCandyBox;
    private EditBox poolBallsBox;
    private EditBox poolTreasuresBox;
    private EditBox minRollBox;
    private EditBox maxRollBox;
    private Button earnableToggleButton;

    public AuspiciousPokeballGoldConfigScreen(OpenAuspiciousPokeballGoldConfigPayload initial) {
        super(Component.translatable("gui.cobblesafari.auspiciouspokeball_gold.title"));
        this.initial = initial;
        this.earnableEditing = initial.earnable();
    }

    @Override
    protected int contentTopY() {
        return CONTENT_TOP_Y;
    }

    @Override
    protected boolean hasSecondaryFooterRow() {
        return true;
    }

    private void refreshEarnableButtonText() {
        this.earnableToggleButton.setMessage(Component.translatable(
                this.earnableEditing
                        ? "gui.cobblesafari.auspiciouspokeball_gold.earnable.on"
                        : "gui.cobblesafari.auspiciouspokeball_gold.earnable.off"
        ));
    }

    @Override
    protected void init() {
        int left = this.panelLeft();

        this.earnableToggleButton = addScroll(Button.builder(Component.empty(), b -> {
            this.earnableEditing = !this.earnableEditing;
            this.refreshEarnableButtonText();
        }).bounds(left, EARNABLE_TOGGLE_Y, PANEL_WIDTH, BUTTON_HEIGHT).build());
        this.refreshEarnableButtonText();

        this.poolBerryBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolBerryId());
        this.poolCandyBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolCandyId());
        this.poolBallsBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolBallsId());
        this.poolTreasuresBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolTreasuresId());
        this.minRollBox = makeEditBox(left, 0, COLUMN_WIDTH, 11, Integer.toString(this.initial.minRoll()));
        this.maxRollBox = makeEditBox(left, 0, COLUMN_WIDTH, 11, Integer.toString(this.initial.maxRoll()));

        int playerListY = this.secondaryRowY();
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cobblesafari.auspicious_pokeball.reset_player_list").withStyle(ChatFormatting.RED),
                b -> Services.PLATFORM.sendPayloadToServer(new AuspiciousPokeballResetClaimsPayload(this.initial.pos()))
        ).bounds(left, playerListY, PANEL_WIDTH, BUTTON_HEIGHT).build());

        int duoY = this.bottomRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.auspicious_pokeball.reset"), b -> {
            this.applyFieldsFromMiscConfig();
            this.earnableEditing = false;
            this.refreshEarnableButtonText();
            this.relayoutEditBoxes();
            this.focusScroll(this.poolBerryBox);
        }).bounds(left, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.auspicious_pokeball.save"), b -> {
            Services.PLATFORM.sendPayloadToServer(new SaveAuspiciousPokeballGoldConfigPayload(
                    this.initial.pos(),
                    this.poolBerryBox.getValue(),
                    this.poolCandyBox.getValue(),
                    this.poolBallsBox.getValue(),
                    this.poolTreasuresBox.getValue(),
                    this.minRollBox.getValue(),
                    this.maxRollBox.getValue(),
                    this.earnableEditing
            ));
            this.onClose();
        }).bounds(left + COLUMN_OFFSET, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.relayoutEditBoxes();
        this.focusScroll(this.poolBerryBox);
    }

    private void applyFieldsFromMiscConfig() {
        this.poolBerryBox.setValue(MiscConfig.getAuspiciousPokeballGoldPoolBerryId());
        this.poolCandyBox.setValue(MiscConfig.getAuspiciousPokeballGoldPoolCandyId());
        this.poolBallsBox.setValue(MiscConfig.getAuspiciousPokeballGoldPoolBallsId());
        this.poolTreasuresBox.setValue(MiscConfig.getAuspiciousPokeballGoldPoolTreasuresId());
        this.minRollBox.setValue(Integer.toString(MiscConfig.getAuspiciousPokeballGoldMinRoll()));
        this.maxRollBox.setValue(Integer.toString(MiscConfig.getAuspiciousPokeballGoldMaxRoll()));
    }

    private void relayoutEditBoxes() {
        int left = this.panelLeft();
        int y = this.contentTopY();
        int stride = this.fieldStride();

        this.poolBerryBox.setPosition(left, y);
        y += stride;
        this.poolCandyBox.setPosition(left, y);
        y += stride;
        this.poolBallsBox.setPosition(left, y);
        y += stride;
        this.poolTreasuresBox.setPosition(left, y);
        y += stride;
        this.minRollBox.setPosition(left, y);
        this.maxRollBox.setPosition(left + COLUMN_OFFSET, y);
    }

    @Override
    protected EditBox[] editBoxes() {
        return new EditBox[]{
                this.poolBerryBox, this.poolCandyBox, this.poolBallsBox, this.poolTreasuresBox,
                this.minRollBox, this.maxRollBox
        };
    }

    private void drawEarnersPreview(GuiGraphics g) {
        int left = this.panelLeft();
        int y = EARNERS_LABEL_Y;
        g.drawString(this.font, Component.translatable("gui.cobblesafari.auspiciouspokeball_gold.earners_label"), left, y, LABEL_COLOR, false);
        y += this.font.lineHeight + 2;
        List<String> list = this.initial.earners();
        if (list.isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.cobblesafari.auspiciouspokeball_gold.earners_empty").withStyle(ChatFormatting.DARK_GRAY), left, y, 0x808080, false);
            return;
        }
        int maxLines = 2;
        for (int i = 0; i < Math.min(list.size(), maxLines); i++) {
            g.drawString(this.font, list.get(i), left, y, 0xE0E0E0, false);
            y += this.font.lineHeight;
        }
        if (list.size() > maxLines) {
            g.drawString(this.font, "…", left, y, LABEL_COLOR, false);
        }
    }

    @Override
    protected void renderScrollContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = this.panelLeft();
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.cobblesafari.auspiciouspokeball_gold.earnable_label"),
                left,
                EARNABLE_LABEL_Y,
                LABEL_COLOR,
                false
        );

        drawEarnersPreview(guiGraphics);

        drawFieldLabel(guiGraphics, this.poolBerryBox, "gui.cobblesafari.auspiciouspokeball_gold.hint.pool_berry");
        drawFieldLabel(guiGraphics, this.poolCandyBox, "gui.cobblesafari.auspiciouspokeball_gold.hint.pool_candy");
        drawFieldLabel(guiGraphics, this.poolBallsBox, "gui.cobblesafari.auspiciouspokeball_gold.hint.pool_balls");
        drawFieldLabel(guiGraphics, this.poolTreasuresBox, "gui.cobblesafari.auspiciouspokeball_gold.hint.pool_treasures");
        drawFieldLabel(guiGraphics, this.minRollBox, "gui.cobblesafari.auspicious_pokeball.hint.min_roll");
        drawFieldLabel(guiGraphics, this.maxRollBox, "gui.cobblesafari.auspicious_pokeball.hint.max_roll");
    }
}
