package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;

/**
 * Helpers for reasoning about Cobblemon {@link SpawnDetail}s.
 *
 * <p>Cobblemon 1.8 ships herd spawns as {@code PokemonHerdSpawnDetail}, which extends
 * {@link SpawnDetail} directly rather than {@code PokemonSpawnDetail}. An
 * {@code instanceof PokemonSpawnDetail} test therefore lets every herd through, which silently
 * disabled Repel and the boss-arena suppression for the 704 herd files 1.8.0 ships. Cobblemon
 * itself never uses {@code instanceof} for this; it consults the registered type list, which
 * holds both {@code "pokemon"} and {@code "pokemon-herd"}.
 */
public final class SpawnDetails {

    private SpawnDetails() {}

    /** Whether the detail spawns Pokémon at all, herds included. */
    public static boolean isPokemonSpawn(SpawnDetail detail) {
        return SpawnDetail.Companion.getPokemonTypes().contains(detail.getType());
    }
}
