package maxigregrze.cobblesafari.csmusic;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * In-memory registry of music loaded from datapacks.
 */
public final class CsMusicRegistry {
    private static final Map<String, CsMusicDefinition> BY_ID = new TreeMap<>();

    private CsMusicRegistry() {}

    public static void clear() {
        BY_ID.clear();
    }

    public static void register(CsMusicDefinition def) {
        BY_ID.put(def.id(), def);
    }

    public static boolean has(String id) {
        return id != null && BY_ID.containsKey(id);
    }

    public static Optional<CsMusicDefinition> get(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(BY_ID.get(id));
    }

    public static Map<String, CsMusicDefinition> all() {
        return Collections.unmodifiableMap(BY_ID);
    }

    public static int size() {
        return BY_ID.size();
    }
}
