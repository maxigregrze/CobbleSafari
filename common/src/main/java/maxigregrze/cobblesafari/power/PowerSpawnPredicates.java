package maxigregrze.cobblesafari.power;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.detail.PokemonHerdSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;

public final class PowerSpawnPredicates {

    private PowerSpawnPredicates() {}

    /**
     * Whether the spawn detail can produce a Pokémon of the given elemental variant.
     *
     * <p>Takes a plain {@link SpawnDetail} rather than a {@code PokemonSpawnDetail}: Cobblemon 1.8
     * herds are {@link PokemonHerdSpawnDetail}, which does not extend it. A herd matches when
     * <em>any</em> of its members does.
     */
    public static boolean detailMatchesVariant(SpawnDetail detail, int variantIndex) {
        if (variantIndex == PowerVariantRegistry.INDEX_ALL) {
            return true;
        }
        if (detail instanceof PokemonSpawnDetail pokemonDetail) {
            return propertiesMatchVariant(pokemonDetail.getPokemon(), variantIndex);
        }
        if (detail instanceof PokemonHerdSpawnDetail herdDetail) {
            for (PokemonHerdSpawnDetail.Herdable herdable : herdDetail.getHerdablePokemon()) {
                if (propertiesMatchVariant(herdable.getPokemon(), variantIndex)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean propertiesMatchVariant(PokemonProperties props, int variantIndex) {
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
