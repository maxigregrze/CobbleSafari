package maxigregrze.cobblesafari.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory relay carrying the real {@link BoundingBox} of a jigsaw structure, captured while
 * {@code JigsawPlacement.generateJigsaw} assembles it (see {@code JigsawPlacementMixin}), to the
 * placement code that needs the exact extent to compute a precise dungeon clear region.
 *
 * <p>Keyed by the jigsaw start position. Records are written and consumed on the server thread during
 * a single, synchronous structure placement, so an entry is produced and read back within the same
 * call; {@link #consume(BlockPos)} removes it to avoid stale leftovers.
 */
public final class DungeonStructureBounds {

    private static final Map<BlockPos, BoundingBox> CAPTURED = new ConcurrentHashMap<>();

    private DungeonStructureBounds() {}

    /** Records the assembled bounding box for the structure whose start piece is at {@code start}. */
    public static void record(BlockPos start, BoundingBox box) {
        if (start != null && box != null) {
            CAPTURED.put(start.immutable(), box);
        }
    }

    /** Returns and removes the captured bounding box for {@code start}, or {@code null} if none. */
    public static BoundingBox consume(BlockPos start) {
        return start == null ? null : CAPTURED.remove(start.immutable());
    }
}
