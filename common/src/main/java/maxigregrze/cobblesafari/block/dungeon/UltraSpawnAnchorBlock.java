package maxigregrze.cobblesafari.block.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;

/**
 * Marks a spot where the sub-dungeon's wild encounter may appear. A plain full block with no
 * behaviour whatsoever: its whole purpose is to be replaced (action plan 183, section 4.3).
 *
 * <p>A structure may carry any number of them. The generation pipeline collects every one inside
 * the captured structure bounds, draws <b>one</b> uniformly at random for the encounter, and
 * erases them all - a marker left behind would be an accidental decorative block in a gameplay
 * structure.</p>
 *
 * <p>It carries an item and a visible model on purpose. This is an author's tool, placed by hand
 * while building in Blockbench or in-world, so it has to be reachable from the creative tab and
 * recognisable at a glance - unlike {@code DUNGEON_PORTAL_EFFECT}, which only code ever places.</p>
 */
public class UltraSpawnAnchorBlock extends Block {

    public static final MapCodec<UltraSpawnAnchorBlock> CODEC = simpleCodec(UltraSpawnAnchorBlock::new);

    public UltraSpawnAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
