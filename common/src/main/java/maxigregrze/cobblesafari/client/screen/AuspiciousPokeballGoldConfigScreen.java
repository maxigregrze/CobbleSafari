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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AuspiciousPokeballGoldConfigScreen extends Screen {

    private static final int LABEL_TO_FIELD_GAP = 4;
    private static final int AFTER_FIELD_GAP = 10;
    private static final int EDIT_HEIGHT = 20;

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

    private int contentTopY() {
        return 96;
    }

    private int bottomDuoRowY() {
        return this.height - 28;
    }

    private int bottomPlayerListRowY() {
        return this.bottomDuoRowY() - 24;
    }

    private int fieldStride() {
        return this.font.lineHeight + LABEL_TO_FIELD_GAP + EDIT_HEIGHT + AFTER_FIELD_GAP;
    }

    private int labelBaselineY(EditBox box) {
        return box.getY() - LABEL_TO_FIELD_GAP - this.font.lineHeight;
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
        int cx = this.width / 2;
        int left = cx - 154;

        int toggleY = 72;
        this.earnableToggleButton = Button.builder(Component.empty(), b -> {
            this.earnableEditing = !this.earnableEditing;
            this.refreshEarnableButtonText();
        }).bounds(left, toggleY, 308, 20).build();
        this.addRenderableWidget(this.earnableToggleButton);
        this.refreshEarnableButtonText();

        this.poolBerryBox = new EditBox(this.font, left, 0, 308, 20, Component.empty());
        this.poolBerryBox.setMaxLength(512);
        this.poolBerryBox.setValue(this.initial.poolBerryId());
        this.addRenderableWidget(this.poolBerryBox);

        this.poolCandyBox = new EditBox(this.font, left, 0, 308, 20, Component.empty());
        this.poolCandyBox.setMaxLength(512);
        this.poolCandyBox.setValue(this.initial.poolCandyId());
        this.addRenderableWidget(this.poolCandyBox);

        this.poolBallsBox = new EditBox(this.font, left, 0, 308, 20, Component.empty());
        this.poolBallsBox.setMaxLength(512);
        this.poolBallsBox.setValue(this.initial.poolBallsId());
        this.addRenderableWidget(this.poolBallsBox);

        this.poolTreasuresBox = new EditBox(this.font, left, 0, 308, 20, Component.empty());
        this.poolTreasuresBox.setMaxLength(512);
        this.poolTreasuresBox.setValue(this.initial.poolTreasuresId());
        this.addRenderableWidget(this.poolTreasuresBox);

        this.minRollBox = new EditBox(this.font, left, 0, 150, 20, Component.empty());
        this.minRollBox.setMaxLength(11);
        this.minRollBox.setValue(Integer.toString(this.initial.minRoll()));
        this.addRenderableWidget(this.minRollBox);

        this.maxRollBox = new EditBox(this.font, left, 0, 150, 20, Component.empty());
        this.maxRollBox.setMaxLength(11);
        this.maxRollBox.setValue(Integer.toString(this.initial.maxRoll()));
        this.addRenderableWidget(this.maxRollBox);

        int playerListY = this.bottomPlayerListRowY();
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cobblesafari.auspicious_pokeball.reset_player_list").withStyle(ChatFormatting.RED),
                b -> Services.PLATFORM.sendPayloadToServer(new AuspiciousPokeballResetClaimsPayload(this.initial.pos()))
        ).bounds(left, playerListY, 308, 20).build());

        int duoY = this.bottomDuoRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.auspicious_pokeball.reset"), b -> {
            this.applyFieldsFromMiscConfig();
            this.earnableEditing = false;
            this.refreshEarnableButtonText();
            this.relayoutEditBoxes();
            this.setInitialFocus(this.poolBerryBox);
        }).bounds(left, duoY, 150, 20).build());

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
        }).bounds(left + 158, duoY, 150, 20).build());

        this.relayoutEditBoxes();
        this.setInitialFocus(this.poolBerryBox);
    }

    private void applyFieldsFromMiscConfig() {
        this.poolBerryBox.setValue(MiscConfig.getAuspiciousPokeballPoolBerryId());
        this.poolCandyBox.setValue(MiscConfig.getAuspiciousPokeballPoolCandyId());
        this.poolBallsBox.setValue(MiscConfig.getAuspiciousPokeballPoolBallsId());
        this.poolTreasuresBox.setValue(MiscConfig.getAuspiciousPokeballPoolTreasuresId());
        this.minRollBox.setValue(Integer.toString(MiscConfig.getAuspiciousPokeballMinRoll()));
        this.maxRollBox.setValue(Integer.toString(MiscConfig.getAuspiciousPokeballMaxRoll()));
    }

    private void relayoutEditBoxes() {
        int left = this.width / 2 - 154;
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
        this.maxRollBox.setPosition(left + 158, y);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox box : allEditBoxes()) {
            if (box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    private EditBox[] allEditBoxes() {
        return new EditBox[]{
                this.poolBerryBox, this.poolCandyBox, this.poolBallsBox, this.poolTreasuresBox,
                this.minRollBox, this.maxRollBox
        };
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox box : allEditBoxes()) {
            if (box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void drawFieldLabel(GuiGraphics g, EditBox box, String hintKey) {
        g.drawString(this.font, Component.translatable(hintKey), box.getX(), this.labelBaselineY(box), 0xA0A0A0, false);
    }

    private void drawEarnersPreview(GuiGraphics g) {
        int left = this.width / 2 - 154;
        int y = 22;
        g.drawString(this.font, Component.translatable("gui.cobblesafari.auspiciouspokeball_gold.earners_label"), left, y, 0xA0A0A0, false);
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
            g.drawString(this.font, "…", left, y, 0xA0A0A0, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);

        int left = this.width / 2 - 154;
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.cobblesafari.auspiciouspokeball_gold.earnable_label"),
                left,
                60,
                0xA0A0A0,
                false
        );

        drawEarnersPreview(guiGraphics);

        drawFieldLabel(guiGraphics, this.poolBerryBox, "gui.cobblesafari.auspicious_pokeball.hint.pool_berry");
        drawFieldLabel(guiGraphics, this.poolCandyBox, "gui.cobblesafari.auspicious_pokeball.hint.pool_candy");
        drawFieldLabel(guiGraphics, this.poolBallsBox, "gui.cobblesafari.auspicious_pokeball.hint.pool_balls");
        drawFieldLabel(guiGraphics, this.poolTreasuresBox, "gui.cobblesafari.auspicious_pokeball.hint.pool_treasures");
        drawFieldLabel(guiGraphics, this.minRollBox, "gui.cobblesafari.auspicious_pokeball.hint.min_roll");
        drawFieldLabel(guiGraphics, this.maxRollBox, "gui.cobblesafari.auspicious_pokeball.hint.max_roll");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
