package maxigregrze.cobblesafari.item.redchainrandom;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.config.RandomizerItemsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ThreadLocalRandom;

public class RedChainRandomShinyItem extends PokemonModifierItem {
    public RedChainRandomShinyItem(Properties properties, String itemId) {
        super(properties, itemId, true, 'S');
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        if (player.isCreative()) {
            pokemon.setShiny(!pokemon.getShiny());
            return true;
        }
        if (pokemon.getShiny()) {
            pokemon.setShiny(false);
            return true;
        }
        int x = RandomizerItemsConfig.getRedChainRandomShinyRollMax();
        int roll = ThreadLocalRandom.current().nextInt(x + 1);
        if (roll == 0) {
            pokemon.setShiny(true);
            return true;
        }
        player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.no_effect"));
        return true;
    }
}
