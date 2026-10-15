package maxigregrze.cobblesafari.client.screen;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.dungeon.PortalSpawnerMode;
import maxigregrze.cobblesafari.block.dungeon.PortalSpawnerStatus;
import maxigregrze.cobblesafari.network.OpenPortalSpawnerConfigPayload;
import maxigregrze.cobblesafari.network.SavePortalSpawnerConfigPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Config GUI of the Portal Spawner (action plan 188, section 12.7). Close discards, Save sends the
 * values to the server, which clamps them. The status line counts down locally from the snapshot.
 */
public class PortalSpawnerConfigScreen extends CobbleSafariConfigScreen {

    private static final String KEY = "gui.cobblesafari.portal_spawner.";
    private static final int OUT_OF_VIEW = -4096;
    private static final int MAX_DIGITS = 6;
    private static final int STATUS_Y = 36;
    private static final int ENABLED_Y = 50;
    private static final int DESTINATION_Y = 74;

    private final OpenPortalSpawnerConfigPayload initial;
    private final List<String> destinationValues;

    private boolean enabled;
    private String destination;
    private PortalSpawnerMode mode;
    private String lifespanText;
    private String intervalText;
    private String minIntervalText;
    private String maxIntervalText;
    private String minSignalText;
    private long openedAtGameTime = Long.MIN_VALUE;

    private EditBox lifespanBox;
    private EditBox intervalBox;
    private EditBox minIntervalBox;
    private EditBox maxIntervalBox;
    private EditBox minSignalBox;

    public PortalSpawnerConfigScreen(OpenPortalSpawnerConfigPayload initial) {
        super(Component.translatable(KEY + "title"));
        this.initial = initial;
        this.destinationValues = new ArrayList<>();
        this.destinationValues.add("");
        for (String id : initial.destinations()) {
            if (!id.isEmpty() && !this.destinationValues.contains(id)) {
                this.destinationValues.add(id);
            }
        }
        this.enabled = initial.enabled();
        this.destination = this.destinationValues.contains(initial.destination()) ? initial.destination() : "";
        this.mode = PortalSpawnerMode.byOrdinal(initial.mode());
        this.lifespanText = Integer.toString(initial.lifespanSeconds());
        this.intervalText = Integer.toString(initial.intervalSeconds());
        this.minIntervalText = Integer.toString(initial.minIntervalSeconds());
        this.maxIntervalText = Integer.toString(initial.maxIntervalSeconds());
        this.minSignalText = Integer.toString(initial.minSignal());
    }

    private int row3Y() {
        return DESTINATION_Y + BUTTON_HEIGHT + AFTER_FIELD_GAP + this.font.lineHeight + LABEL_TO_FIELD_GAP;
    }

