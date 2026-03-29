package maxigregrze.cobblesafari.item;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ThreadLocalRandom;

public class RedChainRandomEVItem extends PokemonModifierItem {
    public RedChainRandomEVItem(Properties properties, String itemId) {
        super(properties, itemId, true, 'E');
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        EVs evs = pokemon.getEvs();
        int remaining = ThreadLocalRandom.current().nextInt(EVs.MAX_TOTAL_VALUE + 1);

        Stat[] stats = RandomizerStatUtils.SIX_STATS;
        for (int i = 0; i < stats.length; i++) {
            int remainingStats = stats.length - i - 1;
            int maxForCurrent = Math.min(EVs.MAX_STAT_VALUE, remaining);
            int minForCurrent = Math.max(0, remaining - (remainingStats * EVs.MAX_STAT_VALUE));
            int value = (i == stats.length - 1)
                    ? remaining
                    : ThreadLocalRandom.current().nextInt(minForCurrent, maxForCurrent + 1);
            evs.set(stats[i], value);
            remaining -= value;
        }
        return true;
    }
}
