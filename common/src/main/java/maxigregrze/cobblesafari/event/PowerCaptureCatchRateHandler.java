package maxigregrze.cobblesafari.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kotlin.Unit;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.server.level.ServerPlayer;

public class PowerCaptureCatchRateHandler {

    private PowerCaptureCatchRateHandler() {}

    public static void register() {
        CobblemonEvents.POKEMON_CATCH_RATE.subscribe(Priority.NORMAL, event -> {
            if (!(event.getThrower() instanceof ServerPlayer player)) {
                return Unit.INSTANCE;
            }
            PokemonEntity pe = event.getPokemonEntity();
            if (!pe.getPokemon().isWild()) {
                return Unit.INSTANCE;
            }
            Integer capLevel = findCaptureLevel(player, pe.getPokemon());
            if (capLevel == null) {
                return Unit.INSTANCE;
            }
            float mult = switch (capLevel) {
                case 1 -> SpawnBoostConfig.data.effectSettings.capturePowerLevel1Multiplier;
                case 2 -> SpawnBoostConfig.data.effectSettings.capturePowerLevel2Multiplier;
                case 3 -> SpawnBoostConfig.data.effectSettings.capturePowerLevel3Multiplier;
                default -> 1.0f;
            };
            event.setCatchRate(event.getCatchRate() * mult);
            return Unit.INSTANCE;
        });
    }

    private static Integer findCaptureLevel(ServerPlayer player, com.cobblemon.mod.common.pokemon.Pokemon pokemon) {
        for (int lv = 3; lv >= 1; lv--) {
            for (int vi = 0; vi < PowerVariantRegistry.ELEMENTAL_COUNT; vi++) {
                if (player.hasEffect(ModPowerEffects.capture(vi, lv))
                        && PowerVariantRegistry.pokemonHasVariantType(pokemon, vi)) {
                    return lv;
                }
            }
            if (player.hasEffect(ModPowerEffects.capture(PowerVariantRegistry.INDEX_ALL, lv))
                    && PowerVariantRegistry.pokemonHasVariantType(pokemon, PowerVariantRegistry.INDEX_ALL)) {
                return lv;
            }
        }
        return null;
    }
}
