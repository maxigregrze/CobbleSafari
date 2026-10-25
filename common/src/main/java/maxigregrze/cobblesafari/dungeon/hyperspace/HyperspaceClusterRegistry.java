package maxigregrze.cobblesafari.dungeon.hyperspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Star → type → clusters, filled by {@link HyperspaceClusterDataLoader}. */
public final class HyperspaceClusterRegistry {

    private static final Map<Integer, Map<String, List<HyperspaceCluster>>> CLUSTERS = new TreeMap<>();

    private HyperspaceClusterRegistry() {}

    static void clear() {
        CLUSTERS.clear();
    }

    static void register(HyperspaceCluster cluster) {
        CLUSTERS.computeIfAbsent(cluster.star(), k -> new TreeMap<>())
                .computeIfAbsent(cluster.type(), k -> new ArrayList<>())
                .add(cluster);
    }

    /** Types that have at least one cluster at this star, in a stable (alphabetical) order. */
    public static List<String> typesAt(int star) {
        Map<String, List<HyperspaceCluster>> byType = CLUSTERS.get(star);
        return byType == null ? List.of() : List.copyOf(byType.keySet());
    }

    public static List<HyperspaceCluster> clusters(int star, String type) {
        Map<String, List<HyperspaceCluster>> byType = CLUSTERS.get(star);
        List<HyperspaceCluster> list = byType == null ? null : byType.get(type);
        return list == null ? List.of() : List.copyOf(list);
    }

    public static boolean isEmpty() {
        return CLUSTERS.isEmpty();
    }
}
