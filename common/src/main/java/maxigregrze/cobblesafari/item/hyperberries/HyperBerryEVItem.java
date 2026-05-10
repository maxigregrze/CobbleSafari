package maxigregrze.cobblesafari.item.hyperberries;

import com.cobblemon.mod.common.Cobblemon;
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
        boolean changed = false;
        if (pokemon.getFriendship() != 0) {
            pokemon.setFriendship(0, true);
            changed = true;
        }
        EVs evs = pokemon.getEvs();
        for (Stat s : RandomizerStatUtils.SIX_STATS) {
            if (s != stat && evs.get(s) != 0) {
                evs.set(s, 0);
                changed = true;
            }
        }
        if (evs.get(stat) != EVs.MAX_STAT_VALUE) {
            evs.set(stat, EVs.MAX_STAT_VALUE);
            changed = true;
        }
        if (!changed) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.no_effect"));
            return false;
        }
        pokemon.feedPokemon(3, true);
        return true;
    }
}
