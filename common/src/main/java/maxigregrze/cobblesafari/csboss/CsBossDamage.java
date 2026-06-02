package maxigregrze.cobblesafari.csboss;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * Sources de dégâts custom du Boss Battle System (types de dégât datapack).
 * Voir {@code data/cobblesafari/damage_type/csboss_*.json}.
 */
public final class CsBossDamage {
    public static final ResourceKey<DamageType> BULLET = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "csboss_bullet"));
    public static final ResourceKey<DamageType> COWARDICE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "csboss_cowardice"));

    private CsBossDamage() {}

    public static DamageSource bullet(Level level) {
        return source(level, BULLET);
    }

    public static DamageSource cowardice(Level level) {
        return source(level, COWARDICE);
    }

    private static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key));
    }
}
