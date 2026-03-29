package maxigregrze.cobblesafari.item;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.config.RandomizerItemsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RedChainRandomBallItem extends PokemonModifierItem {
    public RedChainRandomBallItem(Properties properties, String itemId) {
        super(properties, itemId, true, 'B');
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        List<RandomizerItemsConfig.WeightedPokeBall> weighted = RandomizerItemsConfig.getWeightedPokeBalls();
        if (weighted.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.cobblesafari.randomizer.no_pokeballs_configured"));
            return false;
        }

        int totalWeight = 0;
        for (RandomizerItemsConfig.WeightedPokeBall entry : weighted) {
            totalWeight += Math.max(0, entry.weight());
        }
        if (totalWeight <= 0) {
            player.sendSystemMessage(Component.translatable("message.cobblesafari.randomizer.no_pokeballs_configured"));
            return false;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        PokeBall selected = null;
        for (RandomizerItemsConfig.WeightedPokeBall entry : weighted) {
            roll -= entry.weight();
            if (roll < 0) {
                selected = entry.pokeBall();
                break;
            }
        }
        if (selected == null) {
            return false;
        }

        pokemon.setCaughtBall(selected);
        return true;
    }
}
