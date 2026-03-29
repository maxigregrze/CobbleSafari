package maxigregrze.cobblesafari.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

public class DungeonDistortionDimensionEffects extends DimensionSpecialEffects {

    public DungeonDistortionDimensionEffects() {
        super(
                Float.NaN,
                true,
                SkyType.NORMAL,
                false,
                false
        );
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}
