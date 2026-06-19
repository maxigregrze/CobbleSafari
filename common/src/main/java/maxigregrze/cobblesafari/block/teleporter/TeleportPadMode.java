package maxigregrze.cobblesafari.block.teleporter;

import net.minecraft.util.StringRepresentable;

/**
 * Teleport pad mode = direction of the destination pad.
 * <ul>
 *   <li>{@code TOP} — partner is up (L-shape: N up + 1 toward facing), connects to {@code BOTTOM}.</li>
 *   <li>{@code BOTTOM} — partner is down (L-shape: 1 toward facing + N down), connects to {@code TOP}.</li>
 *   <li>{@code FRONT} — partner is forward (straight line ±1), connects to another {@code FRONT}.</li>
 * </ul>
 */
public enum TeleportPadMode implements StringRepresentable {
    TOP("top"),
    BOTTOM("bottom"),
    FRONT("front");

    private final String name;

    TeleportPadMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public TeleportPadMode next() {
        TeleportPadMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public TeleportPadMode prev() {
        TeleportPadMode[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }

    /** Compatible partner mode: TOP↔BOTTOM, FRONT↔FRONT. */
    public TeleportPadMode opposite() {
        return switch (this) {
            case TOP -> BOTTOM;
            case BOTTOM -> TOP;
            case FRONT -> FRONT;
        };
    }

    public static TeleportPadMode byName(String name) {
        if (name != null) {
            for (TeleportPadMode mode : values()) {
                if (mode.name.equals(name)) {
                    return mode;
                }
            }
        }
        return TOP;
    }
}
