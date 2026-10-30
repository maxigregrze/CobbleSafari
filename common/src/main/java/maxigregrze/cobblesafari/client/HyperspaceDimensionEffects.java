package maxigregrze.cobblesafari.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Hyperspace Lumiose ({@code cobblesafari:dungeon_paris}, action plan 197, section 3.1): the End's
 * lighting with no vanilla sky at all. {@code HyperspaceSkyRenderer} draws the sky; vanilla is left
 * with the near-black fog colour behind it.
 */
public class HyperspaceDimensionEffects extends DimensionSpecialEffects {

    public HyperspaceDimensionEffects() {
        // NONE: vanilla draws no sky. forceBrightLightmap = true: the End's uniform light, indoors included.
        super(Float.NaN, false, SkyType.NONE, true, false);
    }

    /** Same curve as {@code DimensionSpecialEffects.EndEffects}. */
    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.scale(0.15F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}
