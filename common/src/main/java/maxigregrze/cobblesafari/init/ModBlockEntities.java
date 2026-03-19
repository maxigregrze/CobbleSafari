package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.block.basepc.BasePCBlockEntity;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.block.dungeon.HoopaRingPortalBlockEntity;
import maxigregrze.cobblesafari.block.misc.GiratinaCoreBlockEntity;
import maxigregrze.cobblesafari.block.misc.DistortionPortalBlockEntity;
import maxigregrze.cobblesafari.block.misc.SafariEggNestBlockEntity;
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
    public static BlockEntityType<BasePCBlockEntity> SECRETBASE_PC;
    public static BlockEntityType<IncubatorBlockEntity> INCUBATOR;

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
    }
}
