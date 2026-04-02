package maxigregrze.cobblesafari.item.hyperberries;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.IVs;
import com.cobblemon.mod.common.pokemon.Pokemon;

import maxigregrze.cobblesafari.item.redchainrandom.PokemonModifierItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class HyperBerryIVItem extends PokemonModifierItem {
    private final Stat stat;

    public HyperBerryIVItem(Properties properties, String itemId, Stat stat) {
        super(properties, itemId, false);
        this.stat = stat;
    }

    @Override
    protected boolean applyToPokemon(Player player, PokemonEntity pokemonEntity, Pokemon pokemon) {
        IVs ivs = pokemon.getIvs();
        int current = ivs.get(stat);
        if (current >= IVs.MAX_VALUE) {
            player.sendSystemMessage(Component.translatable("cobblesafari.randomizer.no_effect"));
            return false;
        }
        ivs.set(stat, IVs.MAX_VALUE);
        return true;
    }
}
