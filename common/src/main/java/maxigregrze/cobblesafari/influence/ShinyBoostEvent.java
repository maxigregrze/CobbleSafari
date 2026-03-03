package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.ShinyChanceCalculationEvent;
import kotlin.Unit;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModEffects;

public class ShinyBoostEvent {

    private ShinyBoostEvent() {}

    public static void register() {
        CobblemonEvents.SHINY_CHANCE_CALCULATION.subscribe(Priority.NORMAL, event -> {
            handle(event);
            return Unit.INSTANCE;
        });
    }

    private static void handle(ShinyChanceCalculationEvent event) {
        event.addModificationFunction((currentRate, player, pokemon) -> {
            if (player == null) return currentRate;

            int amplifier = 0;
            float multiplier = 1.0f;

            if (player.hasEffect(ModEffects.ULTRA_SHINY_BOOST.holder)) {
                amplifier = player.getEffect(ModEffects.ULTRA_SHINY_BOOST.holder).getAmplifier();
                multiplier = SpawnBoostConfig.data.effectSettings.ultraShinyBoostMultiplier;
            } else if (player.hasEffect(ModEffects.GREAT_SHINY_BOOST.holder)) {
                amplifier = player.getEffect(ModEffects.GREAT_SHINY_BOOST.holder).getAmplifier();
                multiplier = SpawnBoostConfig.data.effectSettings.superShinyBoostMultiplier;
            } else if (player.hasEffect(ModEffects.SHINY_BOOST.holder)) {
                amplifier = player.getEffect(ModEffects.SHINY_BOOST.holder).getAmplifier();
                multiplier = SpawnBoostConfig.data.effectSettings.shinyBoostMultiplier;
            }

            if (multiplier > 1.0f) {
                float effectiveMultiplier = multiplier * (amplifier + 1);
                return currentRate / effectiveMultiplier;
            }

            return currentRate;
        });
    }
}
