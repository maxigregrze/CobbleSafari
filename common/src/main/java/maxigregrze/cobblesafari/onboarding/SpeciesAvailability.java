package maxigregrze.cobblesafari.onboarding;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import com.cobblemon.mod.common.pokemon.Species;
import maxigregrze.cobblesafari.config.MiscConfig;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * Whether a species may be handed out, given what Cobblemon implements and which species addons
 * the admin declared in the onboarding screen (action plan 198, section 9).
 */
public final class SpeciesAvailability {

    /** The onboarding checkbox that covers a species Cobblemon does not implement. */
    public enum Category { LEGENDARY_OR_MYTHICAL, ULTRA_BEAST, PARADOX, REGULAR }

    private SpeciesAvailability() {}

    /**
     * Whether a property line may be handed out: its species is implemented by Cobblemon, or the
     * admin declared an addon for its category. A line whose species cannot be resolved is let
     * through: the caller's own validation reports it.
     */
    public static boolean isAllowed(@Nullable String spec) {
        Species species = spec == null ? null : resolveSpecies(spec);
        if (species == null || species.getImplemented()) {
            return true;
        }
        MiscConfig.OnboardingState flags = MiscConfig.getOnboardingState();
        return switch (category(species)) {
            case LEGENDARY_OR_MYTHICAL -> flags.legendariesAndMythicals;
            case ULTRA_BEAST -> flags.ultraBeasts;
            case PARADOX -> flags.paradox;
            case REGULAR -> flags.missingRegular;
        };
    }

    /** Cobblemon labels no species with two of these at once; legendary/mythical is tested first. */
    public static Category category(Species species) {
        Set<String> labels = species.getLabels();
        if (labels.contains(CobblemonPokemonLabels.LEGENDARY) || labels.contains(CobblemonPokemonLabels.MYTHICAL)) {
            return Category.LEGENDARY_OR_MYTHICAL;
        }
        if (labels.contains(CobblemonPokemonLabels.ULTRA_BEAST)) {
            return Category.ULTRA_BEAST;
        }
        return labels.contains(CobblemonPokemonLabels.PARADOX) ? Category.PARADOX : Category.REGULAR;
    }

    /** Parses a property line and resolves its species, by identifier or by name; null if none. */
    @Nullable
    public static Species resolveSpecies(String spec) {
        try {
            String name = PokemonProperties.Companion.parse(spec).getSpecies();
            if (name == null || name.isBlank()) {
                return null;
            }
            String id = name.toLowerCase(Locale.ROOT);
            if (id.indexOf(':') >= 0) {
                ResourceLocation location = ResourceLocation.tryParse(id);
                return location == null ? null : PokemonSpecies.INSTANCE.getByIdentifier(location);
            }
            return PokemonSpecies.INSTANCE.getByName(id);
        } catch (Exception e) {
            return null;
        }
    }
}
