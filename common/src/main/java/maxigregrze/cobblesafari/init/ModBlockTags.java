package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {

    public static final TagKey<Block> TRAPS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "traps"));
    public static final TagKey<Block> TRAP_SOFT_BLOCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "trap_soft_blocks"));

    /** The three maple leaf piles, so the litter logic never needs three instanceof checks. */
    public static final TagKey<Block> MAPLE_LEAF_PILES = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "maple_leaf_piles"));

    /** Ground a natural Hoopa portal may be placed above (action plan 188, P4). */
    public static final TagKey<Block> PORTAL_SPAWNABLE_ON = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "portal_spawnable_on"));
    /** Blocks that keep natural Hoopa portals out of {@code repellentRadius} (action plan 188, P5). */
    public static final TagKey<Block> PORTAL_REPELLENTS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "portal_repellents"));

    /** Slightly-low floors (15 px roads) a creative teleport pad may sit on, drawn 1 px lower. */
    public static final TagKey<Block> TELEPORT_PAD_LOW_SUPPORTS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "teleport_pad_low_supports"));

    /**
     * Road-like floors (15 px top) a shard cluster or a lost item may stand on, drawn 1 px lower
     * (plan 196, sections 7.9 and 8.8). See {@code LoweredSupport} for the 15 px check.
     */
    public static final TagKey<Block> LOWERED_SUPPORTS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "lowered_supports"));

    private ModBlockTags() {}
}
