package maxigregrze.cobblesafari.csboss.attack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Registre des patterns d'attaque (plan 100 § 12). Pour l'instant seul {@code test} est implémenté ;
 * les 18 attaques typées {@code base_<type>_1} en héritent (même classe).
 */
public final class CsBossAttackRegistry {

    /** Les 18 types de base Cobblemon. */
    public static final List<String> BASE_TYPES = List.of(
            "normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison", "ground",
            "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy");

    public static final String TEST = "test";

    private static final Map<String, Supplier<CsBossAttack>> FACTORIES = new TreeMap<>();
    /** type Cobblemon → pool d'attaques par défaut. */
    private static final Map<String, List<String>> TYPE_POOLS = new HashMap<>();

    private CsBossAttackRegistry() {}

    public static void registerDefaults() {
        FACTORIES.clear();
        TYPE_POOLS.clear();

        register(TEST, () -> new CsBossTestAttack(TEST));
        for (String type : BASE_TYPES) {
            String id = "base_" + type + "_1";
            register(id, () -> new CsBossTestAttack(id)); // hérite de test pour l'instant
            TYPE_POOLS.put(type, List.of(id));
        }
    }

    public static void register(String id, Supplier<CsBossAttack> factory) {
        FACTORIES.put(id, factory);
    }

    public static boolean has(String id) {
        return FACTORIES.containsKey(id);
    }

    public static CsBossAttack create(String id) {
        Supplier<CsBossAttack> f = FACTORIES.get(id);
        return f == null ? null : f.get();
    }

    public static List<String> allIds() {
        return new ArrayList<>(FACTORIES.keySet());
    }

    /** Pool d'attaques par défaut pour un type Cobblemon (vide si inconnu). */
    public static List<String> poolForType(String type) {
        return TYPE_POOLS.getOrDefault(type.toLowerCase(Locale.ROOT), List.of());
    }
}
