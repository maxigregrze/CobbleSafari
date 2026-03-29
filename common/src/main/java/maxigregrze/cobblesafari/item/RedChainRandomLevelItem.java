package maxigregrze.cobblesafari.item;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ThreadLocalRandom;

public class RedChainRandomLevelItem extends PokemonModifierItem {
    public RedChainRandomLevelItem(Properties properties, String itemId) {
        super(properties, itemId, true, 'L');
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        pokemon.setLevel(ThreadLocalRandom.current().nextInt(1, 101));
        return true;
    }
}
