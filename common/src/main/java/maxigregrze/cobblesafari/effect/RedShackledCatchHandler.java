package maxigregrze.cobblesafari.effect;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokeball.catching.CaptureContext;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kotlin.Unit;
import maxigregrze.cobblesafari.CobbleSafari;

public final class RedShackledCatchHandler {

    private RedShackledCatchHandler() {}

    public static void register() {
        CobblemonEvents.POKE_BALL_CAPTURE_CALCULATED.subscribe(Priority.HIGH, event -> {
            PokemonEntity target = event.getPokemonEntity();
            if (!RedShackledEffects.isShackled(target)) {
                return Unit.INSTANCE;
            }
            if (!target.getPokemon().isWild()) {
                return Unit.INSTANCE;
            }

            event.setCaptureResult(new CaptureContext(1, true, true));
            return Unit.INSTANCE;
        });

        CobbleSafari.LOGGER.info("CobbleSafari >> Red Shackled catch handler registered!");
    }
}
