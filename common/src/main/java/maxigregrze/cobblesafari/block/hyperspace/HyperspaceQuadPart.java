package maxigregrze.cobblesafari.block.hyperspace;

import net.minecraft.util.StringRepresentable;

/** Vertical part of a 4-cell Hyperspace multiblock (lamp post). */
public enum HyperspaceQuadPart implements StringRepresentable {
    BOTTOM("bottom"),
    CENTER("center"),
    CENTERTOP("centertop"),
    TOP("top");

    private final String name;

    HyperspaceQuadPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
