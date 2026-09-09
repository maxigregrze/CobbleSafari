package maxigregrze.cobblesafari.warpride.logic;

import java.util.List;
import java.util.Map;

/**
 * One Ultra Space destination. {@code typeId} is stable and unique ({@code >= 1}) and is what the
 * sub-instance record stores; {@code weight} is its share of a portal roll; {@code tint} is ARGB for
 * the portal sprite; {@code encounters} maps rank 0..3 to flat {@code "species form=x"} strings.
 *
 * <p>{@code destinationId} is the human name, derived by the loader from the file name rather than
 * authored (action plan 184, Decision section 12.3): {@code typeId} is already the machine identity
 * and the file name is already the human one, so a {@code name} field would only be a second source
 * of truth to keep in sync. It exists for the server log, and reaches no client.</p>
 */
public record WarpRideDestination(
        String destinationId,
        int typeId,
        int weight,
        String structurePool,
        int tint,
        String dimension,
        String biome,
        Map<Integer, List<String>> encounters
) {

    /** Id of the registered sub-dungeon hosting this destination. */
    public String dungeonId() {
        return hasOwnDimension()
                ? dimension.substring(dimension.indexOf(':') + 1)
                : SHARED_DUNGEON_ID;
    }

    /** @return true when this destination generates in its own dimension rather than the shared one. */
    public boolean hasOwnDimension() {
        return dimension != null && !dimension.isBlank();
    }

    /**
     * Biome to stamp on the footprint, or {@code null}. Always {@code null} for a destination that
     * owns its dimension: there the biome comes from the dimension's own generator, and stamping
     * one over the structure would only fight it.
     */
    public String effectiveBiome() {
        return hasOwnDimension() ? null : biome;
    }

    public List<String> encountersForRank(int rank) {
        List<String> list = encounters.get(rank);
        return list == null ? List.of() : list;
    }

    /** The shared sub-dimension, used by every destination that does not name one. */
    public static final String SHARED_DUNGEON_ID = "dungeon_ultraspace";
}
