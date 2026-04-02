package maxigregrze.cobblesafari.item.hyperberries;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.Pokemon;

import maxigregrze.cobblesafari.item.RandomizerStatUtils;
import maxigregrze.cobblesafari.item.redchainrandom.PokemonModifierItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class HyperBerryEVItem extends PokemonModifierItem {
    private final Stat stat;

    public HyperBerryEVItem(Properties properties, String itemId, Stat stat) {
        super(properties, itemId, false);
        this.stat = stat;
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        EVs evs = pokemon.getEvs();
        int currentStat = evs.get(stat);
        int total = 0;
        for (Stat loopStat : RandomizerStatUtils.SIX_STATS) {
            total += evs.get(loopStat);
        }

        if (currentStat >= EVs.MAX_STAT_VALUE || total >= EVs.MAX_TOTAL_VALUE) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.no_effect"));
            return false;
        }

        int maxIncrease = Math.min(EVs.MAX_STAT_VALUE - currentStat, EVs.MAX_TOTAL_VALUE - total);
        if (maxIncrease <= 0) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.no_effect"));
            return false;
        }

        evs.set(stat, currentStat + maxIncrease);
        return true;
    }
}
