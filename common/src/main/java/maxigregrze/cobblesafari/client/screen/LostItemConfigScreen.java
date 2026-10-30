package maxigregrze.cobblesafari.client.screen;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.item.PokeBallItem;
import maxigregrze.cobblesafari.block.misc.LostItemBlockEntity;
import maxigregrze.cobblesafari.block.misc.LostItemSkin;
import maxigregrze.cobblesafari.network.LostItemResetClaimsPayload;
import maxigregrze.cobblesafari.network.LootDefaults;
import maxigregrze.cobblesafari.network.OpenLostItemConfigPayload;
import maxigregrze.cobblesafari.network.SaveLostItemConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LostItemConfigScreen extends CobbleSafariConfigScreen {

    private static final int OUT_OF_VIEW = -4096;
    private static final int MODE_BUTTON_Y = 52;
    /** Fixed rows, kept out of the mode-dependent field area so they never have to be repositioned. */
    private static final int SKIN_ROW_Y = MODE_BUTTON_Y + BUTTON_HEIGHT + 14;
    private static final int POKEBALL_ROW_Y = SKIN_ROW_Y + BUTTON_HEIGHT + 14;
    private static final int ARROW_WIDTH = 20;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    /** Same limit as the payload: a written book or a heavily enchanted stack still fits. */
    private static final int MAX_DATA = 32767;

    private final OpenLostItemConfigPayload initial;
    /** Cobblemon's own ball items, in their creative-tab order (action plan 193, section 6.4). */
    private final List<PokeBallItem> balls = CobblemonItems.pokeBalls;

    private int displayMode;

    private EditBox poolBerryBox;
    private EditBox poolCandyBox;
    private EditBox poolBallsBox;
    private EditBox poolTreasuresBox;
    private EditBox minRollBox;
    private EditBox maxRollBox;
    private EditBox lostLootTableBox;
    private EditBox lootItemBox;
    private EditBox lootItemCountBox;
    private EditBox lootItemDataBox;

    private Button modeButton;

    private LostItemSkin skin;
    private int ballIndex;

    public LostItemConfigScreen(OpenLostItemConfigPayload initial) {
        super(Component.translatable("gui.cobblesafari.lost_item.title"));
        this.initial = initial;
        int m = initial.mode();
        this.displayMode = (m >= 0 && m <= 2) ? m : 0;
        this.skin = LostItemSkin.byName(initial.skin());
        this.ballIndex = this.indexOfBall(initial.pokeball());
    }

    @Override
    protected int contentTopY() {
        return POKEBALL_ROW_Y + BUTTON_HEIGHT + 14;
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

        addScroll(Button.builder(Component.literal("< "), b -> this.skin = this.skin.prev())
                .bounds(left, SKIN_ROW_Y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        addScroll(Button.builder(Component.literal(" >"), b -> this.skin = this.skin.next())
                .bounds(left + PANEL_WIDTH - ARROW_WIDTH, SKIN_ROW_Y, ARROW_WIDTH, BUTTON_HEIGHT).build());

        addScroll(Button.builder(Component.literal("< "), b -> this.stepBall(-1))
                .bounds(left, POKEBALL_ROW_Y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        addScroll(Button.builder(Component.literal(" >"), b -> this.stepBall(1))
                .bounds(left + PANEL_WIDTH - ARROW_WIDTH, POKEBALL_ROW_Y, ARROW_WIDTH, BUTTON_HEIGHT).build());

        this.poolBerryBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolBerryId());
        this.poolCandyBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolCandyId());
        this.poolBallsBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolBallsId());
        this.poolTreasuresBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.poolTreasuresId());
        this.minRollBox = makeEditBox(left, 0, COLUMN_WIDTH, 11, Integer.toString(this.initial.minRoll()));
        this.maxRollBox = makeEditBox(left, 0, COLUMN_WIDTH, 11, Integer.toString(this.initial.maxRoll()));
        this.lostLootTableBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.lostItemLootTableId());
        this.lootItemBox = makeEditBox(left, 0, PANEL_WIDTH, 512, this.initial.lootItemId());
        this.lootItemCountBox = makeEditBox(left, 0, COLUMN_WIDTH, 11, Integer.toString(this.initial.lootItemCount()));
        this.lootItemDataBox = makeEditBox(left, 0, PANEL_WIDTH, MAX_DATA, this.initial.lootItemComponents());

        int playerListY = this.secondaryRowY();
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cobblesafari.lost_item.reset_player_list").withStyle(ChatFormatting.RED),
                b -> Services.PLATFORM.sendPayloadToServer(new LostItemResetClaimsPayload(this.initial.pos()))
        ).bounds(left, playerListY, PANEL_WIDTH, BUTTON_HEIGHT).build());

        int duoY = this.bottomRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.lost_item.reset"), b -> {
            this.applyDefaults();
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
                    this.lootItemBox.getValue(),
                    this.skin.getSerializedName(),
                    this.ballId(),
                    this.lootItemCountBox.getValue(),
                    this.lootItemDataBox.getValue()
            ));
            this.onClose();
        }).bounds(left + COLUMN_OFFSET, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.relayoutEditBoxes();
        this.focusFirstFieldForMode();
    }

    private int indexOfBall(String id) {
        for (int i = 0; i < this.balls.size(); i++) {
            if (this.balls.get(i).getPokeBall().getName().toString().equals(id)) {
                return i;
            }
        }
        return 0;
    }

    private void stepBall(int delta) {
        if (!this.balls.isEmpty()) {
            this.ballIndex = Math.floorMod(this.ballIndex + delta, this.balls.size());
        }
    }

    private String ballId() {
        return this.balls.isEmpty()
                ? LostItemBlockEntity.DEFAULT_POKEBALL
                : this.balls.get(this.ballIndex).getPokeBall().getName().toString();
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

    /** Reset: the server's defaults from the open payload, never the local config (action plan 198, F6). */
    private void applyDefaults() {
        LootDefaults defaults = this.initial.defaults();
        this.poolBerryBox.setValue(defaults.poolBerryId());
        this.poolCandyBox.setValue(defaults.poolCandyId());
        this.poolBallsBox.setValue(defaults.poolBallsId());
        this.poolTreasuresBox.setValue(defaults.poolTreasuresId());
        this.minRollBox.setValue(Integer.toString(defaults.minRoll()));
        this.maxRollBox.setValue(Integer.toString(defaults.maxRoll()));
        this.lostLootTableBox.setValue(this.initial.defaultLootTableId());
        this.lootItemBox.setValue(this.initial.defaultLootItemId());
        this.lootItemCountBox.setValue("1");
        this.lootItemDataBox.setValue("");
        this.ballIndex = this.indexOfBall(LostItemBlockEntity.DEFAULT_POKEBALL);
        int m = this.initial.defaultMode();
        this.displayMode = (m >= 0 && m <= 2) ? m : 0;
    }

    private void relayoutEditBoxes() {
        int left = this.panelLeft();
        int y = this.contentTopY();
        int stride = this.fieldStride();

        for (EditBox box : this.editBoxes()) {
            hideField(box);
        }

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
            y += stride;
            showField(this.lootItemCountBox, left, y);
            y += stride;
            showField(this.lootItemDataBox, left, y);
        }
    }

    @Override
    protected EditBox[] editBoxes() {
        return new EditBox[]{
                this.poolBerryBox, this.poolCandyBox, this.poolBallsBox, this.poolTreasuresBox,
                this.minRollBox, this.maxRollBox, this.lostLootTableBox,
                this.lootItemBox, this.lootItemCountBox, this.lootItemDataBox
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
        drawFieldLabel(guiGraphics, this.lootItemCountBox, "gui.cobblesafari.lost_item.hint.loot_item_count");
        drawFieldLabel(guiGraphics, this.lootItemDataBox, "gui.cobblesafari.lost_item.hint.loot_item_data");

        int left = this.panelLeft();
        guiGraphics.drawString(this.font, Component.translatable("gui.cobblesafari.lost_item.skin"), left,
                SKIN_ROW_Y - LABEL_TO_FIELD_GAP - this.font.lineHeight, LABEL_COLOR, false);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("gui.cobblesafari.lost_item.skin." + this.skin.getSerializedName()),
                left + PANEL_HALF_WIDTH, SKIN_ROW_Y + (BUTTON_HEIGHT - this.font.lineHeight) / 2, TITLE_COLOR);

        guiGraphics.drawString(this.font, Component.translatable("gui.cobblesafari.lost_item.pokeball"), left,
                POKEBALL_ROW_Y - LABEL_TO_FIELD_GAP - this.font.lineHeight, LABEL_COLOR, false);
        if (!this.balls.isEmpty()) {
            PokeBallItem ball = this.balls.get(this.ballIndex);
            Component name = ball.getDescription();
            int rowWidth = ICON_SIZE + ICON_GAP + this.font.width(name);
            int x = left + PANEL_HALF_WIDTH - rowWidth / 2;
            guiGraphics.renderItem(new ItemStack(ball), x, POKEBALL_ROW_Y + (BUTTON_HEIGHT - ICON_SIZE) / 2);
            guiGraphics.drawString(this.font, name, x + ICON_SIZE + ICON_GAP,
                    POKEBALL_ROW_Y + (BUTTON_HEIGHT - this.font.lineHeight) / 2, TITLE_COLOR);
        }
    }
}
