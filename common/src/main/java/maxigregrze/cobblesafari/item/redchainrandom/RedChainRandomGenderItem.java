package maxigregrze.cobblesafari.item.redchainrandom;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class RedChainRandomGenderItem extends PokemonModifierItem {
    public RedChainRandomGenderItem(Properties properties, String itemId) {
        super(properties, itemId, true, 'G');
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        Gender current = pokemon.getGender();
        if (current == Gender.GENDERLESS) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.gender_unavailable"));
            return false;
        }

        Gender target = current == Gender.MALE ? Gender.FEMALE : Gender.MALE;
        pokemon.setGender(target);
        if (pokemon.getGender() != target) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.gender_unavailable"));
            return false;
        }
        return true;
    }
}
