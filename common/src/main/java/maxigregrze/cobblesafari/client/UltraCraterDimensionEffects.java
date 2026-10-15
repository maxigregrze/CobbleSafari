package maxigregrze.cobblesafari.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Ultra Crater sky (action plan 190, W5): the overworld's in every respect - clouds at Y 192, a
 * normal sky coloured by the biome, the same brightness-dependent fog tint - except that
 * {@link #isFoggyAt} answers yes everywhere, which is what switches vanilla to its thick, Nether
 * style fog. A biome can only colour fog; how dense it is belongs to these effects.
 */
public class UltraCraterDimensionEffects extends DimensionSpecialEffects {

    /** The overworld's cloud deck; the crater's ground is placed so the clouds sit 16 blocks above the spawn. */
    private static final float CLOUD_LEVEL = 192.0F;

    public UltraCraterDimensionEffects() {
        super(CLOUD_LEVEL, true, SkyType.NORMAL, false, false);
    }

    /** Same curve as {@code DimensionSpecialEffects.OverworldEffects}. */
    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }
}
