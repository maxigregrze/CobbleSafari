package maxigregrze.cobblesafari.block.csboss;

import net.minecraft.util.StringRepresentable;

public enum AnchorVariant implements StringRepresentable {
    DEFAULT("default", ""),
    DISTORTION("distortion", "distortion");

    private final String name;
    private final String textureSuffix;

    AnchorVariant(String name, String textureSuffix) {
        this.name = name;
        this.textureSuffix = textureSuffix;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String modelName(boolean active) {
        String base = "bossanchor_base";
        if (!textureSuffix.isEmpty()) {
            base += "_" + textureSuffix;
        }
        if (active) {
            base += "_active";
        }
        return base;
    }

    public AnchorVariant next() {
        AnchorVariant[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public AnchorVariant prev() {
        AnchorVariant[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }

    public static AnchorVariant byName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT;
        }
        for (AnchorVariant variant : values()) {
            if (variant.name.equals(name)) {
                return variant;
            }
        }
        return DEFAULT;
    }
}
