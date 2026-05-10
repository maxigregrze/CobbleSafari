package maxigregrze.cobblesafari.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.drop.DropTable;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kotlin.Unit;
import kotlin.ranges.IntRange;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;

public class PowerSalvageLootHandler {

    private PowerSalvageLootHandler() {}

    public static void register() {
        CobblemonEvents.LOOT_DROPPED.subscribe(Priority.NORMAL, event -> {
            if (!(event.getEntity() instanceof PokemonEntity pe)) {
                return Unit.INSTANCE;
            }
            if (!pe.getPokemon().isWild()) {
                return Unit.INSTANCE;
            }
            var player = event.getPlayer();
            if (player == null) {
                return Unit.INSTANCE;
            }
            int extra = salvageExtraRolls(player, pe.getPokemon());
            if (extra <= 0) {
                return Unit.INSTANCE;
            }
            DropTable table = event.getTable();
            IntRange amount = table.getAmount();
            var pokemon = pe.getPokemon();
            for (int i = 0; i < extra; i++) {
                event.getDrops().addAll(table.getDrops(amount, pokemon));
            }
            return Unit.INSTANCE;
        });
    }

    private static int salvageExtraRolls(net.minecraft.server.level.ServerPlayer player, com.cobblemon.mod.common.pokemon.Pokemon pokemon) {
        for (int lv = 3; lv >= 1; lv--) {
            for (int vi = 0; vi < PowerVariantRegistry.ELEMENTAL_COUNT; vi++) {
                if (player.hasEffect(ModPowerEffects.salvage(vi, lv))
                        && PowerVariantRegistry.pokemonHasVariantType(pokemon, vi)) {
                    return switch (lv) {
                        case 1 -> SpawnBoostConfig.data.effectSettings.salvagePowerLevel1ExtraRolls;
                        case 2 -> SpawnBoostConfig.data.effectSettings.salvagePowerLevel2ExtraRolls;
                        case 3 -> SpawnBoostConfig.data.effectSettings.salvagePowerLevel3ExtraRolls;
                        default -> 0;
                    };
                }
            }
            if (player.hasEffect(ModPowerEffects.salvage(PowerVariantRegistry.INDEX_ALL, lv))
                    && PowerVariantRegistry.pokemonHasVariantType(pokemon, PowerVariantRegistry.INDEX_ALL)) {
                return switch (lv) {
                    case 1 -> SpawnBoostConfig.data.effectSettings.salvagePowerLevel1ExtraRolls;
                    case 2 -> SpawnBoostConfig.data.effectSettings.salvagePowerLevel2ExtraRolls;
                    case 3 -> SpawnBoostConfig.data.effectSettings.salvagePowerLevel3ExtraRolls;
                    default -> 0;
                };
            }
        }
        return 0;
    }
}
