package maxigregrze.cobblesafari.item.hyperberries;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;

import maxigregrze.cobblesafari.item.redchainrandom.PokemonModifierItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class HyperBerryFriendshipItem extends PokemonModifierItem {
    private static final int MAX_FRIENDSHIP = 255;

    public HyperBerryFriendshipItem(Properties properties, String itemId) {
        super(properties, itemId, false);
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        int current = pokemon.getFriendship();
        if (current >= MAX_FRIENDSHIP) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.no_effect"));
            return false;
        }
        int increase = MAX_FRIENDSHIP - current;
        boolean success = pokemon.incrementFriendship(increase, false);
        if (!success) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.no_effect"));
        }
        return success;
    }
}
