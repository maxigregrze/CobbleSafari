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
 * Util d'arène (plan 108 § 1) : repère les blocs « surface » (sommet exposé, {@code y = triggerY ± tol})
 * dans le disque des participants, et pose des blocs sur cette surface. Appelé ponctuellement
 * (pas par tick) — un scan complet du disque est acceptable.
 */
public final class CsBossSurfaceScanner {

    public static final int DEFAULT_Y_TOLERANCE = 3;

    private CsBossSurfaceScanner() {}

    /** Surfaces du disque de l'arène, ancrées sur le Y du bloc trigger ± {@link #DEFAULT_Y_TOLERANCE}. */
    public static List<BlockPos> scanSurface(ServerLevel level, BossBattleSession session) {
        return scanSurface(level, session.getArenaCenter(), session.getPlayerRadius(),
                session.getTriggerPos().getY(), DEFAULT_Y_TOLERANCE);
    }

    /**
     * Pour chaque colonne du disque ({@code dx²+dz² ≤ radius²}), le premier bloc solide (en partant
     * du haut) dont le bloc au‑dessus est remplaçable, avec {@code y ∈ [triggerY−tol, triggerY+tol]}.
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
     * Surface d'une colonne ({@code x},{@code z}) : premier bloc solide en partant du haut, dont le
     * bloc au‑dessus est remplaçable, avec {@code y ∈ [triggerY−tol, triggerY+tol]} ({@code null} sinon).
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

    /** Pose {@code state} au‑dessus d'une surface si la case est remplaçable. */
    public static boolean placeOnSurface(ServerLevel level, BlockPos surface, BlockState state) {
        BlockPos pos = surface.above();
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        level.setBlockAndUpdate(pos, state);
        return true;
    }
}
