package maxigregrze.cobblesafari.block;

import net.minecraft.util.StringRepresentable;

public enum DistortionFlowerPart implements StringRepresentable {
    BASE("base"),
    STEM("stem"),
    FLOWER("flower");

    private final String name;

    DistortionFlowerPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}

