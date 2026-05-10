package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.ShinyChanceCalculationEvent;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModEffects;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

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

            int amp = 0;
            float multiplier = 1.0f;
            boolean applied = false;

            if (player.hasEffect(ModEffects.ULTRA_SHINY_BOOST.holder)) {
                amp = effectAmplifier(player, ModEffects.ULTRA_SHINY_BOOST.holder);
                multiplier = SpawnBoostConfig.data.effectSettings.ultraShinyBoostMultiplier;
                applied = true;
            } else if (player.hasEffect(ModEffects.GREAT_SHINY_BOOST.holder)) {
                amp = effectAmplifier(player, ModEffects.GREAT_SHINY_BOOST.holder);
                multiplier = SpawnBoostConfig.data.effectSettings.superShinyBoostMultiplier;
                applied = true;
            } else if (player.hasEffect(ModEffects.SHINY_BOOST.holder)) {
                amp = effectAmplifier(player, ModEffects.SHINY_BOOST.holder);
                multiplier = SpawnBoostConfig.data.effectSettings.shinyBoostMultiplier;
                applied = true;
            } else {
                Float sparkle = resolveSparklingMultiplier(player, pokemon);
                if (sparkle != null && sparkle > 1.0f) {
                    multiplier = sparkle;
                    amp = 0;
                    applied = true;
                }
            }

            if (applied && multiplier > 1.0f) {
                float effectiveMultiplier = multiplier * (amp + 1);
                return currentRate / effectiveMultiplier;
            }

            return currentRate;
        });
    }

    private static int effectAmplifier(ServerPlayer player, Holder<MobEffect> holder) {
        MobEffectInstance inst = player.getEffect(holder);
        return inst != null ? inst.getAmplifier() : 0;
    }

    private static Float resolveSparklingMultiplier(ServerPlayer player, Pokemon pokemon) {
        for (int level = 3; level >= 1; level--) {
            for (int vi = 0; vi < PowerVariantRegistry.VARIANT_COUNT; vi++) {
                if (!player.hasEffect(ModPowerEffects.sparkling(vi, level))) {
                    continue;
                }
                if (!PowerVariantRegistry.pokemonHasVariantType(pokemon, vi)) {
                    continue;
                }
                return switch (level) {
                    case 1 -> SpawnBoostConfig.data.effectSettings.shinyBoostMultiplier;
                    case 2 -> SpawnBoostConfig.data.effectSettings.superShinyBoostMultiplier;
                    case 3 -> SpawnBoostConfig.data.effectSettings.ultraShinyBoostMultiplier;
                    default -> 1.0f;
                };
            }
        }
        return null;
    }
}
