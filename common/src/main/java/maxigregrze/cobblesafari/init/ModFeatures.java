package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.worldgen.PunchingBagFeature;
import maxigregrze.cobblesafari.worldgen.DistortionStumpFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ModFeatures {

    private ModFeatures() {}

    public static Feature<NoneFeatureConfiguration> PUNCHINGBAG;
    public static Feature<NoneFeatureConfiguration> DISTORTION_STUMP;

    public static void register() {
        CobbleSafari.LOGGER.info("Registering features for " + CobbleSafari.MOD_ID);
        PUNCHINGBAG = Registry.register(
                BuiltInRegistries.FEATURE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "punchingbag"),
                new PunchingBagFeature(NoneFeatureConfiguration.CODEC));
        DISTORTION_STUMP = Registry.register(
                BuiltInRegistries.FEATURE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "distortion_stump"),
                new DistortionStumpFeature(NoneFeatureConfiguration.CODEC));
    }
}
