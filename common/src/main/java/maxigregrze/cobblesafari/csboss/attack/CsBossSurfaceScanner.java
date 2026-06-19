package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Arena util: identifies "surface" blocks (exposed top, {@code y = triggerY ± tol})
 * in the participant disk, and places blocks on that surface. Called occasionally
 * (not per tick) — a full disk scan is acceptable.
 */
public final class CsBossSurfaceScanner {

    public static final int DEFAULT_Y_TOLERANCE = 3;

    private CsBossSurfaceScanner() {}

    /** Surfaces of the arena block-detection square, anchored to trigger block Y ± {@link #DEFAULT_Y_TOLERANCE}. */
    public static List<BlockPos> scanSurface(ServerLevel level, BossBattleSession session) {
        int half = (int) Math.ceil(session.getBlockRadius());
        return scanSurfaceSquare(level, session.getArenaCenter(), half,
                session.getTriggerPos().getY(), DEFAULT_Y_TOLERANCE);
    }

    /**
     * For each column of the square ({@code |dx| ≤ radius, |dz| ≤ radius}), the first solid block
     * (from the top) whose block above is replaceable, with {@code y ∈ [triggerY−tol, triggerY+tol]}.
     */
    public static List<BlockPos> scanSurfaceSquare(ServerLevel level, Vec3 center, int radius,
                                                   int triggerY, int yTolerance) {
        List<BlockPos> out = new ArrayList<>();
        int cx = (int) Math.floor(center.x);
        int cz = (int) Math.floor(center.z);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                for (int y = triggerY + yTolerance; y >= triggerY - yTolerance; y--) {
                    p.set(x, y, z);
                    if (isSurface(level, p)) {
                        out.add(p.immutable());
                        break;
                    }
                }
            }
        }
        return out;
    }

    /**
     * For each column of the disk ({@code dx²+dz² ≤ radius²}), the first solid block (from the top)
     * whose block above is replaceable, with {@code y ∈ [triggerY−tol, triggerY+tol]}.
     */
    public static List<BlockPos> scanSurface(ServerLevel level, Vec3 center, int radius, int triggerY, int yTolerance) {
        List<BlockPos> out = new ArrayList<>();
        int cx = (int) Math.floor(center.x);
        int cz = (int) Math.floor(center.z);
        int r2 = radius * radius;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                for (int y = triggerY + yTolerance; y >= triggerY - yTolerance; y--) {
                    p.set(x, y, z);
                    if (isSurface(level, p)) {
                        out.add(p.immutable());
                        break;
                    }
                }
            }
        }
        return out;
    }

    /**
     * Surface of a column ({@code x},{@code z}): first solid block from the top, whose
     * block above is replaceable, with {@code y ∈ [triggerY−tol, triggerY+tol]} ({@code null} otherwise).
     */
    @org.jetbrains.annotations.Nullable
    public static BlockPos findSurfaceColumn(ServerLevel level, int x, int z, int triggerY, int yTolerance) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = triggerY + yTolerance; y >= triggerY - yTolerance; y--) {
            p.set(x, y, z);
            if (isSurface(level, p)) {
                return p.immutable();
            }
        }
        return null;
    }

    private static boolean isSurface(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.isAir() || !s.getFluidState().isEmpty() || !s.isFaceSturdy(level, p, Direction.UP)) {
            return false;
        }
        BlockState above = level.getBlockState(p.above());
        return above.isAir() || above.canBeReplaced();
    }

    /** Places {@code state} above a surface if the cell is replaceable. */
    public static boolean placeOnSurface(ServerLevel level, BlockPos surface, BlockState state) {
        BlockPos pos = surface.above();
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        level.setBlockAndUpdate(pos, state);
        return true;
    }
}
