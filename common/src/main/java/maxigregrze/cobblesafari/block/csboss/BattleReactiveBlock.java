package maxigregrze.cobblesafari.block.csboss;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Arena "reactive" block: implemented by blocks that change state
 * at boss fight start/end (entrance gate, etc.). The scan touches ONLY
 * blocks implementing this interface — never arbitrary world edits.
 */
public interface BattleReactiveBlock {

    /** Toggles idle↔combat state. Must be idempotent and purely local to the block. */
    void setBattleState(ServerLevel level, BlockPos pos, BlockState state, boolean battle);

    /** {@code true} if this block, in this state, should be captured by the scan. */
    default boolean isReactive(BlockState state) {
        return true;
    }
}
