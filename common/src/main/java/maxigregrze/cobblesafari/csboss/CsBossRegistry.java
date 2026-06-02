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
 * Registre en mémoire des CSBosses chargés depuis les datapacks (plan 100 § 2.1).
 * Indexé par id direct et par tag (sélection équiprobable).
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
     * Résout une référence : id exact prioritaire, sinon tirage équiprobable parmi un tag.
     * Retourne {@code null} si rien ne correspond.
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
