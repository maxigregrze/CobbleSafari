package maxigregrze.cobblesafari.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The single lookup every neighbour-derived block goes through instead of
 * {@code level.getBlockState(pos)}: it returns what the block at {@code pos} should be
 * <em>treated as</em>, letting a disguised block (the CSBoss mimic) read as the block it copies.
 *
 * <p>Resolution happens here, at the read site, and deliberately <em>not</em> inside
 * {@link ConnectedModelBlock#connectsTo} / {@code stacksWith}. Those two hooks keep their bodies
 * untouched, so the connection graph's asymmetry - "connectsTo always reads the tag of the block
 * being asked", see the tag declarations in {@code ModBlocks} - is preserved by construction, and
 * a subclass overriding them cannot accidentally see through the disguise.</p>
 */
public final class ApparentState {

    private ApparentState() {}

    /**
     * The state to treat {@code pos} as. Identical to {@code level.getBlockState(pos)} for every
     * ordinary block, at the cost of one {@code instanceof}.
     */
    public static BlockState at(BlockGetter level, BlockPos pos) {
        BlockState real = level.getBlockState(pos);
        if (real.getBlock() instanceof ApparentStateBlock disguise) {
            BlockState apparent = disguise.apparentState(real, level, pos);
            if (apparent != null) {
                return apparent;
            }
        }
        return real;
    }
}
