package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.block.teleporter.TeleportPadMode;
import maxigregrze.cobblesafari.network.OpenTeleportPadConfigPayload;
import maxigregrze.cobblesafari.network.SaveTeleportPadConfigPayload;
import maxigregrze.cobblesafari.network.TeleportPadActionPayload;
import maxigregrze.cobblesafari.network.TeleportPadResultPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Creative "command-block-like" teleport-pad config GUI: mode selector, X/Y/Z destination
 * offset (world axes), Check / Auto-detect (server-authoritative search), status line, Save / Exit.
 * The survival variant additionally exposes a hex colour field.
 */
public class TeleportPadConfigScreen extends CobbleSafariConfigScreen {

    private static final int ARROW_WIDTH = 20;
    private static final int OFFSET_FIELD_WIDTH = 96;
    private static final int OFFSET_FIELD_GAP = 10;
    private static final int COLOR_WHITE = 0xE0E0E0;
    private static final int COLOR_GREEN = 0x55FF55;
    private static final int MIN_OFFSET = -100;
    private static final int MAX_OFFSET = 100;

    private final BlockPos pos;
    private final int initialX;
    private final int initialY;
    private final int initialZ;
    private final int initialColor;
    private final boolean allowColor;
    private TeleportPadMode mode;
    private boolean connected;
    private Component status = Component.empty();

    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private EditBox colorBox;
    private int modeRowY;
    private int statusRowY;

    public TeleportPadConfigScreen(OpenTeleportPadConfigPayload initial) {
        super(Component.translatable("gui.cobblesafari.teleport_pad.title"));
        this.pos = initial.pos();
        this.mode = TeleportPadMode.byName(initial.mode());
        this.connected = initial.linked();
        this.initialX = initial.x();
        this.initialY = initial.y();
        this.initialZ = initial.z();
        this.initialColor = initial.color();
        this.allowColor = initial.allowColor();
    }

