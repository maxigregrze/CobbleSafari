package maxigregrze.cobblesafari.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.Cobblemon;
import kotlin.Unit;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;

public class PowerFriendshipCatchHandler {

    private PowerFriendshipCatchHandler() {}

    public static void register() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            var player = event.getPlayer();
            int bonus = friendshipBonus(player);
            if (bonus <= 0) {
                return Unit.INSTANCE;
            }
            var pokemon = event.getPokemon();
            int max = Cobblemon.INSTANCE.getConfig().getMaxPokemonFriendship();
            int next = Math.min(max, pokemon.getFriendship() + bonus);
            pokemon.setFriendship(next, true);
            return Unit.INSTANCE;
        });
    }

    private static int friendshipBonus(net.minecraft.server.level.ServerPlayer player) {
        for (int lv = 3; lv >= 1; lv--) {
            if (player.hasEffect(ModPowerEffects.friendship(lv))) {
                return switch (lv) {
                    case 1 -> SpawnBoostConfig.data.effectSettings.friendshipPowerLevel1Bonus;
                    case 2 -> SpawnBoostConfig.data.effectSettings.friendshipPowerLevel2Bonus;
                    case 3 -> SpawnBoostConfig.data.effectSettings.friendshipPowerLevel3Bonus;
                    default -> 0;
                };
            }
        }
        return 0;
    }
}
