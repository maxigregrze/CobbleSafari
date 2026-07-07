package maxigregrze.cobblesafari.csboss;

import maxigregrze.cobblesafari.block.csboss.BattleReactiveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Scan + toggle reactive blocks around a trigger.
 * Square of side {@code 2·blockRadiusChunks·16 + 1} (demi-côté = chunks × 16) + bounded Y range.
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
        int minX = trigger.getX() - r;
        int maxX = trigger.getX() + r;
        int minZ = trigger.getZ() - r;
        int maxZ = trigger.getZ() + r;
        // Iterate chunk-by-chunk: never force-load a chunk just to scan it (getChunkNow), and skip whole
        // sections that are only air. Correctness is unchanged (every reactive block is still found) but the
        // per-activation cost drops from up to ~2M blind getBlockState to the actual non-air volume (B1).
        for (int cx = minX >> 4; cx <= (maxX >> 4); cx++) {
            for (int cz = minZ >> 4; cz <= (maxZ >> 4); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                int x0 = Math.max(minX, cx << 4);
                int x1 = Math.min(maxX, (cx << 4) + 15);
                int z0 = Math.max(minZ, cz << 4);
                int z1 = Math.min(maxZ, (cz << 4) + 15);
                for (int y = minY; y <= maxY; y++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                    if (section.hasOnlyAir()) {
                        continue;
                    }
                    for (int x = x0; x <= x1; x++) {
                        for (int z = z0; z <= z1; z++) {
                            BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                            if (state.getBlock() instanceof BattleReactiveBlock reactive && reactive.isReactive(state)) {
                                out.add(new BlockPos(x, y, z));
                            }
                        }
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
