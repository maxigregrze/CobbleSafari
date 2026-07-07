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

    /**
     * Only capture bounds while a CobbleSafari placement is running on this thread. Without this guard the
     * jigsaw mixin (which targets the generic {@code JigsawPlacement.generateJigsaw}) would record — and never
     * consume — a {@link BoundingBox} for every vanilla structure (village, trial chamber, …) generated over
     * the server's uptime, leaking one entry each. See action plan 145 (B4).
     */
    private static final ThreadLocal<Boolean> CAPTURING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DungeonStructureBounds() {}

    /** Marks the current thread as running a CobbleSafari jigsaw placement (enables bounds capture). */
    public static void beginCapture() {
        CAPTURING.set(Boolean.TRUE);
    }

    /** Clears the capture flag for the current thread. */
    public static void endCapture() {
        CAPTURING.remove();
    }

    /** @return true while a CobbleSafari placement is active on this thread. */
    public static boolean isCapturing() {
        return Boolean.TRUE.equals(CAPTURING.get());
    }

    /** Records the assembled bounding box for the structure whose start piece is at {@code start}. */
    public static void record(BlockPos start, BoundingBox box) {
        if (isCapturing() && start != null && box != null) {
            CAPTURED.put(start.immutable(), box);
        }
    }

    /** Returns and removes the captured bounding box for {@code start}, or {@code null} if none. */
    public static BoundingBox consume(BlockPos start) {
        return start == null ? null : CAPTURED.remove(start.immutable());
    }
}
