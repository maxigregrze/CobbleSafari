package maxigregrze.cobblesafari.csboss.attack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Registry of attack patterns. For now only {@code test} is implemented;
 * the 18 typed {@code base_<type>_1} attacks inherit from it (same class).
 */
public final class CsBossAttackRegistry {

    /** The 18 base Cobblemon types. */
    public static final List<String> BASE_TYPES = List.of(
            "normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison", "ground",
            "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy");

    public static final String TEST = "test";

    private static final Map<String, Supplier<CsBossAttack>> FACTORIES = new TreeMap<>();
    /** Cobblemon type → default attack pool. */
    private static final Map<String, List<String>> TYPE_POOLS = new HashMap<>();

    private CsBossAttackRegistry() {}

    public static void registerDefaults() {
        FACTORIES.clear();
        TYPE_POOLS.clear();

        register(TEST, () -> new CsBossTestAttack(TEST));
        for (String type : BASE_TYPES) {
            String id = "base_" + type + "_1";
            register(id, () -> new CsBossTestAttack(id)); // inherits test by default
            TYPE_POOLS.put(type, List.of(id));
        }

        // Real patterns — re-registered after the loop (last register wins).
        register("base_electric_1", () -> new ElectricVoltorbAttack("base_electric_1"));
        register("base_fire_1", () -> new FireShadowAttack("base_fire_1"));
        register("base_rock_1", () -> MeteorShowerAttack.rock("base_rock_1"));
        register("base_dragon_1", () -> MeteorShowerAttack.draco("base_dragon_1"));
        // New attack (not in a TYPE_POOL: rollable via explicit moveSet or ALLMOVES).
        register("distortion_1", () -> new DistortionStemAttack("distortion_1"));

        // Patterns from batch 2.
        register("base_ghost_1", () -> new GhostShadowAttack("base_ghost_1"));
        register("base_electric_2", () -> new ElectricFieldAttack("base_electric_2"));

        // Patterns from batch 3 — ring attacks.
        register("base_rock_2", () -> new RockRingAttack("base_rock_2"));
        register("base_electric_3", () -> new ElectricRingAttack("base_electric_3"));

        // Patterns from batch 4 — water.
        register("base_water_1", () -> new WaterShadowAttack("base_water_1"));
        register("base_water_2", () -> new WaterWaveAttack("base_water_2"));

        // Patterns from batch 5 — distortion, rock, ground.
        register("distortion_2", () -> new DistortionWalkAttack("distortion_2"));
        register("distortion_3", () -> new DistortionFieldAttack("distortion_3"));
        register("distortion_4", () -> new DistortionOrbAttack("distortion_4"));
        register("base_rock_3", () -> new RockHeadThrowAttack("base_rock_3"));
        register("base_rock_4", () -> new RockFieldAttack("base_rock_4"));
        register("base_ground_1", () -> new GroundShadowAttack("base_ground_1"));

        // New patterns (ghost / dragon batch).
        register("base_ghost_2", () -> new GhostCrossAttack("base_ghost_2"));
        register("base_ghost_3", () -> new GhostJumpscareAttack("base_ghost_3"));
        register("base_ghost_4", () -> new GhostShadowballAttack("base_ghost_4"));
        register("base_dragon_2", () -> new DragonBeamAttack("base_dragon_2"));
        register("base_dragon_3", () -> new DracoMeteorFieldAttack("base_dragon_3"));

        // Distortion red-chain volley (not in a TYPE_POOL: explicit moveSet or ALLMOVES).
        register("distortion_5", () -> new DistortionRedChainAttack("distortion_5"));

        // Ground shockwave rings.
        register("base_water_3", () -> GroundWaveAttack.water("base_water_3"));
        register("base_poison_1", () -> GroundWaveAttack.poison("base_poison_1"));
        register("base_steel_1", () -> GroundWaveAttack.steel("base_steel_1"));
        register("base_normal_1", () -> GroundWaveAttack.normal("base_normal_1"));

        // Ground eruption + pile barrages.
        register("base_ground_2", () -> new GroundEruptionAttack("base_ground_2"));
        register("base_poison_2", () -> new PoisonPileBarrageAttack("base_poison_2"));
        register("base_ground_3", () -> new MudPileFieldAttack("base_ground_3"));

        TYPE_POOLS.put("ghost", List.of("base_ghost_1", "base_ghost_2", "base_ghost_3", "base_ghost_4"));
        TYPE_POOLS.put("rock", List.of("base_rock_1", "base_rock_2", "base_rock_3", "base_rock_4"));
        TYPE_POOLS.put("electric", List.of("base_electric_1", "base_electric_2", "base_electric_3"));
        TYPE_POOLS.put("water", List.of("base_water_1", "base_water_2", "base_water_3"));
        TYPE_POOLS.put("ground", List.of("base_ground_1", "base_ground_2", "base_ground_3"));
        TYPE_POOLS.put("dragon", List.of("base_dragon_1", "base_dragon_2", "base_dragon_3"));
        TYPE_POOLS.put("poison", List.of("base_poison_1", "base_poison_2"));
        TYPE_POOLS.put("steel", List.of("base_steel_1"));
        TYPE_POOLS.put("normal", List.of("base_normal_1"));
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

    /** Default attack pool for a Cobblemon type (empty if unknown). */
    public static List<String> poolForType(String type) {
        return TYPE_POOLS.getOrDefault(type.toLowerCase(Locale.ROOT), List.of());
    }
}
