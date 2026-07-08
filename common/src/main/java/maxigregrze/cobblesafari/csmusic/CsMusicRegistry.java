package maxigregrze.cobblesafari.csmusic;

import maxigregrze.cobblesafari.CobbleSafari;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * In-memory registry of music loaded from datapacks, indexed by id and by tag.
 */
public final class CsMusicRegistry {
    private static final Map<String, CsMusicDefinition> BY_ID = new TreeMap<>();
    private static final Map<String, List<String>> BY_TAG = new HashMap<>();

    private CsMusicRegistry() {}

    public static void clear() {
        BY_ID.clear();
        BY_TAG.clear();
    }

    public static void register(CsMusicDefinition def) {
        BY_ID.put(def.id(), def);
        for (String tag : def.tags()) {
            BY_TAG.computeIfAbsent(tag, k -> new ArrayList<>()).add(def.id());
        }
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

    /** All definitions carrying {@code tag} (order stable by id). */
    public static List<CsMusicDefinition> byTag(String tag) {
        List<String> ids = BY_TAG.get(tag);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<CsMusicDefinition> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            CsMusicDefinition def = BY_ID.get(id);
            if (def != null) {
                out.add(def);
            }
        }
        return out;
    }

    public static int size() {
        return BY_ID.size();
    }

    /** Warns on unknown parents and breaks/reports parent cycles. Call after a full (re)load. */
    public static void validateRelations() {
        for (CsMusicDefinition def : BY_ID.values()) {
            if (!def.hasParent()) {
                continue;
            }
            if (!BY_ID.containsKey(def.parent())) {
                CobbleSafari.LOGGER.warn("[CSMusic] {} references unknown parent '{}'", def.id(), def.parent());
                continue;
            }
            // Cycle guard: follow the parent chain with a visited set and a depth cap.
            Set<String> seen = new HashSet<>();
            String cursor = def.id();
            int depth = 0;
            while (cursor != null && depth++ < 64) {
                if (!seen.add(cursor)) {
                    CobbleSafari.LOGGER.warn("[CSMusic] parent cycle detected at '{}'", def.id());
                    break;
                }
                CsMusicDefinition node = BY_ID.get(cursor);
                cursor = node != null && node.hasParent() ? node.parent() : null;
            }
        }
    }
}
