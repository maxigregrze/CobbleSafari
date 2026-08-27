package maxigregrze.cobblesafari.warpride.logic;

import java.util.List;
import java.util.Map;

/**
 * One Ultra Space destination. {@code typeId} is stable and unique ({@code >= 1}) and is what the
 * sub-instance record stores; {@code weight} is its share of a portal roll; {@code tint} is ARGB for
 * the portal sprite; {@code encounters} maps rank 0..3 to flat {@code "species form=x"} strings.
 */
public record WarpRideDestination(
        int typeId,
        int weight,
        String structurePool,
        int tint,
        Map<Integer, List<String>> encounters
) {

    public List<String> encountersForRank(int rank) {
        List<String> list = encounters.get(rank);
        return list == null ? List.of() : list;
    }
}
