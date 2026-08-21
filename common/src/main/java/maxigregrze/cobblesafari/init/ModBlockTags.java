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

    private ModBlockTags() {}
}
