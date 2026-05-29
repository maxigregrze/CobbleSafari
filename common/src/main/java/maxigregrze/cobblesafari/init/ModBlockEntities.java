package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.block.basepc.BasePCBlockEntity;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.block.dungeon.HoopaRingPortalBlockEntity;
import maxigregrze.cobblesafari.block.distortion.GiratinaCoreBlockEntity;
import maxigregrze.cobblesafari.block.distortion.DistortionPortalBlockEntity;
import maxigregrze.cobblesafari.block.distortion.DistortionStoneBricksRuneBlockEntity;
import maxigregrze.cobblesafari.block.misc.LostNotesBlockEntity;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballBlockEntity;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballGoldBlockEntity;
import maxigregrze.cobblesafari.block.misc.LostItemBlockEntity;
import maxigregrze.cobblesafari.block.misc.SafariEggNestBlockEntity;
import maxigregrze.cobblesafari.block.misc.WhirlwindBlockEntity;
import maxigregrze.cobblesafari.block.misc.UnionRoomDecorBlockEntity;
import maxigregrze.cobblesafari.block.misc.UnionRoomGlobeUpperBlockEntity;
import maxigregrze.cobblesafari.block.misc.UnionRoomSpotlightBlockEntity;
import maxigregrze.cobblesafari.block.misc.UnionRoomExitTeleporterBlockEntity;
import maxigregrze.cobblesafari.block.misc.VoidBlockEntity;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.incubator.IncubatorBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    private ModBlockEntities() {}

    public static BlockEntityType<SafariEggNestBlockEntity> SAFARI_EGG_NEST;
    public static BlockEntityType<HoopaRingPortalBlockEntity> HOOPA_RING_PORTAL;
    public static BlockEntityType<DungeonPortalBlockEntity> DUNGEON_PORTAL;
    public static BlockEntityType<VoidBlockEntity> VOID_BLOCK;
    public static BlockEntityType<GiratinaCoreBlockEntity> GIRATINA_CORE;
    public static BlockEntityType<DistortionPortalBlockEntity> DISTORTION_PORTAL;
    public static BlockEntityType<DistortionStoneBricksRuneBlockEntity> DISTORTION_STONEBRICKS_RUNE;
    public static BlockEntityType<LostNotesBlockEntity> LOST_NOTES;
    public static BlockEntityType<LostItemBlockEntity> LOST_ITEM;
    public static BlockEntityType<AuspiciousPokeballBlockEntity> AUSPICIOUS_POKEBALL;
    public static BlockEntityType<AuspiciousPokeballGoldBlockEntity> AUSPICIOUS_POKEBALL_GOLD;
    public static BlockEntityType<BasePCBlockEntity> SECRETBASE_PC;
    public static BlockEntityType<IncubatorBlockEntity> INCUBATOR;
    public static BlockEntityType<UnionRoomDecorBlockEntity> UNION_ROOM_DECOR;
    public static BlockEntityType<UnionRoomGlobeUpperBlockEntity> UNION_ROOM_GLOBE_UPPER;
    public static BlockEntityType<UnionRoomExitTeleporterBlockEntity> UNION_ROOM_EXIT_TELEPORTER;
    public static BlockEntityType<WhirlwindBlockEntity> WHIRLWIND;
    public static BlockEntityType<UnionRoomSpotlightBlockEntity> UNION_ROOM_SPOTLIGHT;

    public static void register() {
        CobbleSafari.LOGGER.info("Registering block entities for " + CobbleSafari.MOD_ID);

        SAFARI_EGG_NEST = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "safari_egg_nest"),
                BlockEntityType.Builder.of(SafariEggNestBlockEntity::new, ModBlocks.SAFARI_EGG_NEST).build(null)
        );

        HOOPA_RING_PORTAL = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "hoopa_ring_portal"),
                BlockEntityType.Builder.of(HoopaRingPortalBlockEntity::new, ModBlocks.HOOPA_RING_PORTAL).build(null)
        );

        DUNGEON_PORTAL = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_portal"),
                BlockEntityType.Builder.of(DungeonPortalBlockEntity::new, ModBlocks.DUNGEON_PORTAL, ModBlocks.CREATIVE_DUNGEON_PORTAL).build(null)
        );

        VOID_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "void_block"),
                BlockEntityType.Builder.of(VoidBlockEntity::new, ModBlocks.VOID_BLOCK).build(null)
        );

        GIRATINA_CORE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "giratina_core"),
                BlockEntityType.Builder.of(GiratinaCoreBlockEntity::new, ModBlocks.GIRATINA_CORE).build(null)
        );

        DISTORTION_PORTAL = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "distortion_portal"),
                BlockEntityType.Builder.of(DistortionPortalBlockEntity::new, ModBlocks.DISTORTION_PORTAL).build(null)
        );

        DISTORTION_STONEBRICKS_RUNE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "distortion_stone_bricks_rune"),
                BlockEntityType.Builder.of(DistortionStoneBricksRuneBlockEntity::new, ModBlocks.DISTORTION_STONE_BRICKS_RUNE).build(null)
        );

        LOST_NOTES = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "lost_notes"),
                BlockEntityType.Builder.of(LostNotesBlockEntity::new, ModBlocks.LOST_NOTES).build(null)
        );

        LOST_ITEM = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "lost_item"),
                BlockEntityType.Builder.of(LostItemBlockEntity::new, ModBlocks.LOST_ITEM).build(null)
        );

        AUSPICIOUS_POKEBALL = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "auspicious_pokeball"),
                BlockEntityType.Builder.of(AuspiciousPokeballBlockEntity::new, ModBlocks.AUSPICIOUS_POKEBALL, ModBlocks.AUSPICIOUS_POKEBALL_SMALL).build(null)
        );

        AUSPICIOUS_POKEBALL_GOLD = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "auspiciouspokeball_gold"),
                BlockEntityType.Builder.of(AuspiciousPokeballGoldBlockEntity::new, ModBlocks.AUSPICIOUS_POKEBALL_GOLD).build(null)
        );

        SECRETBASE_PC = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "secretbase_pc"),
                BlockEntityType.Builder.of(BasePCBlockEntity::new, ModBlocks.SECRETBASE_PC).build(null)
        );

        INCUBATOR = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "incubator"),
                BlockEntityType.Builder.of(IncubatorBlockEntity::new, ModBlocks.INCUBATOR).build(null)
        );

        UNION_ROOM_DECOR = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "union_room_decor"),
                BlockEntityType.Builder.of(UnionRoomDecorBlockEntity::new,
                        ModBlocks.UNION_ROOM_CROWD,
                        ModBlocks.UNION_ROOM_POKEBALL,
                        ModBlocks.UNION_ROOM_SPOT,
                        ModBlocks.UNION_ROOM_SCREEN,
                        ModBlocks.UNION_ROOM_SCREEN_LEFT,
                        ModBlocks.UNION_ROOM_SCREEN_RIGHT).build(null)
        );

        UNION_ROOM_GLOBE_UPPER = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "union_room_globe_upper"),
                BlockEntityType.Builder.of(UnionRoomGlobeUpperBlockEntity::new,
                        ModBlocks.UNION_ROOM_GLOBE,
                        ModBlocks.UNION_ROOM_EXIT_TELEPORTER).build(null)
        );

        UNION_ROOM_EXIT_TELEPORTER = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "union_room_exit_teleporter"),
                BlockEntityType.Builder.of(UnionRoomExitTeleporterBlockEntity::new, ModBlocks.UNION_ROOM_EXIT_TELEPORTER).build(null)
        );

        WHIRLWIND = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "whirlwind"),
                BlockEntityType.Builder.of(WhirlwindBlockEntity::new, ModBlocks.WHIRLWIND).build(null)
        );

        UNION_ROOM_SPOTLIGHT = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "union_room_spotlight"),
                BlockEntityType.Builder.of(UnionRoomSpotlightBlockEntity::new,
                        ModBlocks.UNION_ROOM_SPOTLIGHT_GREEN,
                        ModBlocks.UNION_ROOM_SPOTLIGHT_YELLOW,
                        ModBlocks.UNION_ROOM_SPOTLIGHT_BLUE,
                        ModBlocks.UNION_ROOM_SPOTLIGHT_RED).build(null)
        );
    }
}
