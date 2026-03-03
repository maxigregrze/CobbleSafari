package maxigregrze.cobblesafari.safari;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kotlin.Unit;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SafariConfig;

import java.util.UUID;

public class SafariCatchEventHandler {

    private static final float SECONDS_BEFORE_SHAKE = 1.0f;
    private static final float SECONDS_BETWEEN_SHAKES = 1.25f;

    private SafariCatchEventHandler() {}

    public static void register() {
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(Priority.NORMAL, event -> {
            handlePokeballHit(event.getPokemon());
            return Unit.INSTANCE;
        });

        CobblemonEvents.POKEMON_CATCH_RATE.subscribe(Priority.NORMAL, event -> {
            applyCatchRateModifiers(event);
            return Unit.INSTANCE;
        });

        CobblemonEvents.POKE_BALL_CAPTURE_CALCULATED.subscribe(Priority.NORMAL, event -> {
            handleCaptureResult(event);
            return Unit.INSTANCE;
        });

        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            var entity = event.getPokemon().getEntity();
            if (entity != null) {
                SafariStateManager.remove(entity.getUUID());
            }
            return Unit.INSTANCE;
        });

        CobbleSafari.LOGGER.info("CobbleSafari >> Safari catch event handlers registered!");
    }

    private static void handlePokeballHit(PokemonEntity pokemonEntity) {
        if (!SafariStateManager.isInSafariDimension(pokemonEntity)) return;
        SafariPokemonState state = SafariStateManager.getState(pokemonEntity.getUUID());
        if (state != null && state.isFleeing()) {
            SafariStateManager.pauseFleeTimer(pokemonEntity.getUUID());
        }
    }

    private static void applyCatchRateModifiers(com.cobblemon.mod.common.api.events.pokeball.PokemonCatchRateEvent event) {
        var pokemonEntity = event.getPokemonEntity();
        if (!SafariStateManager.isInSafariDimension(pokemonEntity)) return;
        
        SafariPokemonState state = SafariStateManager.getState(pokemonEntity.getUUID());
        float multiplier = (state != null) ? state.getCatchRateMultiplier() : 1.0f;
        if (pokemonEntity.getPokemon().getShiny()) {
            multiplier *= SafariConfig.getShinyCatchMultiplier();
        }
        event.setCatchRate(event.getCatchRate() * multiplier);
    }

    private static void handleCaptureResult(com.cobblemon.mod.common.api.events.pokeball.PokeBallCaptureCalculatedEvent event) {
        var pokemonEntity = event.getPokemonEntity();
        if (!SafariStateManager.isInSafariDimension(pokemonEntity)) return;
        
        var entityId = pokemonEntity.getUUID();
        SafariPokemonState state = SafariStateManager.getOrCreate(entityId);

        if (event.getCaptureResult().isSuccessfulCapture()) {
            SafariStateManager.remove(entityId);
        } else if (state.isFleeing()) {
            scheduleFleeAfterBreakFree(pokemonEntity, state, event.getCaptureResult().getNumberOfShakes());
        } else {
            int delayTicks = computeBreakFreeDelay(event.getCaptureResult().getNumberOfShakes());
            scheduleFleeRollAfterShakes(pokemonEntity, delayTicks);
        }
    }

    private static void scheduleFleeAfterBreakFree(PokemonEntity pokemonEntity, SafariPokemonState state, int shakes) {
        var entityId = pokemonEntity.getUUID();
        int newToken = state.getFleeToken();
        int delayTicks = computeBreakFreeDelay(shakes);
        SafariStateManager.scheduleTickDelay(delayTicks, () -> {
            if (!pokemonEntity.isAlive()) {
                SafariStateManager.remove(entityId);
                return;
            }
            SafariPokemonState current = SafariStateManager.getState(entityId);
            if (current == null || current.getFleeToken() != newToken) return;
            SafariStateManager.triggerImmediateDespawn(pokemonEntity);
        });
    }

    private static int computeBreakFreeDelay(int numberOfShakes) {
        int iterations = numberOfShakes + 1;
        float totalSeconds = SECONDS_BEFORE_SHAKE + iterations * SECONDS_BETWEEN_SHAKES;
        return (int) (totalSeconds * 20);
    }

    private static void scheduleFleeRollAfterShakes(PokemonEntity pokemonEntity, int delayTicks) {
        UUID entityId = pokemonEntity.getUUID();
        SafariStateManager.scheduleTickDelay(delayTicks, () -> {
            if (!pokemonEntity.isAlive()) {
                SafariStateManager.remove(entityId);
                return;
            }
            if (!SafariStateManager.isInSafariDimension(pokemonEntity)) return;
            
            SafariPokemonState state = SafariStateManager.getOrCreate(entityId);
            if (state.isFleeing()) return;

            SafariStateManager.tryFlee(pokemonEntity);
        });
    }
}
