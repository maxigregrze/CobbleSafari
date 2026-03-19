package maxigregrze.cobblesafari.item;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.world.entity.player.Player;

public class RedChainRandomShinyItem extends PokemonModifierItem {
    public RedChainRandomShinyItem(Properties properties, String itemId) {
        super(properties, itemId, true);
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        pokemon.setShiny(!pokemon.getShiny());
        return true;
    }
}
