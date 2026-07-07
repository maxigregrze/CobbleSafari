package maxigregrze.cobblesafari.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live server-side index of currently loaded {@link AuspiciousPokeballGoldBlockEntity} positions, keyed by
 * dimension. Populated as the block entities load and cleared as they unload/break, so the dimensional
 * objectives redeem can pick a nearby golden pokeball via an O(index) lookup instead of scanning hundreds of
 * chunks each completion (B5). Bounded by the number of <em>loaded</em> golden pokeballs and self-cleaning.
 */
public final class AuspiciousPokeballGoldIndex {

    private static final Map<ResourceLocation, Set<BlockPos>> BY_DIMENSION = new ConcurrentHashMap<>();

    private AuspiciousPokeballGoldIndex() {}

    public static void register(ResourceLocation dimension, BlockPos pos) {
        BY_DIMENSION.computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    public static void unregister(ResourceLocation dimension, BlockPos pos) {
        Set<BlockPos> set = BY_DIMENSION.get(dimension);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) {
                BY_DIMENSION.remove(dimension);
            }
        }
    }

    /** Snapshot of the known golden-pokeball positions in {@code dimension} (empty if none). */
    public static List<BlockPos> positionsIn(ResourceLocation dimension) {
        Set<BlockPos> set = BY_DIMENSION.get(dimension);
        return set == null ? List.of() : List.copyOf(set);
    }
}
