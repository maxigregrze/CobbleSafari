package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {

    private ModCreativeTabs() {}

    public static final ResourceKey<CreativeModeTab> SAFARI_UNDERGROUND_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "safari_underground"));
    public static final ResourceKey<CreativeModeTab> SAFARI_DIMENSION_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "safari_dimension"));
    public static final ResourceKey<CreativeModeTab> MISC_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "misc"));
    public static final ResourceKey<CreativeModeTab> DISTORTION_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "distortion"));

    public static final CreativeModeTab SAFARI_UNDERGROUND_TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.cobblesafari.safari_underground"))
            .icon(() -> new ItemStack(ModBlocks.UNDERGROUND_STONE_TRANSITION))
            .displayItems((params, output) -> {
                output.accept(ModItems.HEART_SCALE);
                output.accept(ModItems.STAR_PIECE);
                output.accept(ModItems.SPHERE_BLUE_S);
                output.accept(ModItems.SPHERE_BLUE_L);
                output.accept(ModItems.SPHERE_RED_S);
                output.accept(ModItems.SPHERE_RED_L);
                output.accept(ModItems.SPHERE_GREEN_S);
                output.accept(ModItems.SPHERE_GREEN_L);
                output.accept(ModItems.SPHERE_PALE_S);
                output.accept(ModItems.SPHERE_PALE_L);
                output.accept(ModItems.SPHERE_PRISM_S);
                output.accept(ModItems.SPHERE_PRISM_L);
                output.accept(ModBlocks.UNDERGROUND_SMOOTH_STONE);
                output.accept(ModBlocks.UNDERGROUND_STONE);
                output.accept(ModBlocks.UNDERGROUND_STONE_TRANSITION);
                output.accept(ModBlocks.UNDERGROUND_HARDSTONE);
                output.accept(ModBlocks.UNDERGROUND_HARDSTONE_STAIRS);
                output.accept(ModBlocks.UNDERGROUND_TIMBER_VERTICAL_SMALL);
                output.accept(ModBlocks.UNDERGROUND_TIMBER_VERTICAL_LARGE);
                output.accept(ModBlocks.UNDERGROUND_TIMBER_VERTICAL_LARGE_LIGHT);
                output.accept(ModBlocks.UNDERGROUND_TIMBER_HORIZONTAL);
                output.accept(ModBlocks.UNDERGROUND_TIMBER_CORNER_HORIZONTAL);
                output.accept(ModBlocks.UNDERGROUND_TIMBER_CORNER_VERTICAL);
                output.accept(ModBlocks.UNDERGROUND_TIMBER_CORNER_VERTICAL_SMALL);
                output.accept(ModBlocks.UNDERGROUND_SECRET);
                output.accept(ModBlocks.UNDERGROUND_BOULDER);
                output.accept(ModBlocks.UNDERGROUND_DIGSITE);
                output.accept(ModBlocks.UNDERGROUND_PC);
                output.accept(ModBlocks.SECRETBASE_PC);
                output.accept(ModItems.FLAG_REGULAR);
                output.accept(ModItems.FLAG_BRONZE);
                output.accept(ModItems.FLAG_SILVER);
                output.accept(ModItems.FLAG_GOLD);
                output.accept(ModItems.FLAG_PLATINUM);
                output.accept(ModItems.FLAG_CREATIVE);
                output.accept(ModItems.FOSSIL_RANDOM);
                output.accept(ModItems.FOSSIL_PERFECT);
                output.accept(ModItems.FOSSIL_IV_MAX);
                output.accept(ModItems.FOSSIL_HIDDEN_ABILITY);
                output.accept(ModItems.FOSSIL_SHINY);
                output.accept(ModItems.LUCKY_MINING_HELMET);
                for (String type : ModBlocks.SPHERE_BLOCKS.keySet()) {
                    output.accept(ModBlocks.SPHERE_BLOCKS.get(type));
                    output.accept(ModBlocks.SPHERE_SLABS.get(type));
                    output.accept(ModBlocks.SPHERE_STAIRS.get(type));
                }
            })
            .build();

    public static final CreativeModeTab SAFARI_DIMENSION_TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
            .icon(() -> new ItemStack(ModBlocks.SAFARI_EGG_NEST))
            .title(Component.translatable("itemGroup.cobblesafari.safari_dimension"))
            .displayItems((params, output) -> {
                output.accept(ModBlocks.SAFARI_TELEPORTER);
                output.accept(ModItems.TICKET_SAFARI);
                output.accept(ModBlocks.SAFARI_EGG_NEST);
                output.accept(ModBlocks.MAGNETIC_CRYSTAL);
                output.accept(ModBlocks.MAGNETIC_CLUSTER);
                output.accept(ModBlocks.ICICLE);
                output.accept(ModBlocks.AIR_KELP);
                output.accept(ModBlocks.AIR_TUBE_CORAL_FAN);
                output.accept(ModBlocks.AIR_BRAIN_CORAL_FAN);
                output.accept(ModBlocks.AIR_BUBBLE_CORAL_FAN);
                output.accept(ModBlocks.AIR_FIRE_CORAL_FAN);
                output.accept(ModBlocks.AIR_HORN_CORAL_FAN);
                output.accept(ModBlocks.AIR_TUBE_CORAL);
                output.accept(ModBlocks.AIR_BRAIN_CORAL);
                output.accept(ModBlocks.AIR_BUBBLE_CORAL);
                output.accept(ModBlocks.AIR_FIRE_CORAL);
                output.accept(ModBlocks.AIR_HORN_CORAL);
                output.accept(ModItems.MUD_BALL);
                output.accept(ModItems.BAIT);
            })
            .build();

    public static final CreativeModeTab DISTORTION_TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 2)
            .icon(() -> new ItemStack(ModBlocks.DISTORTION_BOULDER))
            .title(Component.translatable("itemGroup.cobblesafari.distortion"))
            .displayItems((params, output) -> {
                output.accept(ModBlocks.DISTORTION_ROCK);
                output.accept(ModBlocks.DISTORTION_ROCK_VERTICAL);
                output.accept(ModBlocks.DISTORTION_ROCK_UPSIDEDOWN);
                output.accept(ModBlocks.DISTORTION_ROCK_DEEP);
                output.accept(ModBlocks.DISTORTION_ROCK_DEEP_VERTICAL);
                output.accept(ModBlocks.DISTORTION_ROCK_DEEP_UPSIDEDOWN);
                output.accept(ModBlocks.DISTORTION_ROCK_FLAT);
                output.accept(ModBlocks.DISTORTION_ROCK_FLAT_VERTICAL);
                output.accept(ModBlocks.DISTORTION_ROCK_FLAT_UPSIDEDOWN);
                output.accept(ModBlocks.DISTORTION_ROCK_FLOATING);
                output.accept(ModBlocks.DISTORTION_BOULDER);
                output.accept(ModBlocks.SUSPICIOUS_DISTORTION_ROCK);
            })
            .build();

    public static final CreativeModeTab MISC_TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 3)
            .icon(() -> new ItemStack(ModItems.REPEL))
            .title(Component.translatable("itemGroup.cobblesafari.misc"))
            .displayItems((params, output) -> {
                output.accept(ModBlocks.VOID_BLOCK);
                output.accept(ModBlocks.HOOPA_RING_PORTAL);
                output.accept(ModItems.TICKET_DUNGEON);
                output.accept(ModItems.SHINY_INCENSE);
                output.accept(ModItems.SUPER_SHINY_INCENSE);
                output.accept(ModItems.ULTRA_SHINY_INCENSE);
                output.accept(ModItems.REPEL);
                output.accept(ModItems.PERFUME_UNCOMMON);
                output.accept(ModItems.PERFUME_RARE);
                output.accept(ModItems.PERFUME_ULTRARARE);
                output.accept(ModBlocks.INCUBATOR);
                output.accept(ModItems.EGG_CREATIVE);
                output.accept(ModItems.WILD_EGG_BASE);
                ModItems.WILD_EGGS.values().forEach(output::accept);
            })
            .build();

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SAFARI_UNDERGROUND_KEY, SAFARI_UNDERGROUND_TAB);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SAFARI_DIMENSION_KEY, SAFARI_DIMENSION_TAB);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DISTORTION_KEY, DISTORTION_TAB);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MISC_KEY, MISC_TAB);
    }
}
