package maxigregrze.cobblesafari.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A block that may ask to be <em>treated as</em> a different block than it is - a disguise the
 * neighbour-derived engine honours when it decides what connects to what.
 *
 * <p>Implemented by the CSBoss mimic block, whose identity lives in its block entity and is
 * therefore invisible to any rule that only reads a {@link BlockState}. This interface is the
 * inversion that keeps {@code block/base/} free of any import from {@code block/csboss/}: the
 * generic layer declares the contract, the mimic implements it.</p>
 *
 * @see ApparentState the lookup every read site goes through
 */
public interface ApparentStateBlock {

    /**
     * The state this block should be treated as at {@code pos}, or {@code null} to be taken at
     * face value. Must be pure and cheap: it is reached from {@code computeState}, which runs on
     * every neighbour update. Implementations return a stored value, never a computation.
     */
    @Nullable
    BlockState apparentState(BlockState self, BlockGetter level, BlockPos pos);
}
