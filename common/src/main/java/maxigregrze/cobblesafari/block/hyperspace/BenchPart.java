package maxigregrze.cobblesafari.block.hyperspace;

import net.minecraft.util.StringRepresentable;

/**
 * Which bench model a cell draws, named after the side its neighbour is on in the model's own
 * frame (as drawn, which is FACING=south): {@code east} has a neighbour to the east, so its legs
 * stand on the west end of the row.
 */
public enum BenchPart implements StringRepresentable {
    SOLO("solo"),
    EAST("east"),
    WEST("west"),
    MIDDLE("middle");

    private final String name;

    BenchPart(String name) {
        this.name = name;
    }

    public static BenchPart of(boolean east, boolean west) {
        if (east && west) {
            return MIDDLE;
        }
        if (east) {
            return EAST;
        }
        return west ? WEST : SOLO;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
