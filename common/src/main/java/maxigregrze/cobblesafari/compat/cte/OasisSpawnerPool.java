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
    private static final String LOOT = "cobblesafari:underground_oasis";
    private static final String OMINOUS_LOOT = "cobblesafari:underground_ominous_oasis";

    public static final Map<String, OasisSpawnerPool> POOLS = Map.of(

            "minecraft:rabbit", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                            new PokemonEntry("cacnea", 1, 15, null, null, false, true, false),
                            new PokemonEntry("gligar", 1, 15, null, null, false, true, false),
                            new PokemonEntry("numel", 1, 15, null, null, false, true, false)
                    ),
                    List.of(
                            new PokemonEntry("cacturne", 2, 30, null, null, false, true, true),
                            new PokemonEntry("gliscor", 2, 30, null, null, false, true, true),
                            new PokemonEntry("camerupt", 2, 30, null, null, false, true, true),
                            new PokemonEntry("camerupt mega", 1, 30, null, null, false, true, true)
                    )
            ),

            "minecraft:snow_golem", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                            new PokemonEntry("spheal", 1, 15, null, null, false, true, false),
                            new PokemonEntry("seel", 1, 15, null, null, false, true, false),
                            new PokemonEntry("snorunt", 1, 15, null, null, false, true, false)
                    ),
                    List.of(
                            new PokemonEntry("sealeo", 2, 30, null, null, false, true, true),
                            new PokemonEntry("walrein", 2, 30, null, null, false, true, true),
                            new PokemonEntry("froslass", 2, 30, null, null, false, true, true),
                            new PokemonEntry("glalie mega", 1, 30, null, null, false, true, true)
                    )
            ),

            "minecraft:tadpole", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                            new PokemonEntry("voltorb", 1, 15, null, null, false, true, false),
                            new PokemonEntry("geodude alolan", 1, 15, null, null, false, true, false),
                            new PokemonEntry("electrike", 1, 15, null, null, false, true, false)
                    ),
                    List.of(
                            new PokemonEntry("electrode", 2, 30, null, null, false, true, true),
                            new PokemonEntry("graveler alolan", 2, 30, null, null, false, true, true),
                            new PokemonEntry("manectric", 2, 30, null, null, false, true, true),
                            new PokemonEntry("manectric mega", 1, 30, null, null, false, true, true)
                    )
            ),

            "minecraft:frog", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                            new PokemonEntry("tangela", 1, 15, null, null, false, true, false),
                            new PokemonEntry("exeggcute", 1, 15, null, null, false, true, false),
                            new PokemonEntry("capsakid", 1, 15, null, null, false, true, false)
                    ),
                    List.of(
                            new PokemonEntry("tangrowth", 2, 30, null, null, false, true, true),
                            new PokemonEntry("exeggutor", 2, 30, null, null, false, true, true),
                            new PokemonEntry("scovillain", 2, 30, null, null, false, true, true),
                            new PokemonEntry("scovillain mega", 1, 30, null, null, false, true, true)
                    )
            ),

            "minecraft:sniffer", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                            new PokemonEntry("slugma", 1, 15, null, null, false, true, false),
                            new PokemonEntry("salandit", 1, 15, null, "male", false, true, false),
                            new PokemonEntry("houndour", 1, 15, null, null, false, true, false)
                    ),
                    List.of(
                            new PokemonEntry("magcargo", 2, 30, null, null, false, true, true),
                            new PokemonEntry("salazzle", 2, 30, null, null, false, true, true),
                            new PokemonEntry("houndoom", 2, 30, null, null, false, true, true),
                            new PokemonEntry("houndoom mega", 1, 30, null, null, false, true, true)
                    )
            ),

            "minecraft:glow_squid", new OasisSpawnerPool(
                    DETECTION_RANGE, SPAWN_RANGE, MAX_SIMULTANEOUS, TOTAL_PER_TRIAL, COOLDOWN, false,
                    LOOT, OMINOUS_LOOT,
                    List.of(
                            new PokemonEntry("finneon", 1, 15, null, null, false, true, false),
                            new PokemonEntry("mareanie", 1, 15, null, null, false, true, false),
                            new PokemonEntry("carvanha", 1, 15, null, null, false, true, false)
                    ),
                    List.of(
                            new PokemonEntry("lumineon", 2, 30, null, null, false, true, true),
                            new PokemonEntry("toxapex", 2, 30, null, null, false, true, true),
                            new PokemonEntry("sharpedo", 2, 30, null, null, false, true, true),
                            new PokemonEntry("sharpedo mega", 1, 30, null, null, false, true, true)
                    )
            )
    );
}
