package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.basepc.BasePCBlock;
import maxigregrze.cobblesafari.block.basepc.BasePCBlockItem;
import maxigregrze.cobblesafari.block.BlockPart;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.HoopaRingPortalBlock;
import maxigregrze.cobblesafari.block.misc.AirKelpBlock;
import maxigregrze.cobblesafari.block.misc.AppearingDistortionFlowerBlock;
import maxigregrze.cobblesafari.block.misc.AquaticDecorationBlock;
import maxigregrze.cobblesafari.block.misc.BelltowerTrellisBlock;
import maxigregrze.cobblesafari.block.misc.DistortionFlowerBlock;
import maxigregrze.cobblesafari.block.misc.IcicleBlock;
import maxigregrze.cobblesafari.block.misc.DistortionBoulderBlock;
import maxigregrze.cobblesafari.block.misc.DistortionRockBlock;
import maxigregrze.cobblesafari.block.misc.DistortionRockDirectionalBlock;
import maxigregrze.cobblesafari.block.misc.DistortionRockVerticalBlock;
import maxigregrze.cobblesafari.block.misc.MagneticClusterBlock;
import maxigregrze.cobblesafari.block.misc.MagneticCrystalBlock;
import maxigregrze.cobblesafari.block.misc.SafariEggNestBlock;
import maxigregrze.cobblesafari.block.misc.VanishingDistortionFlowerBlock;
import maxigregrze.cobblesafari.block.misc.VoidBlock;
import maxigregrze.cobblesafari.block.teleporter.SafariTeleporterBlock;
import maxigregrze.cobblesafari.block.underground.UndergroundBoulderBlock;
import maxigregrze.cobblesafari.block.underground.UndergroundDigsiteBlock;
import maxigregrze.cobblesafari.block.underground.UndergroundPCBlock;
import maxigregrze.cobblesafari.block.underground.UndergroundSecretBlock;
import maxigregrze.cobblesafari.block.incubator.IncubatorBlock;
import maxigregrze.cobblesafari.block.incubator.IncubatorBlockItem;
import maxigregrze.cobblesafari.block.underground.UndergroundTimberBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModBlocks {

    private ModBlocks() {}

    public static final Block SAFARI_TELEPORTER = registerBlock("safari_teleporter",
            new SafariTeleporterBlock(BlockBehaviour.Properties.of()
                    .strength(50.0f, 1200.0f)
                    .lightLevel(state -> 5)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            ));

    public static final Block SAFARI_EGG_NEST = registerBlock("safari_egg_nest",
            new SafariEggNestBlock(BlockBehaviour.Properties.of()
                    .strength(0.5f)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .noLootTable()
            ));

    public static final Block HOOPA_RING_PORTAL = registerBlock("hoopa_ring_portal",
            new HoopaRingPortalBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .lightLevel(state -> 10)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block DUNGEON_PORTAL = registerBlock("dungeon_portal",
            new DungeonPortalBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .lightLevel(state -> 10)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block MAGNETIC_CRYSTAL = registerBlock("magnetic_crystal",
            new MagneticCrystalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 10)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            ));

    public static final Block MAGNETIC_CLUSTER = registerBlock("magnetic_cluster",
            new MagneticClusterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 10)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            ));

    public static final Block VOID_BLOCK = registerBlock("void_block",
            new VoidBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0f, 3600000.0f)
                    .noLootTable()
                    .noOcclusion()
            ));


    public static final Block UNDERGROUND_SMOOTH_STONE = registerBlock("underground_smooth_stone",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PINK)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
            ));

    public static final Block UNDERGROUND_STONE = registerBlock("underground_stone",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
            ));

    public static final Block UNDERGROUND_STONE_TRANSITION = registerBlock("underground_stone_transition",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
            ));

    public static final Block UNDERGROUND_HARDSTONE = registerBlock("underground_hardstone",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
            ));

    public static final Block UNDERGROUND_HARDSTONE_STAIRS = registerBlock("underground_hardstone_stairs",
            new StairBlock(UNDERGROUND_HARDSTONE.defaultBlockState(), BlockBehaviour.Properties.of()
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
            ));

    public static final Block UNDERGROUND_TIMBER_VERTICAL_SMALL = registerBlock("underground_timber_vertical_small",
            new UndergroundTimberBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noCollission(),
                    true, UndergroundTimberBlock::verticalSmall));

    public static final Block UNDERGROUND_TIMBER_VERTICAL_LARGE = registerBlock("underground_timber_vertical_large",
            new UndergroundTimberBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noCollission(),
                    true, UndergroundTimberBlock::verticalLarge));

    public static final Block UNDERGROUND_TIMBER_VERTICAL_LARGE_LIGHT = registerBlock("underground_timber_vertical_large_light",
            new UndergroundTimberBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> 15)
                    .noOcclusion()
                    .noCollission(),
                    true, UndergroundTimberBlock::verticalLarge));

    public static final Block UNDERGROUND_TIMBER_HORIZONTAL = registerBlock("underground_timber_horizontal",
            new UndergroundTimberBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noCollission(),
                    true, UndergroundTimberBlock::horizontal));

    public static final Block UNDERGROUND_TIMBER_CORNER_HORIZONTAL = registerBlock("underground_timber_corner_horizontal",
            new UndergroundTimberBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noCollission(),
                    false, UndergroundTimberBlock::cornerHorizontal));

    public static final Block UNDERGROUND_TIMBER_CORNER_VERTICAL = registerBlock("underground_timber_corner_vertical",
            new UndergroundTimberBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noCollission(),
                    false, UndergroundTimberBlock::cornerVertical));

    public static final Block UNDERGROUND_TIMBER_CORNER_VERTICAL_SMALL = registerBlock("underground_timber_corner_vertical_small",
            new UndergroundTimberBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noCollission(),
                    false, UndergroundTimberBlock::cornerVerticalSmall));

    public static final Block UNDERGROUND_DIGSITE = registerBlock("underground_digsite",
            new UndergroundDigsiteBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 3)
            ));

    public static final Block UNDERGROUND_SECRET = registerBlock("underground_secret",
            new UndergroundSecretBlock(BlockBehaviour.Properties.of()
                    .strength(0.5f)
                    .noOcclusion()
                    .noCollission()
            ));

    private static final VoxelShape CORAL_FAN_SHAPE = Block.box(2, 0, 2, 14, 4, 14);
    private static final VoxelShape CORAL_PLANT_SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public static final Block AIR_KELP = registerBlock("air_kelp",
            new AirKelpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .instabreak()
                    .sound(SoundType.WET_GRASS)
                    .noOcclusion()
                    .noCollission()
                    .randomTicks()));

    public static final Block AIR_TUBE_CORAL_FAN = registerBlock("air_tube_coral_fan",
            new AquaticDecorationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .instabreak()
                    .sound(SoundType.CORAL_BLOCK)
                    .noOcclusion()
                    .noCollission(),
                    CORAL_FAN_SHAPE));

    public static final Block AIR_BRAIN_CORAL_FAN = registerBlock("air_brain_coral_fan",
            new AquaticDecorationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .instabreak()
                    .sound(SoundType.CORAL_BLOCK)
                    .noOcclusion()
                    .noCollission(),
                    CORAL_FAN_SHAPE));

    public static final Block AIR_BUBBLE_CORAL_FAN = registerBlock("air_bubble_coral_fan",
            new AquaticDecorationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instabreak()
                    .sound(SoundType.CORAL_BLOCK)
                    .noOcclusion()
                    .noCollission(),
                    CORAL_FAN_SHAPE));

    public static final Block AIR_FIRE_CORAL_FAN = registerBlock("air_fire_coral_fan",
            new AquaticDecorationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .instabreak()
                    .sound(SoundType.CORAL_BLOCK)
                    .noOcclusion()
                    .noCollission(),
                    CORAL_FAN_SHAPE));

    public static final Block AIR_HORN_CORAL_FAN = registerBlock("air_horn_coral_fan",
            new AquaticDecorationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .instabreak()
                    .sound(SoundType.CORAL_BLOCK)
                    .noOcclusion()
                    .noCollission(),
                    CORAL_FAN_SHAPE));

    public static final Block AIR_TUBE_CORAL = registerBlock("air_tube_coral",
            new AquaticDecorationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .instabreak()
                    .sound(SoundType.CORAL_BLOCK)
                    .noOcclusion()
                    .noCollission(),
                    CORAL_PLANT_SHAPE));

    public static final Block AIR_BRAIN_CORAL = registerBlock("air_brain_coral",
            new AquaticDecorationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .instabreak()
                    .sound(SoundType.CORAL_BLOCK)
                    .noOcclusion()
                    .noCollission(),
                    CORAL_PLANT_SHAPE));

    public static final Block AIR_BUBBLE_CORAL = registerBlock("air_bubble_coral",
            new AquaticDecorationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instabreak()
                    .sound(SoundType.CORAL_BLOCK)
                    .noOcclusion()
                    .noCollission(),
                    CORAL_PLANT_SHAPE));

    public static final Block AIR_FIRE_CORAL = registerBlock("air_fire_coral",
            new AquaticDecorationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .instabreak()
                    .sound(SoundType.CORAL_BLOCK)
                    .noOcclusion()
                    .noCollission(),
                    CORAL_PLANT_SHAPE));

    public static final Block AIR_HORN_CORAL = registerBlock("air_horn_coral",
            new AquaticDecorationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .instabreak()
                    .sound(SoundType.CORAL_BLOCK)
                    .noOcclusion()
                    .noCollission(),
                    CORAL_PLANT_SHAPE));

    public static final Block UNDERGROUND_BOULDER = registerBlock("underground_boulder",
            new UndergroundBoulderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            ));

    public static final Block UNDERGROUND_PC = registerBlock("underground_pc",
            new UndergroundPCBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    public static final Block ICICLE = registerBlock("icicle",
            new IcicleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .noOcclusion()
                    .sound(SoundType.GLASS)
                    .strength(1.5F, 1.5F)
                    .dynamicShape()
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)
                    .isRedstoneConductor((state, level, pos) -> false)
            ));

    public static final Block SECRETBASE_PC = registerBlockWithoutItem("secretbase_pc",
            new BasePCBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(BasePCBlock.PART) == BlockPart.TOP ? 15 : 0)
            ));

    public static final Block DISTORTION_ROCK = registerBlock("distortion_rock",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block SUSPICIOUS_DISTORTION_ROCK = registerBlock("suspicious_distortion_rock",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block DISTORTION_ROCK_VERTICAL = registerBlock("distortion_rock_vertical",
            new DistortionRockVerticalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block DISTORTION_ROCK_UPSIDEDOWN = registerBlock("distortion_rock_upsidedown",
            new DistortionRockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block DISTORTION_ROCK_DEEP = registerBlock("distortion_rock_deep",
            new DistortionRockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block DISTORTION_ROCK_DEEP_VERTICAL = registerBlock("distortion_rock_deep_vertical",
            new DistortionRockDirectionalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block DISTORTION_ROCK_DEEP_UPSIDEDOWN = registerBlock("distortion_rock_deep_upsidedown",
            new DistortionRockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block DISTORTION_ROCK_FLAT = registerBlock("distortion_rock_flat",
            new DistortionRockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            ));

    public static final Block DISTORTION_ROCK_FLAT_VERTICAL = registerBlock("distortion_rock_flat_vertical",
            new DistortionRockDirectionalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            ));

    public static final Block DISTORTION_ROCK_FLAT_UPSIDEDOWN = registerBlock("distortion_rock_flat_upsidedown",
            new DistortionRockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            ));

    public static final Block DISTORTION_BOULDER = registerBlock("distortion_boulder",
            new DistortionBoulderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block DISTORTION_FLOWER = registerBlock("distortion_flower",
            new DistortionFlowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noLootTable()
            ));

    public static final Block VANISHING_DISTORTION_FLOWER = registerBlock("vanishing_distortion_flower",
            new VanishingDistortionFlowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noLootTable()
            ));

    public static final Block APPEARING_DISTORTION_FLOWER = registerBlock("appearing_distortion_flower",
            new AppearingDistortionFlowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noLootTable()
            ));

    public static final Block DISTORTION_ROCK_FLOATING = registerBlock("distortion_rock_floating",
            new DistortionRockDirectionalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block BELLTOWER_TRELLIS = registerBlock("belltower_trellis",
            new BelltowerTrellisBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
            ));

    public static final Block INCUBATOR = registerBlockWithoutItem("incubator",
            new IncubatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    public static final Map<String, Block> SPHERE_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Block> SPHERE_SLABS = new LinkedHashMap<>();
    public static final Map<String, Block> SPHERE_STAIRS = new LinkedHashMap<>();

    private static final String[] SPHERE_TYPES_ARRAY = {
        "blue_s", "blue_l", "red_s", "red_l", "green_s", "green_l",
        "pale_s", "pale_l", "prism_s", "prism_l"
    };

    static {
        for (String type : SPHERE_TYPES_ARRAY) {
            MapColor color = getSphereMapColor(type);
            Block base = registerBlock("sphere_" + type + "_block",
                    new Block(BlockBehaviour.Properties.of()
                            .mapColor(color)
                            .strength(1.5f, 6.0f)
                            .sound(SoundType.STONE)
                    ));
            SPHERE_BLOCKS.put(type, base);
        }
        for (String type : SPHERE_TYPES_ARRAY) {
            Block base = SPHERE_BLOCKS.get(type);
            SPHERE_SLABS.put(type, registerBlock("sphere_" + type + "_slab",
                    new SlabBlock(BlockBehaviour.Properties.ofFullCopy(base))));
        }
        for (String type : SPHERE_TYPES_ARRAY) {
            Block base = SPHERE_BLOCKS.get(type);
            SPHERE_STAIRS.put(type, registerBlock("sphere_" + type + "_stairs",
                    new StairBlock(base.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(base))));
        }
    }

    private static MapColor getSphereMapColor(String type) {
        return switch (type) {
            case "blue_s", "blue_l"   -> MapColor.COLOR_BLUE;
            case "red_s", "red_l"     -> MapColor.COLOR_RED;
            case "green_s", "green_l" -> MapColor.COLOR_GREEN;
            case "pale_s", "pale_l"   -> MapColor.QUARTZ;
            case "prism_s", "prism_l" -> MapColor.DIAMOND;
            default -> MapColor.STONE;
        };
    }

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, name), block);
    }

    private static Block registerBlockWithoutItem(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, name),
                new BlockItem(block, new Item.Properties()));
    }

    public static void register() {
        CobbleSafari.LOGGER.info("Registering blocks for " + CobbleSafari.MOD_ID);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "secretbase_pc"),
                new BasePCBlockItem(SECRETBASE_PC, new Item.Properties().rarity(Rarity.RARE)));

        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "incubator"),
                new IncubatorBlockItem(INCUBATOR, new Item.Properties()));
    }
}
