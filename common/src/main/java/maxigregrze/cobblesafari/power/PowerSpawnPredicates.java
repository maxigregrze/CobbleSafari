package maxigregrze.cobblesafari.power;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;

public final class PowerSpawnPredicates {

    private PowerSpawnPredicates() {}

    public static boolean detailMatchesVariant(PokemonSpawnDetail detail, int variantIndex) {
        if (variantIndex == PowerVariantRegistry.INDEX_ALL) {
            return true;
        }
        var props = detail.getPokemon();
        String speciesId = props.getSpecies();
        if (speciesId == null) {
            return false;
        }
        ResourceLocation identifier = speciesId.indexOf(':') >= 0
                ? ResourceLocation.parse(speciesId)
                : ResourceLocation.fromNamespaceAndPath("cobblemon", speciesId);
        Species species = PokemonSpecies.getByIdentifier(identifier);
        if (species == null) {
            return false;
        }
        FormData form = species.getForm(props.getAspects());
        ElementalType target = PowerVariantRegistry.elementalType(variantIndex);
        if (target == null) {
            return false;
        }
        for (ElementalType t : form.getTypes()) {
            if (t.equals(target)) {
                return true;
            }
        }
        return false;
    }
}
