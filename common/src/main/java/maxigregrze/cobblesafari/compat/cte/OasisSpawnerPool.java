package maxigregrze.cobblesafari.compat.cte;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record OasisSpawnerPool(
        int playerDetectionRange,
        int spawnRange,
        int maxSimultaneousPokemon,
        int totalPokemonPerTrial,
        int spawnerCooldown,
        boolean doPokemonSpawnedGlow,
        String lootTable,
        String ominousLootTable,
        List<PokemonEntry> normalPokemon,
        List<PokemonEntry> ominousPokemon
) {

    public record PokemonEntry(
            String species,
            int weight,
            int level,
            @Nullable String form,
            @Nullable String gender,
            boolean isUncatchable,
            boolean mustBeDefeatedInBattle,
            boolean isAggressive
    ) {}

    private static final int DETECTION_RANGE = 8;
    private static final int SPAWN_RANGE = 6;
    private static final int MAX_SIMULTANEOUS = 2;
    private static final int TOTAL_PER_TRIAL = 3;
    private static final int COOLDOWN = 36000;
    private static final String LOOT = "cobblesafari:underground_oasis_small_spheres";
    private static final String OMINOUS_LOOT = "cobblesafari:underground_oasis_large_spheres";

    public static final Map<String, OasisSpawnerPool> POOLS = Map.of(

            "minecraft:rabbit", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                            new PokemonEntry("cacnea", 1, 20, null, null, false, true, false),
                            new PokemonEntry("maractus", 1, 20, null, null, false, true, false),
                            new PokemonEntry("yamask", 1, 20, null, null, false, true, false),
                            new PokemonEntry("sandshrew", 1, 20, null, null, false, true, false),
                            new PokemonEntry("golett", 1, 20, null, null, false, true, false),
                            new PokemonEntry("sableye", 1, 20, null, null, false, true, false),
                            new PokemonEntry("baltoy", 1, 20, null, null, false, true, false)
                    ),
                    List.of(
                        new PokemonEntry("cacturne", 1, 60, null, null, false, true, false),
                        new PokemonEntry("maractus", 1, 60, null, null, false, true, false),
                        new PokemonEntry("cofagrigus", 1, 60, null, null, false, true, false),
                        new PokemonEntry("sandslash", 1, 60, null, null, false, true, false),
                        new PokemonEntry("golurk", 1, 60, null, null, false, true, false),
                        new PokemonEntry("sableye", 1, 60, null, null, false, true, false),
                        new PokemonEntry("claydol", 1, 60, null, null, false, true, false)
                    )
            ),

            "minecraft:snow_golem", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                            new PokemonEntry("spheal", 1, 20, null, null, false, true, false),
                            new PokemonEntry("glalie", 1, 20, null, null, false, true, false),
                            new PokemonEntry("snorunt", 1, 20, null, null, false, true, false),
                            new PokemonEntry("cryogonal", 1, 20, null, null, false, true, false),
                            new PokemonEntry("eiscue", 1, 20, null, null, false, true, false),
                            new PokemonEntry("froslass", 1, 20, null, null, false, true, false),
                            new PokemonEntry("swinub", 1, 20, null, null, false, true, false)
                    ),
                    List.of(
                        new PokemonEntry("sealeo", 1, 60, null, null, false, true, false),
                        new PokemonEntry("glalie", 1, 60, null, null, false, true, false),
                        new PokemonEntry("cryogonal", 1, 60, null, null, false, true, false),
                        new PokemonEntry("eiscue", 1, 60, null, null, false, true, false),
                        new PokemonEntry("froslass", 1, 60, null, null, false, true, false),
                        new PokemonEntry("piloswine", 1, 60, null, null, false, true, false)
                    )
            ),

            "minecraft:tadpole", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                            new PokemonEntry("geodude alolan", 1, 20, null, null, false, true, false),
                            new PokemonEntry("roggenrola", 1, 20, null, null, false, true, false),
                            new PokemonEntry("joltik", 1, 20, null, null, false, true, false),
                            new PokemonEntry("electrike", 1, 20, null, null, false, true, false),
                            new PokemonEntry("beldum", 1, 20, null, null, false, true, false),
                            new PokemonEntry("ferroseed", 1, 20, null, null, false, true, false),
                            new PokemonEntry("magnemite", 1, 20, null, null, false, true, false)

                    ),
                    List.of(
                        new PokemonEntry("graveler alolan", 1, 60, null, null, false, true, false),
                        new PokemonEntry("boldore", 1, 60, null, null, false, true, false),
                        new PokemonEntry("galvantula", 1, 60, null, null, false, true, false),
                        new PokemonEntry("manectric", 1, 60, null, null, false, true, false),
                        new PokemonEntry("metang", 1, 60, null, null, false, true, false),
                        new PokemonEntry("ferrothorn", 1, 60, null, null, false, true, false),
                        new PokemonEntry("magneton", 1, 60, null, null, false, true, false)
                    )
            ),

            "minecraft:frog", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                            new PokemonEntry("tangela", 1, 20, null, null, false, true, false),
                            new PokemonEntry("paras", 1, 20, null, null, false, true, false),
                            new PokemonEntry("yanma", 1, 20, null, null, false, true, false),
                            new PokemonEntry("shuckle", 1, 20, null, null, false, true, false),
                            new PokemonEntry("carnivine", 1, 20, null, null, false, true, false),
                            new PokemonEntry("morelull", 1, 20, null, null, false, true, false),
                            new PokemonEntry("tropius", 1, 20, null, null, false, true, false)
                    ),
                    List.of(
                        new PokemonEntry("tangrowth", 1, 60, null, null, false, true, false),
                        new PokemonEntry("parasect", 1, 60, null, null, false, true, false),
                        new PokemonEntry("yanmega", 1, 60, null, null, false, true, false),
                        new PokemonEntry("shuckle", 1, 60, null, null, false, true, false),
                        new PokemonEntry("carnivine", 1, 60, null, null, false, true, false),
                        new PokemonEntry("shiinotic", 1, 60, null, null, false, true, false),
                        new PokemonEntry("tropius", 1, 60, null, null, false, true, false)
                    )
            ),

            "minecraft:sniffer", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                        new PokemonEntry("torkoal", 1, 20, null, null, false, true, false),
                        new PokemonEntry("slugma", 1, 20, null, null, false, true, false),
                        new PokemonEntry("turtonator", 1, 20, null, null, false, true, false),
                        new PokemonEntry("salandit", 1, 20, null, "male", false, true, false),
                        new PokemonEntry("growlithe hisuian", 1, 20, null, null, false, true, false),
                        new PokemonEntry("darumaka", 1, 20, null, null, false, true, false),
                        new PokemonEntry("houndour", 1, 20, null, null, false, true, false)
                    ),
                    List.of(
                        new PokemonEntry("torkoal", 1, 60, null, null, false, true, false),
                        new PokemonEntry("magcargo", 1, 60, null, null, false, true, false),
                        new PokemonEntry("turtonator", 1, 60, null, null, false, true, false),
                        new PokemonEntry("salazzle", 1, 60, null, null, false, true, false),
                        new PokemonEntry("arcanine hisuian", 1, 60, null, null, false, true, false),
                        new PokemonEntry("darmanitan", 1, 60, null, null, false, true, false),
                        new PokemonEntry("houndoom", 1, 60, null, null, false, true, false)
                    )
            ),

            "minecraft:glow_squid", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                        new PokemonEntry("wailmer", 1, 20, null, null, false, true, false),
                        new PokemonEntry("finneon", 1, 20, null, null, false, true, false),
                        new PokemonEntry("wiglett", 1, 20, null, null, false, true, false),
                        new PokemonEntry("corsola", 1, 20, null, null, false, true, false),
                        new PokemonEntry("frillish", 2, 20, null, null, false, true, false),
                        new PokemonEntry("mareanie", 1, 20, null, null, false, true, false)
                    ),
                    List.of(
                        new PokemonEntry("wailord", 1, 60, null, null, false, true, false),
                        new PokemonEntry("lumineon", 1, 60, null, null, false, true, false),
                        new PokemonEntry("wugtrio", 1, 60, null, null, false, true, false),
                        new PokemonEntry("corsola", 1, 60, null, null, false, true, false),
                        new PokemonEntry("jellicent", 2, 60, null, null, false, true, false),
                        new PokemonEntry("toxapex", 1, 60, null, null, false, true, false)
                    )
            )
    );
}
