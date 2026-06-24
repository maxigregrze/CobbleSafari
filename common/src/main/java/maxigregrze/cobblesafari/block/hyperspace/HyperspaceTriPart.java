package maxigregrze.cobblesafari.block.hyperspace;

import net.minecraft.util.StringRepresentable;

/** Vertical part of a 3-cell Hyperspace multiblock (large neon, lamp post). */
public enum HyperspaceTriPart implements StringRepresentable {
    BOTTOM("bottom"),
    CENTER("center"),
    TOP("top");

    private final String name;

    HyperspaceTriPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
