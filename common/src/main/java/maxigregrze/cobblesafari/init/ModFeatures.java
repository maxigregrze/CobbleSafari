package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.worldgen.PunchingBagFeature;
import maxigregrze.cobblesafari.worldgen.DistortionStumpFeature;
import maxigregrze.cobblesafari.worldgen.HyperspaceDecorationFeature;
import maxigregrze.cobblesafari.worldgen.PlaceOnGroundDecorator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public class ModFeatures {

    private ModFeatures() {}

    public static Feature<NoneFeatureConfiguration> PUNCHINGBAG;
    public static Feature<NoneFeatureConfiguration> DISTORTION_STUMP;
    public static Feature<NoneFeatureConfiguration> HYPERSPACE_DECORATION;

    /**
     * Backport of the post-1.21.1 {@code minecraft:place_on_ground} tree decorator; see
     * {@link PlaceOnGroundDecorator}. The {@code TreeDecoratorType} constructor is private in
     * vanilla, so it is opened by the mod's access widener / access transformer - the same
     * treatment {@code StairBlock}'s constructor already receives.
     */
    public static TreeDecoratorType<PlaceOnGroundDecorator> PLACE_ON_GROUND;

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
        HYPERSPACE_DECORATION = Registry.register(
                BuiltInRegistries.FEATURE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "hyperspace_decoration"),
                new HyperspaceDecorationFeature(NoneFeatureConfiguration.CODEC));
        PLACE_ON_GROUND = Registry.register(
                BuiltInRegistries.TREE_DECORATOR_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "place_on_ground"),
                new TreeDecoratorType<>(PlaceOnGroundDecorator.CODEC));
    }
}
