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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Creative "command-block-like" teleport-pad config GUI: mode selector, X/Y/Z destination
 * offset (world axes), Check / Auto-detect (server-authoritative search), status line, Save / Exit.
 * The survival variant additionally exposes a hex colour field.
 */
public class TeleportPadConfigScreen extends Screen {

    private static final int FIELD_WIDTH = 58;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 95;
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
    private int colorRowY;
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
        int left = this.width / 2 - 100;
        int y = 40;

        this.modeRowY = y;
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> this.mode = this.mode.prev())
                .bounds(left, y, 20, FIELD_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> this.mode = this.mode.next())
                .bounds(left + 180, y, 20, FIELD_HEIGHT).build());
        y += 44;

        if (this.allowColor) {
            this.colorRowY = y;
            this.colorBox = new EditBox(this.font, left, y, 200, FIELD_HEIGHT, Component.empty());
            this.colorBox.setMaxLength(7);
            this.colorBox.setFilter(s -> s.isEmpty() || s.matches("#?[0-9a-fA-F]{0,6}"));
            this.colorBox.setValue(formatColor(this.initialColor));
            this.addRenderableWidget(this.colorBox);
            y += 32;
        }

        this.xBox = makeOffsetBox(left, y, this.initialX);
        this.yBox = makeOffsetBox(left + 71, y, this.initialY);
        this.zBox = makeOffsetBox(left + 142, y, this.initialZ);
        applyConnectedColor();
        y += 32;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.teleport_pad.check"),
                b -> sendAction(TeleportPadActionPayload.Action.CHECK)).bounds(left, y, BUTTON_WIDTH, FIELD_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.teleport_pad.autodetect"),
                b -> sendAction(TeleportPadActionPayload.Action.AUTODETECT)).bounds(left + 105, y, BUTTON_WIDTH, FIELD_HEIGHT).build());
        y += 28;

        this.statusRowY = y;
        y += 24;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.teleport_pad.save"),
                b -> save()).bounds(left, y, BUTTON_WIDTH, FIELD_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblesafari.teleport_pad.exit"),
                b -> this.onClose()).bounds(left + 105, y, BUTTON_WIDTH, FIELD_HEIGHT).build());
    }

    private EditBox makeOffsetBox(int x, int y, int value) {
        EditBox box = new EditBox(this.font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        box.setMaxLength(4);
        box.setFilter(s -> s.isEmpty() || s.matches("-?\\d{0,3}"));
        box.setValue(Integer.toString(value));
        box.setResponder(s -> markEdited());
        this.addRenderableWidget(box);
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        int left = this.width / 2 - 100;
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable("gui.cobblesafari.teleport_pad.mode." + this.mode.getSerializedName()),
                this.width / 2, this.modeRowY + (FIELD_HEIGHT - this.font.lineHeight) / 2, 0xFFFFFF);
        if (this.allowColor && this.colorBox != null) {
            g.drawString(this.font, Component.translatable("gui.cobblesafari.teleport_pad.color"),
                    left, this.colorRowY - 10, 0xA0A0A0, false);
        }
        g.drawString(this.font, "X", left, this.xBox.getY() - 10, 0xA0A0A0, false);
        g.drawString(this.font, "Y", left + 71, this.yBox.getY() - 10, 0xA0A0A0, false);
        g.drawString(this.font, "Z", left + 142, this.zBox.getY() - 10, 0xA0A0A0, false);
        if (!this.status.getString().isEmpty()) {
            g.drawCenteredString(this.font, this.status, this.width / 2, this.statusRowY, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
