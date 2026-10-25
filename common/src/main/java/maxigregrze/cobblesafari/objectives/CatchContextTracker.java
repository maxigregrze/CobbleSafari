package maxigregrze.cobblesafari.objectives;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kotlin.Unit;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers whether the ball that will capture a Pokémon hit it in or out of battle, for the
 * {@code task_catch_unaware} / {@code task_catch_battle} tasks (action plan 196, § 7.5).
 */
final class CatchContextTracker {

    /** An entry older than this is dropped: the capture it was waiting for never came. */
    private static final long MAX_AGE_TICKS = 1200;

    private record Hit(boolean inBattle, long gameTime) {}

    private static final Map<UUID, Hit> HITS = new ConcurrentHashMap<>();

    private CatchContextTracker() {}

    static void register() {
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(Priority.NORMAL, event -> {
            record(event.getPokemon());
            return Unit.INSTANCE;
        });
    }

    private static void record(PokemonEntity entity) {
        long now = entity.level().getGameTime();
        HITS.values().removeIf(hit -> now - hit.gameTime() > MAX_AGE_TICKS || hit.gameTime() > now);
        HITS.put(entity.getPokemon().getUuid(), new Hit(entity.isBattling(), now));
    }

    /** @return true / false for a ball hit in / out of battle, {@code null} when no hit was seen. */
    @Nullable
    static Boolean consume(UUID pokemonUuid) {
        Hit hit = HITS.remove(pokemonUuid);
        return hit == null ? null : hit.inBattle();
    }
}
