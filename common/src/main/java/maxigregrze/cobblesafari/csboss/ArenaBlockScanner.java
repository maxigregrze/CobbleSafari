package maxigregrze.cobblesafari.csboss;

import maxigregrze.cobblesafari.block.csboss.BattleReactiveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Scan + toggle reactive blocks around a trigger (plan 100 § 10).
 * Radius in chunks (X/Z) + bounded fixed Y range.
 */
public final class ArenaBlockScanner {

    /** Y range relative to trigger (bounded to avoid an oversized scan). */
    private static final int ARENA_Y_BELOW = 8;
    private static final int ARENA_Y_ABOVE = 24;

    private ArenaBlockScanner() {}

    public static List<BlockPos> scan(ServerLevel level, BlockPos trigger, int blockRadiusChunks) {
        List<BlockPos> out = new ArrayList<>();
        int r = blockRadiusChunks * 16;
        int minY = Math.max(level.getMinBuildHeight(), trigger.getY() - ARENA_Y_BELOW);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, trigger.getY() + ARENA_Y_ABOVE);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = trigger.getX() - r; x <= trigger.getX() + r; x++) {
            for (int z = trigger.getZ() - r; z <= trigger.getZ() + r; z++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof BattleReactiveBlock reactive && reactive.isReactive(state)) {
                        out.add(cursor.immutable());
                    }
                }
            }
        }
        return out;
    }

    public static void setBattleState(ServerLevel level, List<BlockPos> positions, boolean battle) {
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BattleReactiveBlock reactive) {
                reactive.setBattleState(level, pos, state, battle);
            }
        }
    }

    /** Convenience check for restoring the trigger itself via its block. */
    public static boolean isReactiveBlock(Block block) {
        return block instanceof BattleReactiveBlock;
    }
}
