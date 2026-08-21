package maxigregrze.cobblesafari.block.misc;

import net.minecraft.util.StringRepresentable;

/**
 * Where a bridge block sits inside its chain, and how long that chain is. Derived, never chosen:
 * a lone block is {@link #SINGLE}, and appending to it re-reads every member of the run.
 *
 * <p>Two and three-or-more are distinct model sets on purpose - a two-block bridge sags between
 * its two raised ends without ever reaching the flat {@link #MIDDLE} trough.</p>
 */
public enum BridgeSegment implements StringRepresentable {
    SINGLE("single"),
    DOUBLE_START("double_start"),
    DOUBLE_END("double_end"),
    START("start"),
    MIDDLE("middle"),
    END("end");

    private final String name;

    BridgeSegment(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * The segment for a block at {@code index} in a chain of {@code length}, where index 0 is the
     * block touching the anchor. Out-of-range input degrades to {@link #MIDDLE} rather than
     * throwing: a chain longer than the scan cap is clamped, not rejected.
     */
    public static BridgeSegment of(int index, int length) {
        if (length <= 1) {
            return SINGLE;
        }
        if (length == 2) {
            return index <= 0 ? DOUBLE_START : DOUBLE_END;
        }
        if (index <= 0) {
            return START;
        }
        return index >= length - 1 ? END : MIDDLE;
    }
}
