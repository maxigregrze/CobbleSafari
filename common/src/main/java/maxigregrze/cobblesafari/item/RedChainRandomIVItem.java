package maxigregrze.cobblesafari.item;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ThreadLocalRandom;

public class RedChainRandomIVItem extends PokemonModifierItem {
    public RedChainRandomIVItem(Properties properties, String itemId) {
        super(properties, itemId, true);
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        IVs ivs = pokemon.getIvs();
        for (Stat stat : RandomizerStatUtils.SIX_STATS) {
            ivs.set(stat, ThreadLocalRandom.current().nextInt(IVs.MAX_VALUE + 1));
        }
        return true;
    }
}
