package maxigregrze.cobblesafari.mark;

import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class PartnerRibbonTitles {

    public static final ResourceLocation RIBBON_PARTNER_ID =
            ResourceLocation.fromNamespaceAndPath("cobblemon", "ribbon_partner");

    private static final String TITLE_KEY = "cobblemon.mark.ribbon_partner.title";

    private PartnerRibbonTitles() {
    }

    public static boolean isPartnerRibbon(Mark mark) {
        return mark != null && RIBBON_PARTNER_ID.equals(mark.getIdentifier());
    }

    public static Mark resolveEntityMark(PokemonEntity entity) {
        return entity.getPokemon().getActiveMark();
    }

    public static MutableComponent applyTitle(Pokemon pokemon, MutableComponent pokemonDisplayName, Mark mark) {
        if (!isPartnerRibbon(mark)) {
            return mark.getTitle(pokemonDisplayName);
        }
        pokemon.refreshOriginalTrainer();
        String ot = pokemon.getOriginalTrainerName();
        if (ot == null || ot.isBlank()) {
            return pokemonDisplayName;
        }
        MutableComponent title = Component.translatable(
                TITLE_KEY,
                ot,
                pokemonDisplayName.copy().withColor(-1)
        );
        String titleColour = mark.getTitleColour();
        if (titleColour != null) {
            try {
                title = title.withColor(Integer.parseInt(titleColour, 16));
            } catch (NumberFormatException ignored) {
                // keep uncoloured title
            }
        }
        return title;
    }
}
