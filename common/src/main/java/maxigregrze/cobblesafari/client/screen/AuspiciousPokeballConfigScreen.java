package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.network.AuspiciousPokeballResetClaimsPayload;
import maxigregrze.cobblesafari.network.OpenAuspiciousPokeballConfigPayload;
import maxigregrze.cobblesafari.network.SaveAuspiciousPokeballConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class AuspiciousPokeballConfigScreen extends CobbleSafariConfigScreen {

    private final OpenAuspiciousPokeballConfigPayload initial;

    private EditBox poolBerryBox;
    private EditBox poolCandyBox;
    private EditBox poolBallsBox;
    private EditBox poolTreasuresBox;
    private EditBox minRollBox;
    private EditBox maxRollBox;

    public AuspiciousPokeballConfigScreen(OpenAuspiciousPokeballConfigPayload initial) {
        super(Component.translatable("gui.cobblesafari.auspicious_pokeball.title"));
        this.initial = initial;
    }

    @Override
    protected boolean hasSecondaryFooterRow() {
        return true;
    }

    @Override
    protected void init() {
        int left = this.panelLeft();

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
            this.relayoutEditBoxes();
            this.focusScroll(this.poolBerryBox);
        }).bounds(left, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.auspicious_pokeball.save"), b -> {
            Services.PLATFORM.sendPayloadToServer(new SaveAuspiciousPokeballConfigPayload(
                    this.initial.pos(),
                    this.poolBerryBox.getValue(),
                    this.poolCandyBox.getValue(),
                    this.poolBallsBox.getValue(),
                    this.poolTreasuresBox.getValue(),
                    this.minRollBox.getValue(),
                    this.maxRollBox.getValue()
            ));
            this.onClose();
        }).bounds(left + COLUMN_OFFSET, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.relayoutEditBoxes();
        this.focusScroll(this.poolBerryBox);
    }

    /** True when this screen is editing the small variant ({@code cobblesafari:auspiciouspokeball_small}). */
    private boolean isSmallVariant() {
        return this.minecraft != null
                && this.minecraft.level != null
                && this.minecraft.level.getBlockState(this.initial.pos()).getBlock() == ModBlocks.AUSPICIOUS_POKEBALL_SMALL;
    }

    private void applyFieldsFromMiscConfig() {
        if (isSmallVariant()) {
            this.poolBerryBox.setValue(MiscConfig.getAuspiciousPokeballSmallPoolBerryId());
            this.poolCandyBox.setValue(MiscConfig.getAuspiciousPokeballSmallPoolCandyId());
            this.poolBallsBox.setValue(MiscConfig.getAuspiciousPokeballSmallPoolBallsId());
            this.poolTreasuresBox.setValue(MiscConfig.getAuspiciousPokeballSmallPoolTreasuresId());
            this.minRollBox.setValue(Integer.toString(MiscConfig.getAuspiciousPokeballSmallMinRoll()));
            this.maxRollBox.setValue(Integer.toString(MiscConfig.getAuspiciousPokeballSmallMaxRoll()));
        } else {
            this.poolBerryBox.setValue(MiscConfig.getAuspiciousPokeballPoolBerryId());
            this.poolCandyBox.setValue(MiscConfig.getAuspiciousPokeballPoolCandyId());
            this.poolBallsBox.setValue(MiscConfig.getAuspiciousPokeballPoolBallsId());
            this.poolTreasuresBox.setValue(MiscConfig.getAuspiciousPokeballPoolTreasuresId());
            this.minRollBox.setValue(Integer.toString(MiscConfig.getAuspiciousPokeballMinRoll()));
            this.maxRollBox.setValue(Integer.toString(MiscConfig.getAuspiciousPokeballMaxRoll()));
        }
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

    @Override
    protected void renderScrollContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        String poolPrefix = isSmallVariant()
                ? "gui.cobblesafari.auspiciouspokeball_small.hint."
                : "gui.cobblesafari.auspicious_pokeball.hint.";
        drawFieldLabel(guiGraphics, this.poolBerryBox, poolPrefix + "pool_berry");
        drawFieldLabel(guiGraphics, this.poolCandyBox, poolPrefix + "pool_candy");
        drawFieldLabel(guiGraphics, this.poolBallsBox, poolPrefix + "pool_balls");
        drawFieldLabel(guiGraphics, this.poolTreasuresBox, poolPrefix + "pool_treasures");
        drawFieldLabel(guiGraphics, this.minRollBox, "gui.cobblesafari.auspicious_pokeball.hint.min_roll");
        drawFieldLabel(guiGraphics, this.maxRollBox, "gui.cobblesafari.auspicious_pokeball.hint.max_roll");
    }
}
