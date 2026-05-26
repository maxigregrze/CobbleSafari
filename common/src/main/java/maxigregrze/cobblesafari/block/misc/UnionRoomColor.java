package maxigregrze.cobblesafari.block.misc;

import net.minecraft.util.StringRepresentable;

public enum UnionRoomColor implements StringRepresentable {
    GREEN("green"),
    YELLOW("yellow"),
    BLUE("blue"),
    RED("red");

    public static final UnionRoomColor[] VALUES = values();

    private final String name;

    UnionRoomColor(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String textureSuffix() {
        return switch (this) {
            case GREEN -> "1";
            case YELLOW -> "2";
            case BLUE -> "3";
            case RED -> "4";
        };
    }
}
