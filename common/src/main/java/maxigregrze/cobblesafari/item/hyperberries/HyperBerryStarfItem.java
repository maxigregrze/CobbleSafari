package maxigregrze.cobblesafari.item.hyperberries;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.item.RandomizerStatUtils;
import maxigregrze.cobblesafari.item.redchainrandom.PokemonModifierItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class HyperBerryStarfItem extends PokemonModifierItem {

    private static final int TARGET_IV = 24;

    public HyperBerryStarfItem(Properties properties, String itemId) {
        super(properties, itemId, false);
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        boolean changed = false;
        int maxFriendship = Cobblemon.INSTANCE.getConfig().getMaxPokemonFriendship();
        if (pokemon.getFriendship() != maxFriendship) {
            pokemon.setFriendship(maxFriendship, true);
            changed = true;
        }
        IVs ivs = pokemon.getIvs();
        for (Stat s : RandomizerStatUtils.SIX_STATS) {
            if (ivs.get(s) != TARGET_IV) {
                ivs.set(s, TARGET_IV);
                changed = true;
            }
        }
        if (!changed) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.no_effect"));
            return false;
        }
        pokemon.feedPokemon(1, true);
        return true;
    }
}
