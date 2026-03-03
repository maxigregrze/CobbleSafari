package maxigregrze.cobblesafari.block;

import net.minecraft.util.StringRepresentable;

public enum BlockPart implements StringRepresentable {
    CENTER("center"),
    SIDE("side"),
    TOP("top");

    private final String name;

    BlockPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