    @Override
    protected void init() {
        int left = this.panelLeft();
        int y = this.contentTopY();

        this.modeRowY = y;
        addScroll(Button.builder(Component.literal("<"), b -> this.mode = this.mode.prev())
                .bounds(left, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        addScroll(Button.builder(Component.literal(">"), b -> this.mode = this.mode.next())
                .bounds(left + PANEL_WIDTH - ARROW_WIDTH, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        y += 40;

        if (this.allowColor) {
            this.colorBox = makeEditBox(left, y, PANEL_WIDTH, 7, formatColor(this.initialColor));
            this.colorBox.setFilter(s -> s.isEmpty() || s.matches("#?[0-9a-fA-F]{0,6}"));
            y += this.fieldStride();
        }

        this.xBox = makeOffsetBox(left, y, this.initialX);
        this.yBox = makeOffsetBox(left + OFFSET_FIELD_WIDTH + OFFSET_FIELD_GAP, y, this.initialY);
        this.zBox = makeOffsetBox(left + 2 * (OFFSET_FIELD_WIDTH + OFFSET_FIELD_GAP), y, this.initialZ);
        applyConnectedColor();
        y += this.fieldStride();

        addScroll(Button.builder(Component.translatable("gui.cobblesafari.teleport_pad.check"),
                b -> sendAction(TeleportPadActionPayload.Action.CHECK)).bounds(left, y, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        addScroll(Button.builder(Component.translatable("gui.cobblesafari.teleport_pad.autodetect"),
                b -> sendAction(TeleportPadActionPayload.Action.AUTODETECT)).bounds(left + COLUMN_OFFSET, y, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + 8;

        this.statusRowY = y;

        int duoY = this.bottomRowY();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.teleport_pad.save"),
                b -> save()).bounds(left, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.teleport_pad.exit"),
                b -> this.onClose()).bounds(left + COLUMN_OFFSET, duoY, COLUMN_WIDTH, BUTTON_HEIGHT).build());
    }

    private EditBox makeOffsetBox(int x, int y, int value) {
        EditBox box = makeEditBox(x, y, OFFSET_FIELD_WIDTH, 4, Integer.toString(value));
        box.setFilter(s -> s.isEmpty() || s.matches("-?\\d{0,3}"));
        box.setResponder(s -> markEdited());
        return box;
    }

    private void markEdited() {
        if (this.connected) {
            this.connected = false;
            applyConnectedColor();
        }
    }

    private void applyConnectedColor() {
        int color = this.connected ? COLOR_GREEN : COLOR_WHITE;
        if (this.xBox != null) {
            this.xBox.setTextColor(color);
            this.yBox.setTextColor(color);
            this.zBox.setTextColor(color);
        }
    }

    private int parse(EditBox box) {
        try {
            return Math.clamp(Integer.parseInt(box.getValue().trim()), MIN_OFFSET, MAX_OFFSET);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void sendAction(TeleportPadActionPayload.Action action) {
        Services.PLATFORM.sendPayloadToServer(new TeleportPadActionPayload(
                this.pos, action, this.mode.getSerializedName(), parse(this.xBox), parse(this.yBox), parse(this.zBox)));
    }

    private void save() {
        int color = -1;
        if (this.allowColor && this.colorBox != null) {
            color = parseColor(this.colorBox.getValue());
            if (color < 0) {
                color = this.initialColor;
            }
        }
        Services.PLATFORM.sendPayloadToServer(new SaveTeleportPadConfigPayload(
                this.pos, this.mode.getSerializedName(), parse(this.xBox), parse(this.yBox), parse(this.zBox), color));
        this.onClose();
    }

    private static String formatColor(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

    private static int parseColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (!s.matches("[0-9a-fA-F]{6}")) {
            return -1;
        }
        return Integer.parseInt(s, 16);
    }

    public void applyResult(TeleportPadResultPayload payload) {
        if (payload.fills()) {
            this.xBox.setValue(Integer.toString(payload.x()));
            this.yBox.setValue(Integer.toString(payload.y()));
            this.zBox.setValue(Integer.toString(payload.z()));
            this.connected = true;
        } else {
            this.connected = false;
        }
        applyConnectedColor();
        this.status = statusComponent(payload);
    }

    private Component statusComponent(TeleportPadResultPayload payload) {
        return switch (payload.status()) {
            case VALID -> Component.translatable("gui.cobblesafari.teleport_pad.check.valid",
                    payload.x(), payload.y(), payload.z());
            case NOT_FOUND -> Component.translatable("gui.cobblesafari.teleport_pad.check.not_found");
            case WRONG_MODE -> Component.translatable("gui.cobblesafari.teleport_pad.check.wrong_mode");
            case OUT_OF_RANGE -> Component.translatable("gui.cobblesafari.teleport_pad.check.out_of_range");
            case OBSTRUCTED -> Component.translatable("gui.cobblesafari.teleport_pad.check.obstructed");
            case FOUND -> Component.translatable("gui.cobblesafari.teleport_pad.autodetect.found",
                    payload.x(), payload.y(), payload.z());
            case NONE -> Component.translatable("gui.cobblesafari.teleport_pad.autodetect.none");
        };
    }

    @Override
    protected EditBox[] editBoxes() {
        return new EditBox[]{this.colorBox, this.xBox, this.yBox, this.zBox};
    }

    @Override
    protected int scrollContentBottom() {
        return Math.max(super.scrollContentBottom(), this.statusRowY + this.font.lineHeight + 2);
    }

    @Override
    protected void renderScrollContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int left = this.panelLeft();
        g.drawCenteredString(this.font,
                Component.translatable("gui.cobblesafari.teleport_pad.mode." + this.mode.getSerializedName()),
                this.width / 2, this.modeRowY + (BUTTON_HEIGHT - this.font.lineHeight) / 2, TITLE_COLOR);
        if (this.allowColor && this.colorBox != null) {
            g.drawString(this.font, Component.translatable("gui.cobblesafari.teleport_pad.color"),
                    left, labelBaselineY(this.colorBox), LABEL_COLOR, false);
        }
        g.drawString(this.font, "X", this.xBox.getX(), labelBaselineY(this.xBox), LABEL_COLOR, false);
        g.drawString(this.font, "Y", this.yBox.getX(), labelBaselineY(this.yBox), LABEL_COLOR, false);
        g.drawString(this.font, "Z", this.zBox.getX(), labelBaselineY(this.zBox), LABEL_COLOR, false);
        if (!this.status.getString().isEmpty()) {
            g.drawCenteredString(this.font, this.status, this.width / 2, this.statusRowY, TITLE_COLOR);
        }
    }
}
