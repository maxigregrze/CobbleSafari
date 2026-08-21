package maxigregrze.cobblesafari.block.misc;

import net.minecraft.util.StringRepresentable;

/**
 * Where a bridge block sits across the <em>parallel lines</em> beside it, which picks the texture
 * so that several lines read as one wide deck. Left and right are taken from an observer facing
 * along the chain.
 *
 * <p>Only the immediate neighbours are consulted: a block with a matching line on its right is the
 * leftmost of its group and carries {@link #LEFT}. That single rule covers two lines and
 * three-or-more alike, with no need to count how many lines there are.</p>
 */
public enum BridgeLine implements StringRepresentable {
    SOLO("solo"),
    LEFT("left"),
    MIDDLE("middle"),
    RIGHT("right");

    private final String name;

    BridgeLine(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static BridgeLine of(boolean peerLeft, boolean peerRight) {
        if (peerLeft && peerRight) {
            return MIDDLE;
        }
        if (peerRight) {
            return LEFT;
        }
        return peerLeft ? RIGHT : SOLO;
    }
}
