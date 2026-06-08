package maxigregrze.cobblesafari.csboss;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * In-memory registry of CSBosses loaded from datapacks (plan 100 § 2.1).
 * Indexed by direct id and by tag (uniform random selection).
 */
public final class CsBossRegistry {
    private static final Map<String, CsBossDefinition> BY_ID = new TreeMap<>();
    private static final Map<String, List<String>> BY_TAG = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private CsBossRegistry() {}

    public static void clear() {
        BY_ID.clear();
        BY_TAG.clear();
    }

    public static void register(CsBossDefinition def) {
        BY_ID.put(def.bossId(), def);
        for (String tag : def.tags()) {
            BY_TAG.computeIfAbsent(tag, t -> new ArrayList<>()).add(def.bossId());
        }
    }

    public static Optional<CsBossDefinition> get(String bossId) {
        return Optional.ofNullable(BY_ID.get(bossId));
    }

    public static boolean has(String bossId) {
        return BY_ID.containsKey(bossId);
    }

    /**
     * Resolves a reference: exact id first, otherwise uniform random pick from a tag.
     * Returns {@code null} if nothing matches.
     */
    public static CsBossDefinition resolve(String ref) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        CsBossDefinition direct = BY_ID.get(ref);
        if (direct != null) {
            return direct;
        }
        List<String> tagged = BY_TAG.get(ref);
        if (tagged == null || tagged.isEmpty()) {
            return null;
        }
        String chosen = tagged.get(RANDOM.nextInt(tagged.size()));
        return BY_ID.get(chosen);
    }

    public static List<String> allIds() {
        return new ArrayList<>(BY_ID.keySet());
    }

    public static Map<String, CsBossDefinition> all() {
        return Collections.unmodifiableMap(BY_ID);
    }

    public static int size() {
        return BY_ID.size();
    }
}
