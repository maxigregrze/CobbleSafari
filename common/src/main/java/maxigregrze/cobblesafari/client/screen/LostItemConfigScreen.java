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
import net.minecraft.network.chat.Component;

public class LostItemConfigScreen extends CobbleSafariConfigScreen {

    private static final int OUT_OF_VIEW = -4096;
    private static final int MODE_BUTTON_Y = 52;

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

    @Override
    protected int contentTopY() {
        return MODE_BUTTON_Y + BUTTON_HEIGHT + 14;
    }

    @Override
    protected boolean hasSecondaryFooterRow() {
        return true;
    }

    @Override
    protected void init() {
        int left = this.panelLeft();

        this.modeButton = addScroll(Button.builder(modeLabel(), b -> {
            this.displayMode = (this.displayMode + 1) % 3;
            this.modeButton.setMessage(modeLabel());
            this.relayoutEditBoxes();
            this.focusFirstFieldForMode();
        }).bounds(left, MODE_BUTTON_Y, PANEL_WIDTH, BUTTON_HEIGHT).build());

        this.poolBerryBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolBerryId());
        this.poolCandyBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolCandyId());
        this.poolBallsBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolBallsId());
        this.poolTreasuresBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolTreasuresId());
        this.minRollBox = makeEditBox(left, 0, COLUMN_WIDTH, 11, Integer.toString(this.initial.minRoll()));
        this.maxRollBox = makeEditBox(left, 0, COLUMN_WIDTH, 11, Integer.toString(this.initial.maxRoll()));
        this.lostLootTableBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.lostItemLootTableId());
        this.lootItemBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.lootItemId());

        int playerListY = this.secondaryRowY();
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cobblesafari.lost_item.reset_player_list").withStyle(ChatFormatting.RED),
                b -> Services.PLATFORM.sendPayloadToServer(new LostItemResetClaimsPayload(this.initial.pos()))
        ).bounds(left, playerListY, PANEL_WIDTH, BUTTON_HEIGHT).build());

        int duoY = this.bottomRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.lost_item.reset"), b -> {
            this.applyFieldsFromMiscConfig();
            this.modeButton.setMessage(modeLabel());
            this.relayoutEditBoxes();
            this.focusFirstFieldForMode();
        }).bounds(left, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

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
        }).bounds(left + COLUMN_OFFSET, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.relayoutEditBoxes();
        this.focusFirstFieldForMode();
    }

    private void focusFirstFieldForMode() {
        switch (this.displayMode) {
            case 1 -> this.focusScroll(this.lostLootTableBox);
            case 2 -> this.focusScroll(this.lootItemBox);
            default -> this.focusScroll(this.poolBerryBox);
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
        int left = this.panelLeft();
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
            showField(this.maxRollBox, left + COLUMN_OFFSET, y);
        } else if (this.displayMode == 1) {
            showField(this.lostLootTableBox, left, y);
        } else {
            showField(this.lootItemBox, left, y);
        }
    }

    @Override
    protected EditBox[] editBoxes() {
        return new EditBox[]{
                this.poolBerryBox, this.poolCandyBox, this.poolBallsBox, this.poolTreasuresBox,
                this.minRollBox, this.maxRollBox, this.lostLootTableBox, this.lootItemBox
        };
    }

    @Override
    protected void renderScrollContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        drawFieldLabel(guiGraphics, this.poolBerryBox, "gui.cobblesafari.lost_item.hint.pool_berry");
        drawFieldLabel(guiGraphics, this.poolCandyBox, "gui.cobblesafari.lost_item.hint.pool_candy");
        drawFieldLabel(guiGraphics, this.poolBallsBox, "gui.cobblesafari.lost_item.hint.pool_balls");
        drawFieldLabel(guiGraphics, this.poolTreasuresBox, "gui.cobblesafari.lost_item.hint.pool_treasures");
        drawFieldLabel(guiGraphics, this.minRollBox, "gui.cobblesafari.lost_item.hint.min_roll");
        drawFieldLabel(guiGraphics, this.maxRollBox, "gui.cobblesafari.lost_item.hint.max_roll");
        drawFieldLabel(guiGraphics, this.lostLootTableBox, "gui.cobblesafari.lost_item.hint.lost_item_loot_table");
        drawFieldLabel(guiGraphics, this.lootItemBox, "gui.cobblesafari.lost_item.hint.loot_item");
    }
}