    @Override
    protected void init() {
        if (this.openedAtGameTime == Long.MIN_VALUE && this.minecraft != null && this.minecraft.level != null) {
            this.openedAtGameTime = this.minecraft.level.getGameTime();
        }
        int left = this.panelLeft();
        int row3 = this.row3Y();

        addScroll(Checkbox.builder(Component.translatable(KEY + "enabled"), this.font)
                .pos(left, ENABLED_Y)
                .selected(this.enabled)
                .onValueChange((checkbox, value) -> this.enabled = value)
                .build());

        addScroll(CycleButton.<String>builder(this::destinationLabel)
                .withValues(this.destinationValues)
                .withInitialValue(this.destination)
                .create(left, DESTINATION_Y, PANEL_WIDTH, BUTTON_HEIGHT,
                        Component.translatable(KEY + "destination"), (button, value) -> this.destination = value));

        this.lifespanBox = numericBox(left, row3, this.lifespanText, s -> this.lifespanText = s);
        addScroll(CycleButton.<PortalSpawnerMode>builder(m -> Component.translatable(m.translationKey()))
                .withValues(PortalSpawnerMode.values())
                .withInitialValue(this.mode)
                .create(left + COLUMN_OFFSET, row3, COLUMN_WIDTH, BUTTON_HEIGHT,
                        Component.translatable(KEY + "mode"), (button, value) -> {
                            this.mode = value;
                            this.relayoutModeFields();
                        }));

        this.intervalBox = numericBox(left, 0, this.intervalText, s -> this.intervalText = s);
        this.minIntervalBox = numericBox(left, 0, this.minIntervalText, s -> this.minIntervalText = s);
        this.maxIntervalBox = numericBox(left, 0, this.maxIntervalText, s -> this.maxIntervalText = s);
        this.minSignalBox = numericBox(left, 0, this.minSignalText, s -> this.minSignalText = s);

        int footerY = this.bottomRowY();
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + "close"), b -> this.onClose())
                .bounds(left, footerY, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + "save"), b -> {
            this.save();
            this.onClose();
        }).bounds(left + COLUMN_OFFSET, footerY, COLUMN_WIDTH, BUTTON_HEIGHT).build());

        this.relayoutModeFields();
    }

    private EditBox numericBox(int x, int y, String value, java.util.function.Consumer<String> responder) {
        EditBox box = makeEditBox(x, y, COLUMN_WIDTH, MAX_DIGITS, value);
        box.setFilter(s -> s.matches("\\d{0," + MAX_DIGITS + "}"));
        box.setResponder(responder);
        return box;
    }

    private Component destinationLabel(String id) {
        if (id.isEmpty()) {
            return Component.translatable(KEY + "destination.random");
        }
        return Component.translatable("dimension." + CobbleSafari.MOD_ID + "." + id);
    }

    private void relayoutModeFields() {
        int left = this.panelLeft();
        int row4 = this.row3Y() + this.fieldStride();
        hideField(this.intervalBox);
        hideField(this.minIntervalBox);
        hideField(this.maxIntervalBox);
        hideField(this.minSignalBox);
        switch (this.mode) {
            case INTERVAL -> showField(this.intervalBox, left, row4);
            case RANDOM_INTERVAL -> {
                showField(this.minIntervalBox, left, row4);
                showField(this.maxIntervalBox, left + COLUMN_OFFSET, row4);
            }
            case REDSTONE -> showField(this.minSignalBox, left, row4);
        }
    }

    private static void hideField(EditBox box) {
        box.setVisible(false);
        box.setPosition(OUT_OF_VIEW, OUT_OF_VIEW);
    }

    private static void showField(EditBox box, int x, int y) {
        box.setPosition(x, y);
        box.setVisible(true);
    }

    private void save() {
        Services.PLATFORM.sendPayloadToServer(new SavePortalSpawnerConfigPayload(
                this.initial.pos(),
                this.enabled,
                this.destination,
                parseOr(this.lifespanText, this.initial.lifespanSeconds()),
                this.mode.ordinal(),
                parseOr(this.intervalText, this.initial.intervalSeconds()),
                parseOr(this.minIntervalText, this.initial.minIntervalSeconds()),
                parseOr(this.maxIntervalText, this.initial.maxIntervalSeconds()),
                parseOr(this.minSignalText, this.initial.minSignal())));
    }

    private static int parseOr(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected EditBox[] editBoxes() {
        return new EditBox[]{
                this.lifespanBox, this.intervalBox, this.minIntervalBox, this.maxIntervalBox, this.minSignalBox
        };
    }

    @Override
    protected void renderScrollContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.drawString(this.font, this.statusLine(), this.panelLeft(), STATUS_Y, TITLE_COLOR, false);
        drawFieldLabel(g, this.lifespanBox, KEY + "lifespan");
        drawFieldLabel(g, this.intervalBox, KEY + "interval");
        drawFieldLabel(g, this.minIntervalBox, KEY + "min_interval");
        drawFieldLabel(g, this.maxIntervalBox, KEY + "max_interval");
        drawFieldLabel(g, this.minSignalBox, KEY + "min_signal");
    }

    private Component statusLine() {
        PortalSpawnerStatus status = PortalSpawnerStatus.byOrdinal(this.initial.status());
        if (!status.hasCountdown()) {
            return Component.translatable(status.translationKey());
        }
        long elapsed = 0;
        if (this.openedAtGameTime != Long.MIN_VALUE && this.minecraft != null && this.minecraft.level != null) {
            elapsed = Math.max(0, this.minecraft.level.getGameTime() - this.openedAtGameTime);
        }
        long remaining = Math.max(0, this.initial.statusRemainingTicks() - elapsed);
        return Component.translatable(status.translationKey(), formatTicks(remaining));
    }

    private static String formatTicks(long ticks) {
        long totalSeconds = ticks / 20;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, seconds)
                : String.format("%d:%02d", minutes, seconds);
    }
}
