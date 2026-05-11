package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.network.LostItemResetClaimsPayload;
import maxigregrze.cobblesafari.network.OpenLostItemConfigPayload;
import maxigregrze.cobblesafari.network.SaveLostItemConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LostItemConfigScreen extends Screen {

    private static final int OUT_OF_VIEW = -4096;
    private static final int EDIT_HEIGHT = 20;
    /** Espace entre le bas du libellé et le haut de la zone de saisie */
    private static final int LABEL_TO_FIELD_GAP = 4;
    /** Espace vertical sous une ligne de champ avant le libellé suivant */
    private static final int AFTER_FIELD_GAP = 10;

    private final OpenLostItemConfigPayload initial;

    private int displayMode;

    private EditBox poolBerryBox;
    private EditBox poolCandyBox;
    private EditBox poolBallsBox;
    private EditBox poolTreasuresBox;
    private EditBox minRollBox;
    private EditBox maxRollBox;
    private EditBox lostLootTableBox;
    private EditBox lootItemBox;

    private Button modeButton;

    public LostItemConfigScreen(OpenLostItemConfigPayload initial) {
        super(Component.translatable("gui.cobblesafari.lost_item.title"));
        this.initial = initial;
        int m = initial.mode();
        this.displayMode = (m >= 0 && m <= 2) ? m : 0;
    }

    private int modeButtonY() {
        return 52;
    }

    private int contentTopY() {
        return this.modeButtonY() + 20 + 14;
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

    @Override
    protected void init() {
        int cx = this.width / 2;
        int left = cx - 154;

        this.modeButton = Button.builder(modeLabel(), b -> {
            this.displayMode = (this.displayMode + 1) % 3;
            this.modeButton.setMessage(modeLabel());
            this.relayoutEditBoxes();
            this.focusFirstFieldForMode();
        }).bounds(left, this.modeButtonY(), 308, 20).build();
        this.addRenderableWidget(this.modeButton);

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

        this.lostLootTableBox = new EditBox(this.font, left, 0, 308, 20, Component.empty());
        this.lostLootTableBox.setMaxLength(512);
        this.lostLootTableBox.setValue(this.initial.lostItemLootTableId());
        this.addRenderableWidget(this.lostLootTableBox);

        this.lootItemBox = new EditBox(this.font, left, 0, 308, 20, Component.empty());
        this.lootItemBox.setMaxLength(512);
        this.lootItemBox.setValue(this.initial.lootItemId());
        this.addRenderableWidget(this.lootItemBox);

        int playerListY = this.bottomPlayerListRowY();
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cobblesafari.lost_item.reset_player_list").withStyle(ChatFormatting.RED),
                b -> Services.PLATFORM.sendPayloadToServer(new LostItemResetClaimsPayload(this.initial.pos()))
        ).bounds(left, playerListY, 308, 20).build());

        int duoY = this.bottomDuoRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.lost_item.reset"), b -> {
            this.applyFieldsFromMiscConfig();
            this.modeButton.setMessage(modeLabel());
            this.relayoutEditBoxes();
            this.focusFirstFieldForMode();
        }).bounds(left, duoY, 150, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.lost_item.save"), b -> {
            Services.PLATFORM.sendPayloadToServer(new SaveLostItemConfigPayload(
                    this.initial.pos(),
                    this.displayMode,
                    this.poolBerryBox.getValue(),
                    this.poolCandyBox.getValue(),
                    this.poolBallsBox.getValue(),
                    this.poolTreasuresBox.getValue(),
                    this.minRollBox.getValue(),
                    this.maxRollBox.getValue(),
                    this.lostLootTableBox.getValue(),
                    this.lootItemBox.getValue()
            ));
            this.onClose();
        }).bounds(left + 158, duoY, 150, 20).build());

        this.relayoutEditBoxes();
        this.focusFirstFieldForMode();
    }

    private void focusFirstFieldForMode() {
        switch (this.displayMode) {
            case 1 -> this.setInitialFocus(this.lostLootTableBox);
            case 2 -> this.setInitialFocus(this.lootItemBox);
            default -> this.setInitialFocus(this.poolBerryBox);
        }
    }

    private Component modeLabel() {
        return Component.translatable("gui.cobblesafari.lost_item.mode_button", this.displayMode);
    }

    private void hideField(EditBox box) {
        box.setVisible(false);
        box.setPosition(OUT_OF_VIEW, OUT_OF_VIEW);
    }

    private void showField(EditBox box, int x, int y) {
        box.setPosition(x, y);
        box.setVisible(true);
    }

    private void applyFieldsFromMiscConfig() {
        this.poolBerryBox.setValue(MiscConfig.getLostItemPoolBerryId());
        this.poolCandyBox.setValue(MiscConfig.getLostItemPoolCandyId());
        this.poolBallsBox.setValue(MiscConfig.getLostItemPoolBallsId());
        this.poolTreasuresBox.setValue(MiscConfig.getLostItemPoolTreasuresId());
        this.minRollBox.setValue(Integer.toString(MiscConfig.getLostItemMinRoll()));
        this.maxRollBox.setValue(Integer.toString(MiscConfig.getLostItemMaxRoll()));
        this.lostLootTableBox.setValue(MiscConfig.getLostItemLootTableId());
        this.lootItemBox.setValue(MiscConfig.getLostItemLootItemId());
        int m = MiscConfig.getLostItemMode();
        this.displayMode = (m >= 0 && m <= 2) ? m : 0;
    }

    private void relayoutEditBoxes() {
        int left = this.width / 2 - 154;
        int y = this.contentTopY();
        int stride = this.fieldStride();

        hideField(this.poolBerryBox);
        hideField(this.poolCandyBox);
        hideField(this.poolBallsBox);
        hideField(this.poolTreasuresBox);
        hideField(this.minRollBox);
        hideField(this.maxRollBox);
        hideField(this.lostLootTableBox);
        hideField(this.lootItemBox);

        if (this.displayMode == 0) {
            showField(this.poolBerryBox, left, y);
            y += stride;
            showField(this.poolCandyBox, left, y);
            y += stride;
            showField(this.poolBallsBox, left, y);
            y += stride;
            showField(this.poolTreasuresBox, left, y);
            y += stride;
            showField(this.minRollBox, left, y);
            showField(this.maxRollBox, left + 158, y);
        } else if (this.displayMode == 1) {
            showField(this.lostLootTableBox, left, y);
        } else {
            showField(this.lootItemBox, left, y);
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox box : allEditBoxes()) {
            if (box != null && box.isVisible() && box.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    private EditBox[] allEditBoxes() {
        return new EditBox[]{
                this.poolBerryBox, this.poolCandyBox, this.poolBallsBox, this.poolTreasuresBox,
                this.minRollBox, this.maxRollBox, this.lostLootTableBox, this.lootItemBox
        };
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox box : allEditBoxes()) {
            if (box != null && box.isVisible() && box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void drawFieldLabel(GuiGraphics g, EditBox box, String hintKey) {
        if (!box.isVisible()) {
            return;
        }
        g.drawString(this.font, Component.translatable(hintKey), box.getX(), this.labelBaselineY(box), 0xA0A0A0, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);

        drawFieldLabel(guiGraphics, this.poolBerryBox, "gui.cobblesafari.lost_item.hint.pool_berry");
        drawFieldLabel(guiGraphics, this.poolCandyBox, "gui.cobblesafari.lost_item.hint.pool_candy");
        drawFieldLabel(guiGraphics, this.poolBallsBox, "gui.cobblesafari.lost_item.hint.pool_balls");
        drawFieldLabel(guiGraphics, this.poolTreasuresBox, "gui.cobblesafari.lost_item.hint.pool_treasures");
        drawFieldLabel(guiGraphics, this.minRollBox, "gui.cobblesafari.lost_item.hint.min_roll");
        drawFieldLabel(guiGraphics, this.maxRollBox, "gui.cobblesafari.lost_item.hint.max_roll");
        drawFieldLabel(guiGraphics, this.lostLootTableBox, "gui.cobblesafari.lost_item.hint.lost_item_loot_table");
        drawFieldLabel(guiGraphics, this.lootItemBox, "gui.cobblesafari.lost_item.hint.loot_item");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
