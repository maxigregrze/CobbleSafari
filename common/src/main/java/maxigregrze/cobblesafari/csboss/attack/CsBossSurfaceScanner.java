package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.block.csboss.BattleReactiveBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossMimicBlock;
import maxigregrze.cobblesafari.block.csboss.CsBossTileBlockEntity;
import maxigregrze.cobblesafari.csboss.BossBattleSession;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Arena util: identifies "surface" blocks (exposed top, {@code y = triggerY ± tol})
 * in the participant disk, and places blocks on that surface. Called occasionally
 * (not per tick) - a full disk scan is acceptable.
 */
public final class CsBossSurfaceScanner {

    public static final int DEFAULT_Y_TOLERANCE = 3;

    private CsBossSurfaceScanner() {}

    /** Surfaces of the arena block-detection square, anchored to the detected floor Y ± {@link #DEFAULT_Y_TOLERANCE}. */
    public static List<BlockPos> scanSurface(ServerLevel level, BossBattleSession session) {
        int half = (int) Math.ceil(session.getBlockRadius());
        return scanSurfaceSquare(level, session.getArenaCenter(), half,
                session.getFloorY(), DEFAULT_Y_TOLERANCE);
    }

    /**
     * For each column of the square ({@code |dx| ≤ radius, |dz| ≤ radius}), the first ground block
     * (from the top, see {@link #isSurface}) whose cell above can hold a tile, with {@code y ∈ [triggerY−tol, triggerY+tol]}.
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
     * For each column of the disk ({@code dx²+dz² ≤ radius²}), the first ground block (from the top)
     * whose cell above can hold a tile, with {@code y ∈ [triggerY−tol, triggerY+tol]}.
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
     * Surface of a column ({@code x},{@code z}): first ground block from the top, whose
     * cell above can hold a tile, with {@code y ∈ [triggerY−tol, triggerY+tol]} ({@code null} otherwise).
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

    /**
     * Ground block whose cell above can hold a tile (retours plan 199). Any block with a collision
     * counts as ground, sturdy top or not (leaves, slabs, stairs...). Arena blocks are read in their
     * fight state, so a mimic the opening wave has not reached yet still counts as the floor it is
     * about to become - otherwise the scan falls through it onto whatever lies underneath.
     */
    private static boolean isSurface(ServerLevel level, BlockPos p) {
        BlockState s = inBattle(level.getBlockState(p));
        if (s.isAir() || !s.getFluidState().isEmpty() || s.getCollisionShape(level, p).isEmpty()) {
            return false;
        }
        BlockPos up = p.above();
        BlockState above = inBattle(level.getBlockState(up));
        return above.isAir() || above.canBeReplaced() || isDecoration(level, up, above) || isHiddenMimic(above);
    }

    /**
     * Mimic block that is gone during the fight: still a block, so it would refuse a tile. The tile
     * takes its place and gives it back (state and block entity data) when it expires.
     */
    private static boolean isHiddenMimic(BlockState state) {
        return state.getBlock() instanceof CsBossMimicBlock && !CsBossMimicBlock.isSolid(inBattle(state));
    }

    private static BlockState inBattle(BlockState state) {
        return state.getBlock() instanceof BattleReactiveBlock reactive ? reactive.battleState(state) : state;
    }

    /**
     * Collision-free decoration a floor hazard may stand in for a while (leaf pile, flowers, torch...):
     * the tile keeps it and puts it back when it expires ({@link CsBossTileBlockEntity}). Never a block
     * entity, an arena block, a fluid, or half of a two-block plant (the other half would pop off).
     */
    private static boolean isDecoration(ServerLevel level, BlockPos pos, BlockState s) {
        return !s.hasBlockEntity()
                && !(s.getBlock() instanceof BattleReactiveBlock)
                && s.getFluidState().isEmpty()
                && !s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && s.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Places {@code state} above a surface if the cell is replaceable or holds a decoration; a tile
     * placed this way remembers what it covered and restores it when it is removed.
     */
    public static boolean placeOnSurface(ServerLevel level, BlockPos surface, BlockState state) {
        BlockPos pos = surface.above();
        BlockState current = level.getBlockState(pos);
        boolean mimic = isHiddenMimic(current);
        if (current.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || !(mimic || current.isAir() || current.canBeReplaced() || isDecoration(level, pos, current))) {
            return false;
        }
        // Le mimic revient dans son état de combat, données comprises (bloc imité, mode).
        BlockState replaced = mimic ? inBattle(current) : current;
        CompoundTag data = null;
        if (mimic && level.getBlockEntity(pos) instanceof BlockEntity be) {
            data = be.saveWithoutMetadata(level.registryAccess());
        }
        level.setBlockAndUpdate(pos, state);
        if (!replaced.isAir() && level.getBlockEntity(pos) instanceof CsBossTileBlockEntity tile) {
            tile.setReplaced(replaced, data);
        }
        return true;
    }
}
