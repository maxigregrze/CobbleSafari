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
            register(id, () -> new CsBossTestAttack(id)); // hérite de test par défaut
            TYPE_POOLS.put(type, List.of(id));
        }

        // Patterns réels (plan 107) — ré-enregistrés après la boucle (le dernier register gagne).
        register("base_electric_1", () -> new ElectricVoltorbAttack("base_electric_1"));
        register("base_fire_1", () -> new FireShadowAttack("base_fire_1"));
        register("base_rock_1", () -> MeteorShowerAttack.rock("base_rock_1"));
        register("base_dragon_1", () -> MeteorShowerAttack.draco("base_dragon_1"));
        // Attaque nouvelle (pas dans un TYPE_POOL : rollable via moveSet explicite ou ALLMOVES).
        register("distortion_1", () -> new DistortionStemAttack("distortion_1"));

        // Patterns du 2e lot (plan 108).
        register("base_ghost_1", () -> new GhostShadowAttack("base_ghost_1"));
        register("base_electric_2", () -> new ElectricFieldAttack("base_electric_2"));

        // Patterns du 3e lot (plan 109) — attaques en anneau.
        register("base_rock_2", () -> new RockRingAttack("base_rock_2"));
        register("base_electric_3", () -> new ElectricRingAttack("base_electric_3"));

        // Patterns du 4e lot (plan 110) — eau.
        register("base_water_1", () -> new WaterShadowAttack("base_water_1"));
        register("base_water_2", () -> new WaterWaveAttack("base_water_2"));

        // Patterns du 5e lot (plan 113) — distorsion, roche, sol.
        register("distortion_2", () -> new DistortionWalkAttack("distortion_2"));
        register("distortion_3", () -> new DistortionFieldAttack("distortion_3"));
        register("distortion_4", () -> new DistortionOrbAttack("distortion_4"));
        register("base_rock_3", () -> new RockHeadThrowAttack("base_rock_3"));
        register("base_rock_4", () -> new RockFieldAttack("base_rock_4"));
        register("base_ground_1", () -> new GroundShadowAttack("base_ground_1"));

        TYPE_POOLS.put("ghost", List.of("base_ghost_1"));
        TYPE_POOLS.put("rock", List.of("base_rock_1", "base_rock_2", "base_rock_3", "base_rock_4"));
        TYPE_POOLS.put("electric", List.of("base_electric_1", "base_electric_2", "base_electric_3"));
        TYPE_POOLS.put("water", List.of("base_water_1", "base_water_2"));
        TYPE_POOLS.put("ground", List.of("base_ground_1"));
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
