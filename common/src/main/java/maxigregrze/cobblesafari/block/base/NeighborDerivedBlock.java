package maxigregrze.cobblesafari.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block whose blockstate is derived from its surroundings, and which can therefore be asked
 * "what would you look like if you stood here?" without actually being placed there.
 *
 * <p>This is what lets a disguise resolve a full state rather than a bare
 * {@code defaultBlockState()}: the mimic hands its own position to the block it copies and stores
 * the answer. A target that does not implement this simply falls back to its default state, which
 * is how every non-participating block (multiblocks, facing blocks, plain cubes) degrades.</p>
 */
public interface NeighborDerivedBlock {

    /**
     * The state this block would carry if it stood at {@code pos}. Must be pure - it runs from
     * inside neighbour updates, where a re-entrant write is a classic source of state corruption.
     */
    BlockState apparentStateAt(BlockGetter level, BlockPos pos);
}
