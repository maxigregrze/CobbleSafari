package maxigregrze.cobblesafari.item.hyperberries;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.item.redchainrandom.PokemonModifierItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class HyperBerryIVItem extends PokemonModifierItem {
    private static final int TARGET_IV = 31;

    private final Stat stat;

    public HyperBerryIVItem(Properties properties, String itemId, Stat stat) {
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
        IVs ivs = pokemon.getIvs();
        if (ivs.get(stat) != TARGET_IV) {
            ivs.set(stat, TARGET_IV);
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
