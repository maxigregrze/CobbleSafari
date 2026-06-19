package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.basepc.BasePCBlock;
import maxigregrze.cobblesafari.block.basepc.BasePCBlockItem;
import maxigregrze.cobblesafari.block.BlockPart;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.CreativeDungeonPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.HoopaRingPortalBlock;
import maxigregrze.cobblesafari.block.misc.AirKelpBlock;
import maxigregrze.cobblesafari.block.distortion.AppearingDistortionFlowerBlock;
import maxigregrze.cobblesafari.block.misc.AquaticDecorationBlock;
import maxigregrze.cobblesafari.block.misc.BelltowerTrellisBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionCarpetFlowerBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionFlowerBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionDoorBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionPortalBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionWeedBlock;
import maxigregrze.cobblesafari.block.misc.IcicleBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionBoulderBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionRockBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionRockDirectionalBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionRockVerticalBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionStoneBricksRuneBlock;
import maxigregrze.cobblesafari.block.distortion.DistortionStonebricksRubbleBlock;
import maxigregrze.cobblesafari.block.distortion.GiratinaCoreBlock;
import maxigregrze.cobblesafari.block.distortion.GiratinaCoreSideBlock;
import maxigregrze.cobblesafari.block.misc.LostNotesBlock;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballBlock;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballSmallBlock;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballGoldBlock;
import maxigregrze.cobblesafari.block.misc.LostItemBlock;
import maxigregrze.cobblesafari.block.misc.LostItemVisualBlock;
import maxigregrze.cobblesafari.block.misc.MagneticClusterBlock;
import maxigregrze.cobblesafari.block.misc.MagneticCrystalBlock;
import maxigregrze.cobblesafari.block.misc.LiquidBarrierBlock;
import maxigregrze.cobblesafari.block.misc.KarateMannequinBlock;
import maxigregrze.cobblesafari.block.misc.DraconicCraterBlock;
import maxigregrze.cobblesafari.block.misc.HotGeyserBlock;
import maxigregrze.cobblesafari.block.misc.MudPileBlock;
import maxigregrze.cobblesafari.block.misc.VolcanicCraterBlock;
import maxigregrze.cobblesafari.block.misc.SludgePileBlock;
import maxigregrze.cobblesafari.block.misc.PunchingBagBlock;
import maxigregrze.cobblesafari.block.misc.TombstoneBlock;
import maxigregrze.cobblesafari.block.misc.SafariEggNestBlock;
import maxigregrze.cobblesafari.block.misc.VoidBlock;
import maxigregrze.cobblesafari.block.distortion.VanishingDistortionFlowerBlock;
import maxigregrze.cobblesafari.block.misc.WhirlwindBlock;
import maxigregrze.cobblesafari.block.misc.UnionRoomDecorBlock;
import maxigregrze.cobblesafari.block.misc.OnlineFeaturePcBlock;
import maxigregrze.cobblesafari.block.misc.UnionRoomGlobeBlock;
import maxigregrze.cobblesafari.block.misc.UnionRoomExitTeleporterBlock;
import maxigregrze.cobblesafari.block.misc.PokemonStatueBlock;
import maxigregrze.cobblesafari.block.misc.UnionRoomPillarBlock;
import maxigregrze.cobblesafari.block.misc.UnionRoomSpotlightBlock;
import maxigregrze.cobblesafari.block.misc.UnionRoomSpotlightDisplayLightBlock;
import maxigregrze.cobblesafari.block.teleporter.SafariTeleporterBlock;
import maxigregrze.cobblesafari.block.teleporter.SurvivalTeleportPadBlock;
import maxigregrze.cobblesafari.block.teleporter.TeleportPadBlock;
import maxigregrze.cobblesafari.block.underground.UndergroundBoulderBlock;
import maxigregrze.cobblesafari.block.underground.UndergroundDigsiteBlock;
import maxigregrze.cobblesafari.block.underground.UndergroundPCBlock;
import maxigregrze.cobblesafari.block.underground.UndergroundSecretBlock;
import maxigregrze.cobblesafari.block.incubator.IncubatorBlock;
import maxigregrze.cobblesafari.block.incubator.IncubatorBlockItem;
import maxigregrze.cobblesafari.block.csboss.CsBossElectricityBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossTriggerBlock;
import maxigregrze.cobblesafari.block.csboss.EphemeralPileBlock;
import maxigregrze.cobblesafari.block.csboss.MeteoriteBlock;
import maxigregrze.cobblesafari.block.balm.BalmDispenserBlock;
import maxigregrze.cobblesafari.block.balm.BalmDispenserDistortionBlock;
import maxigregrze.cobblesafari.block.rotomphone.EmptyPhoneBlock;
import maxigregrze.cobblesafari.block.trap.DarknessTrapBlock;
import maxigregrze.cobblesafari.block.trap.ExplosionTrapBlock;
import maxigregrze.cobblesafari.block.trap.FartTrapBlock;
import maxigregrze.cobblesafari.block.trap.FireTrapBlock;
import maxigregrze.cobblesafari.block.trap.GravityTrapBlock;
import maxigregrze.cobblesafari.block.trap.MoveTrapBlock;
import maxigregrze.cobblesafari.block.trap.RockTrapBlock;
import maxigregrze.cobblesafari.block.trap.SlowTrapBlock;
import maxigregrze.cobblesafari.block.trap.TeleportTrapBlock;
import maxigregrze.cobblesafari.block.trap.WindTrapBlock;
import maxigregrze.cobblesafari.block.underground.UndergroundTimberBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    /** Ephemeral meteorite blocks placed by rock/dragon boss attacks. No creative item. */
    public static final Block METEORITE = registerBlockWithoutItem("meteorite",
            new MeteoriteBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .strength(1.5F), 200));

    public static final Block DRACO_METEORITE = registerBlockWithoutItem("draco_meteorite",
            new MeteoriteBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .sound(SoundType.STONE)
                    .strength(1.5F), 400));

    /** Display block (model only) for the dirt pile of {@code base_ground_1}. */
    public static final Block ATTACK_DIGDIRT_DISPLAY = registerBlockWithoutItem("attack_digdirt_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.0F)));

    /** Display block (model only) for the scaled dirt-textured mound of {@code base_ground_2}. */
    public static final Block ATTACK_DIGDIRT_DIRT_DISPLAY = registerBlockWithoutItem("attack_digdirt_dirt_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.0F)));

    /** In-flight sludge cube ({@code base_poison_2}). Model only, no item. */
    public static final Block ATTACK_SLUDGE_CUBE_DISPLAY = registerBlockWithoutItem("attack_sludge_cube_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.0F)));

    /** In-flight mud cube ({@code base_ground_3}). Model only, no item. */
    public static final Block ATTACK_MUD_CUBE_DISPLAY = registerBlockWithoutItem("attack_mud_cube_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.0F)));

    /** Ephemeral sludge pile placed by {@code base_poison_2} (10 s). */
    public static final Block EPHEMERAL_SLUDGE_PILE = registerBlockWithoutItem("ephemeral_sludge_pile",
            new EphemeralPileBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .sound(SoundType.SLIME_BLOCK)
                    .strength(0.5F)
                    .noOcclusion()
                    .noCollission(), 200, true));

    /** Ephemeral mud pile placed by {@code base_ground_3} (20 s). */
    public static final Block EPHEMERAL_MUD_PILE = registerBlockWithoutItem("ephemeral_mud_pile",
            new EphemeralPileBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .sound(SoundType.WET_GRASS)
                    .strength(0.5F)
                    .noOcclusion()
                    .noCollission(), 400, false));

    /** Trap electric field placed by {@code base_electric_2}. No creative item. */
    public static final Block CSBOSS_ELECTRICITY = registerBlockWithoutItem("csboss_electricity",
            new CsBossElectricityBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.0F)
                    .lightLevel(s -> 6)));

    public static final Block SAFARI_TELEPORTER = registerBlock("safari_teleporter",
            new SafariTeleporterBlock(BlockBehaviour.Properties.of()
                    .strength(50.0f, 1200.0f)
                    .lightLevel(state -> 5)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            ));

    public static final Block TELEPORT_PAD = registerBlock("teleport_pad",
            new TeleportPadBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .lightLevel(state -> 9)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
            ));

    public static final Block SURVIVAL_TELEPORT_PAD = registerBlock("survival_teleporter_pad",
            new SurvivalTeleportPadBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .lightLevel(state -> 9)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
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

    public static final Block CREATIVE_DUNGEON_PORTAL = registerBlock("creative_dungeon_portal",
            new CreativeDungeonPortalBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .lightLevel(state -> 10)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block DUNGEON_PORTAL_EFFECT = registerBlockWithoutItem("dungeon_portal_effect",
            new Block(BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .lightLevel(state -> 10)
                    .noOcclusion()
                    .noCollission()
                    .dynamicShape()
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

    public static final Block LIQUID_BARRIER = registerBlock("liquid_barrier",
            new LiquidBarrierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(-1.0f, 3600000.0f)
                    .noLootTable()
                    .noOcclusion()
            ));

    public static final Block GIRATINA_CORE = registerBlock("giratina_core",
            new GiratinaCoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> 8)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block GIRATINA_CORE_N = registerBlock("giratina_core_n",
            new GiratinaCoreSideBlock(0, -1, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> 8)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block GIRATINA_CORE_NE = registerBlock("giratina_core_ne",
            new GiratinaCoreSideBlock(1, -1, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            ));

    public static final Block GIRATINA_CORE_E = registerBlock("giratina_core_e",
            new GiratinaCoreSideBlock(1, 0, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> 8)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block GIRATINA_CORE_SE = registerBlock("giratina_core_se",
            new GiratinaCoreSideBlock(1, 1, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            ));

    public static final Block GIRATINA_CORE_S = registerBlock("giratina_core_s",
            new GiratinaCoreSideBlock(0, 1, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> 8)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block GIRATINA_CORE_SW = registerBlock("giratina_core_sw",
            new GiratinaCoreSideBlock(-1, 1, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> 8)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block GIRATINA_CORE_W = registerBlock("giratina_core_w",
            new GiratinaCoreSideBlock(-1, 0, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> 8)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block GIRATINA_CORE_NW = registerBlock("giratina_core_nw",
            new GiratinaCoreSideBlock(-1, -1, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> 8)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block GIRATINA_CORE_MOVING = registerBlockWithoutItem("giratina_core_moving",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            ));

    /** "Moving" model rendered alone for the CSBoss summon projectile. */
    public static final Block BOSSANCHOR_MOVING = registerBlockWithoutItem("bossanchor_moving",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
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

    public static final Block UNDERGROUND_POLISHED_HARDSTONE = registerBlock("underground_polished_hardstone",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
            ));

    public static final Block UNDERGROUND_STONEBRICKS = registerBlock("underground_stonebricks",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
            ));

    public static final Block UNDERGROUND_CRACKED_STONEBRICKS = registerBlock("underground_cracked_stonebricks",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
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

    public static final Block RUSTED_IRON_BLOCK = registerBlock("rusted_iron_block",
            new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final Block COMPACTED_TRASH_BLOCK = registerBlock("compacted_trash_block",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.RAW_IRON)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final Block RUST_CLUMP = registerBlock("rust_clump",
            new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK)));

    public static final Block WHIRLWIND = registerBlock("whirlwind",
            new WhirlwindBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .instabreak()
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .noCollission()
                    .pushReaction(PushReaction.DESTROY)));

    public static final Block WHIRLWIND_DISPLAY = registerBlockWithoutItem("whirlwind_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .instabreak()
                    .noOcclusion()
                    .noLootTable()
                    .noCollission()));

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

    public static final Block DISTORTION_STONE = registerBlock("distortion_stone",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block DISTORTION_STONE_BRICKS = registerBlock("distortion_stone_bricks",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block DISTORTION_STONE_BRICKS_RUNE = registerBlock("distortion_stone_bricks_rune",
            new DistortionStoneBricksRuneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
            ));

    public static final Block DISTORTION_STONEBRICKS_RUBBLE = registerBlock("distortion_stonebricks_rubble",
            new DistortionStonebricksRubbleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block DISTORTION_STONE_BRICKS_STAIRS = registerBlock("distortion_stone_bricks_stairs",
            new StairBlock(DISTORTION_STONE_BRICKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(DISTORTION_STONE_BRICKS)));

    public static final Block DISTORTION_STONE_BRICKS_SLAB = registerBlock("distortion_stone_bricks_slab",
            new SlabBlock(BlockBehaviour.Properties.ofFullCopy(DISTORTION_STONE_BRICKS)));

    public static final Block DISTORTION_STONE_BRICKS_CRACKED = registerBlock("distortion_stone_bricks_cracked",
            new Block(BlockBehaviour.Properties.ofFullCopy(DISTORTION_STONE_BRICKS)));

    public static final Block DISTORTION_STONEBRICKS_DOOR = registerBlock("distortion_stonebricks_door",
            new DistortionDoorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            ));

    /** Temporary full-block fill for the door's empty cells in combat. No item, no drop. */
    public static final Block DISTORTION_STONEBRICKS_FILL = registerBlockWithoutItem("distortion_stonebricks_fill",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .noLootTable()
            ));

    public static final Block LOST_NOTES = registerBlock("lost_notes",
            new LostNotesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block LOST_ITEM = registerBlock("lost_item",
            new LostItemBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block LOST_ITEM_VISUAL = registerBlockWithoutItem("lost_item_visual",
            new LostItemVisualBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block CSBOSS_TRIGGER = registerBlock("csboss_trigger",
            new CsBossTriggerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(CsBossTriggerBlock.ACTIVE) ? 10 : 3)
            ));

    /** CSBoss-reactive mimic block: invisible/solid toggle, copies a target texture. */
    public static final Block CSBOSS_MIMIC_BLOCK = registerBlock("csboss_mimic_block",
            new CsBossMimicBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            ));

    public static final Block BALM_DISPENSER = registerBlock("balm_dispenser",
            new BalmDispenserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            ));

    public static final Block BALM_DISPENSER_DISTORTION = registerBlock("balm_dispenser_distortion",
            new BalmDispenserDistortionBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            ));

    public static final Block AUSPICIOUS_POKEBALL = registerBlock("auspicious_pokeball",
            new AuspiciousPokeballBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(state -> 15)
            ));

    public static final Block AUSPICIOUS_POKEBALL_DISPLAY = registerBlockWithoutItem("auspicious_pokeball_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block AUSPICIOUS_POKEBALL_SMALL = registerBlock("auspiciouspokeball_small",
            new AuspiciousPokeballSmallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(state -> 15)
            ));

    public static final Block AUSPICIOUS_POKEBALL_SMALL_DISPLAY = registerBlockWithoutItem("auspiciouspokeball_small_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block AUSPICIOUS_POKEBALL_GOLD = registerBlock("auspiciouspokeball_gold",
            new AuspiciousPokeballGoldBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(state -> 15)
            ));

    public static final Block AUSPICIOUS_POKEBALL_GOLD_DISPLAY = registerBlockWithoutItem("auspiciouspokeball_gold_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instabreak()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block DISTORTION_PORTAL = registerBlock("distortion_portal",
            new DistortionPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(state -> 15)
            ));

    public static final Block DISTORTION_PORTAL_MOVING = registerBlockWithoutItem("distortion_portal_moving",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .noCollission()
                    .dynamicShape()
                    .lightLevel(state -> 15)
            ));

    public static final Block DISTORTION_PORTAL_MOVING_BACK = registerBlockWithoutItem("distortion_portal_moving_back",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0f, 9.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .noCollission()
                    .dynamicShape()
                    .lightLevel(state -> 15)
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

    public static final Block DISTORTION_FLOWER_CARPET = registerBlock("distortion_flower_carpet",
            new DistortionCarpetFlowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
            ));

    public static final Block DISTORTION_WEED = registerBlock("distortion_weed",
            new DistortionWeedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .instabreak()
                    .sound(SoundType.WET_GRASS)
                    .noOcclusion()
                    .noCollission()
            ));

    public static final Block DISTORTION_WEED_SPAWN = registerBlock("distortion_weed_spawn",
            new DistortionWeedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .instabreak()
                    .sound(SoundType.WET_GRASS)
                    .noOcclusion()
                    .noCollission(),
                    false
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

    public static final Block EMPTYPHONE = registerBlockWithoutItem("emptyphone",
            new EmptyPhoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.3f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    public static final Block UNION_ROOM_WALL = registerBlock("union_room_wall",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(0.4f, 6.0f)
                    .sound(SoundType.STONE)));

    public static final Block UNION_ROOM_FLOOR = registerBlock("union_room_floor",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(0.4f, 6.0f)
                    .sound(SoundType.STONE)));

    public static final Block UNION_ROOM_LIGHT = registerBlock("union_room_light",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(0.4f, 6.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 15)));

    public static final Block UNION_ROOM_LIGHT_LINE = registerBlock("union_room_light_line",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(0.4f, 6.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 15)));

    public static final Block UNION_ROOM_PILLAR = registerBlock("union_room_pillar",
            new UnionRoomPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(0.4f, 6.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    public static final Block UNION_ROOM_SCREEN = registerBlock("union_room_screen",
            new UnionRoomDecorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(s -> 15)));
    public static final Block UNION_ROOM_SCREEN_DISPLAY = registerBlockWithoutItem("union_room_screen_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.3f)
                    .noOcclusion()
                    .noLootTable()
                    .noCollission()));

    public static final Block UNION_ROOM_SCREEN_LEFT = registerBlock("union_room_screen_left",
            new UnionRoomDecorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(s -> 15)));
    public static final Block UNION_ROOM_SCREEN_LEFT_DISPLAY = registerBlockWithoutItem("union_room_screen_left_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.3f)
                    .noOcclusion()
                    .noLootTable()
                    .noCollission()));

    public static final Block UNION_ROOM_SCREEN_RIGHT = registerBlock("union_room_screen_right",
            new UnionRoomDecorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(s -> 15)));
    public static final Block UNION_ROOM_SCREEN_RIGHT_DISPLAY = registerBlockWithoutItem("union_room_screen_right_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.3f)
                    .noOcclusion()
                    .noLootTable()
                    .noCollission()));

    public static final Block UNION_ROOM_CROWD = registerBlock("union_room_crowd",
            new UnionRoomDecorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.3f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(s -> 15)));
    public static final Block UNION_ROOM_CROWD_DISPLAY = registerBlockWithoutItem("union_room_crowd_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.3f)
                    .noOcclusion()
                    .noLootTable()
                    .noCollission()));
    public static final Block UNION_ROOM_POKEBALL = registerBlock("union_room_pokeball",
            new UnionRoomDecorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.3f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(s -> 15)));
    public static final Block UNION_ROOM_POKEBALL_DISPLAY = registerBlockWithoutItem("union_room_pokeball_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.3f)
                    .noOcclusion()
                    .noLootTable()
                    .noCollission()));
    public static final Block UNION_ROOM_SPOT = registerBlock("union_room_spot",
            new UnionRoomDecorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(s -> 15)));
    public static final Block UNION_ROOM_SPOT_DISPLAY = registerBlockWithoutItem("union_room_spot_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.3f)
                    .noOcclusion()
                    .noLootTable()
                    .noCollission()));
    public static final Block UNION_ROOM_GLOBE = registerBlock("union_room_globe",
            new UnionRoomGlobeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.5f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));
    public static final Block UNION_ROOM_EXIT_TELEPORTER = registerBlock("union_room_exit_teleporter",
            new UnionRoomExitTeleporterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.5f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));
    public static final Block ONLINE_FEATURE_PC = registerBlock("online_feature_pc",
            new OnlineFeaturePcBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final Block ONLINE_FEATURE_PC_UNION = registerBlock("online_feature_pc_union",
            new OnlineFeaturePcBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion(), OnlineFeaturePcBlock.Kind.UNION));
    public static final Block ONLINE_FEATURE_PC_GTS = registerBlock("online_feature_pc_gts",
            new OnlineFeaturePcBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion(), OnlineFeaturePcBlock.Kind.GTS));
    public static final Block ONLINE_FEATURE_PC_WONDER = registerBlock("online_feature_pc_wonder",
            new OnlineFeaturePcBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion(), OnlineFeaturePcBlock.Kind.WONDER));
    public static final Block UNION_ROOM_GLOBE_DISPLAY_MOVING = registerBlockWithoutItem("union_room_globe_display_moving",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.3f)
                    .noOcclusion()
                    .noLootTable()
                    .noCollission()));

    public static final Block BULBASAUR_STATUE = registerBlock("bulbasaur_statue",
            new PokemonStatueBlock.Bulbasaur(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)));
    public static final Block CHARMANDER_STATUE = registerBlock("charmander_statue",
            new PokemonStatueBlock.Charmander(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)));
    public static final Block PIKACHU_STATUE = registerBlock("pikachu_statue",
            new PokemonStatueBlock.Pikachu(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)));
    public static final Block SQUIRTLE_STATUE = registerBlock("squirtle_statue",
            new PokemonStatueBlock.Squirtle(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)));
    public static final Block KARATE_MANNEQUIN = registerBlock("karate_mannequin",
            new KarateMannequinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final Block MUD_PILE = registerBlock("mud_pile",
            new MudPileBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.6f)
                    .sound(SoundType.MUD)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    public static final Block SLUDGE_PILE = registerBlock("sludge_pile",
            new SludgePileBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6f)
                    .sound(SoundType.MUD)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    public static final Block VOLCANIC_CRATER = registerBlock("volcanic_crater",
            new VolcanicCraterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.NETHERRACK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final Block HOT_GEYSER = registerBlock("hot_geyser",
            new HotGeyserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final Block DRACONIC_CRATER = registerBlock("draconic_crater",
            new DraconicCraterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.BASALT)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final Block TRAP_DARKNESS = registerBlock("trap_darkness",
            new DarknessTrapBlock(trapProps(), false));
    public static final Block TRAP_DARKNESS_HARD = registerBlock("trap_darkness_hard",
            new DarknessTrapBlock(trapProps(), true));
    public static final Block TRAP_EXPLOSION = registerBlock("trap_explosion",
            new ExplosionTrapBlock(trapProps(), false));
    public static final Block TRAP_EXPLOSION_HARD = registerBlock("trap_explosion_hard",
            new ExplosionTrapBlock(trapProps(), true));
    public static final Block TRAP_FART = registerBlock("trap_fart",
            new FartTrapBlock(trapProps(), false));
    public static final Block TRAP_FART_HARD = registerBlock("trap_fart_hard",
            new FartTrapBlock(trapProps(), true));
    public static final Block TRAP_FIRE = registerBlock("trap_fire",
            new FireTrapBlock(trapProps(), false));
    public static final Block TRAP_FIRE_HARD = registerBlock("trap_fire_hard",
            new FireTrapBlock(trapProps(), true));
    public static final Block TRAP_GRAVITY = registerBlock("trap_gravity",
            new GravityTrapBlock(trapProps(), false));
    public static final Block TRAP_GRAVITY_HARD = registerBlock("trap_gravity_hard",
            new GravityTrapBlock(trapProps(), true));
    public static final Block TRAP_MOVE = registerBlock("trap_move",
            new MoveTrapBlock(trapProps(), false));
    public static final Block TRAP_MOVE_HARD = registerBlock("trap_move_hard",
            new MoveTrapBlock(trapProps(), true));
    public static final Block TRAP_ROCK = registerBlock("trap_rock",
            new RockTrapBlock(trapProps(), false));
    public static final Block TRAP_ROCK_HARD = registerBlock("trap_rock_hard",
            new RockTrapBlock(trapProps(), true));
    public static final Block TRAP_SLOW = registerBlock("trap_slow",
            new SlowTrapBlock(trapProps(), false));
    public static final Block TRAP_SLOW_HARD = registerBlock("trap_slow_hard",
            new SlowTrapBlock(trapProps(), true));
    public static final Block TRAP_TELEPORT = registerBlock("trap_teleport",
            new TeleportTrapBlock(trapProps(), false));
    public static final Block TRAP_TELEPORT_HARD = registerBlock("trap_teleport_hard",
            new TeleportTrapBlock(trapProps(), true));
    public static final Block TRAP_WIND = registerBlock("trap_wind",
            new WindTrapBlock(trapProps(), false));
    public static final Block TRAP_WIND_HARD = registerBlock("trap_wind_hard",
            new WindTrapBlock(trapProps(), true));

    public static final Block PUNCHINGBAG = registerBlock("punchingbag",
            new PunchingBagBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final Block PUNCHINGBAG_BAG_DISPLAY = registerBlockWithoutItem("punchingbag_bag_display",
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0f)
                    .noOcclusion()
                    .noLootTable()
                    .noCollission()));

    public static final Block TOMBSTONE = registerBlock("tombstone",
            new TombstoneBlock.Standard(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final Block TOMBSTONE_SMALL = registerBlock("tombstone_small",
            new TombstoneBlock.Small(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final Block DISTORTION_ROCK_BLOCK = registerBlock("distortion_rock_block",
            new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)));

    public static final Block DISTORTION_ROCK_BLOCK_STONEBRICKS = registerBlock("distortion_rock_block_stonebricks",
            new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)));

    public static final Block DISTORTION_ROCK_BLOCK_STONEBRICKS_STAIRS = registerBlock("distortion_rock_block_stonebricks_stairs",
            new StairBlock(DISTORTION_ROCK_BLOCK_STONEBRICKS.defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(DISTORTION_ROCK_BLOCK_STONEBRICKS)));

    public static final Block DISTORTION_ROCK_BLOCK_STONEBRICKS_SLAB = registerBlock("distortion_rock_block_stonebricks_slab",
            new SlabBlock(BlockBehaviour.Properties.ofFullCopy(DISTORTION_ROCK_BLOCK_STONEBRICKS)));

    public static final Block DISTORTION_ROCK_BLOCK_CHISELED = registerBlock("distortion_rock_block_chiseled",
            new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)));

    public static final Block DISTORTION_ROCK_BLOCK_CRACKED = registerBlock("distortion_rock_block_cracked",
            new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)));

    public static final Block TINKAGEAR_BLOCK = registerBlock("tinkagear_block",
            new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).noOcclusion()));

    public static final Block UNION_ROOM_BRICKS_GREEN = registerBlock("union_room_bricks_green",
            unionRoomBricksBlock());
    public static final Block UNION_ROOM_BRICKS_YELLOW = registerBlock("union_room_bricks_yellow",
            unionRoomBricksBlock());
    public static final Block UNION_ROOM_BRICKS_BLUE = registerBlock("union_room_bricks_blue",
            unionRoomBricksBlock());
    public static final Block UNION_ROOM_BRICKS_RED = registerBlock("union_room_bricks_red",
            unionRoomBricksBlock());

    public static final Block UNION_ROOM_SPOTLIGHT_GREEN = registerBlock("union_room_spotlight_green",
            unionRoomSpotlightBlock());
    public static final Block UNION_ROOM_SPOTLIGHT_YELLOW = registerBlock("union_room_spotlight_yellow",
            unionRoomSpotlightBlock());
    public static final Block UNION_ROOM_SPOTLIGHT_BLUE = registerBlock("union_room_spotlight_blue",
            unionRoomSpotlightBlock());
    public static final Block UNION_ROOM_SPOTLIGHT_RED = registerBlock("union_room_spotlight_red",
            unionRoomSpotlightBlock());

    public static final Block UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_GREEN = registerBlockWithoutItem(
            "union_room_spotlight_display_light_green", unionRoomSpotlightDisplayLightBlock());
    public static final Block UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_YELLOW = registerBlockWithoutItem(
            "union_room_spotlight_display_light_yellow", unionRoomSpotlightDisplayLightBlock());
    public static final Block UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_BLUE = registerBlockWithoutItem(
            "union_room_spotlight_display_light_blue", unionRoomSpotlightDisplayLightBlock());
    public static final Block UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_RED = registerBlockWithoutItem(
            "union_room_spotlight_display_light_red", unionRoomSpotlightDisplayLightBlock());

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

    private static final String SPHERE_PREFIX = "sphere_";

    static {
        for (String type : SPHERE_TYPES_ARRAY) {
            MapColor color = getSphereMapColor(type);
            Block base = registerBlock(SPHERE_PREFIX + type + "_block",
                    new Block(BlockBehaviour.Properties.of()
                            .mapColor(color)
                            .strength(1.5f, 6.0f)
                            .sound(SoundType.STONE)
                    ));
            SPHERE_BLOCKS.put(type, base);
        }
        for (String type : SPHERE_TYPES_ARRAY) {
            Block base = SPHERE_BLOCKS.get(type);
            SPHERE_SLABS.put(type, registerBlock(SPHERE_PREFIX + type + "_slab",
                    new SlabBlock(BlockBehaviour.Properties.ofFullCopy(base))));
        }
        for (String type : SPHERE_TYPES_ARRAY) {
            Block base = SPHERE_BLOCKS.get(type);
            SPHERE_STAIRS.put(type, registerBlock(SPHERE_PREFIX + type + "_stairs",
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

    private static BlockBehaviour.Properties trapProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(0.2f)
                .sound(SoundType.GRASS)
                .noOcclusion()
                .noCollission()
                .pushReaction(PushReaction.DESTROY)
                .isViewBlocking((s, l, p) -> false)
                .isSuffocating((s, l, p) -> false);
    }

    private static Block unionRoomBricksBlock() {
        return new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(0.4f, 6.0f)
                .sound(SoundType.STONE));
    }

    private static UnionRoomSpotlightBlock unionRoomSpotlightBlock() {
        return new UnionRoomSpotlightBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(0.4f, 6.0f)
                .sound(SoundType.STONE)
                .lightLevel(state -> 15)
                .noOcclusion());
    }

    private static UnionRoomSpotlightDisplayLightBlock unionRoomSpotlightDisplayLightBlock() {
        return new UnionRoomSpotlightDisplayLightBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(0.3f)
                .noOcclusion()
                .noLootTable()
                .noCollission());
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

        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "emptyphone"),
                new maxigregrze.cobblesafari.item.EmptyPhoneItem(EMPTYPHONE, new Item.Properties()));
    }
}
