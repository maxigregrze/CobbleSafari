package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's particle types. One per maple colour, since a {@link SimpleParticleType} carries no
 * payload and the three leaves differ only by texture - which is chosen by the particle definition
 * in {@code assets/cobblesafari/particles/}, not at spawn time.
 *
 * <p>The client-side providers are registered per loader; this class only declares the types, so it
 * is safe on a dedicated server.</p>
 */
public final class ModParticles {

    private ModParticles() {}

    public static SimpleParticleType RED_MAPLE_LEAVES;
    public static SimpleParticleType ORANGE_MAPLE_LEAVES;
    public static SimpleParticleType YELLOW_MAPLE_LEAVES;

    public static void register() {
        CobbleSafari.LOGGER.info("Registering particles for " + CobbleSafari.MOD_ID);
        RED_MAPLE_LEAVES = simple("red_maple_leaves");
        ORANGE_MAPLE_LEAVES = simple("orange_maple_leaves");
        YELLOW_MAPLE_LEAVES = simple("yellow_maple_leaves");
    }

    /**
     * {@code SimpleParticleType}'s constructor is protected, so an anonymous subclass is the
     * loader-neutral way to build one from common code - the alternative would be a Fabric-only
     * {@code FabricParticleTypes} call, which this module must not import.
     */
    private static SimpleParticleType simple(String name) {
        return Registry.register(
                BuiltInRegistries.PARTICLE_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, name),
                new SimpleParticleType(false) {});
    }
}
