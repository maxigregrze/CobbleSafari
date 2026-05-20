package maxigregrze.cobblesafari.client.screen.rotomphone;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SpeciesAutocompleteHelper {

    private static List<String> speciesIdsCache;

    private SpeciesAutocompleteHelper() {}

    public static List<String> getSpeciesIds() {
        if (speciesIdsCache == null) {
            List<String> out = new ArrayList<>();
            for (Species sp : PokemonSpecies.INSTANCE.getImplemented()) {
                ResourceLocation rl = sp.getResourceIdentifier();
                if (Cobblemon.MODID.equals(rl.getNamespace())) {
                    out.add(rl.getPath());
                } else {
                    out.add(rl.toString());
                }
            }
            out.sort(Comparator.naturalOrder());
            speciesIdsCache = List.copyOf(out);
        }
        return speciesIdsCache;
    }

    public static List<String> suggest(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String id : getSpeciesIds()) {
            String lower = id.toLowerCase(Locale.ROOT);
            if (lower.startsWith(q) || lower.contains(q)) {
                matches.add(id);
                if (matches.size() >= limit) {
                    break;
                }
            }
        }
        return matches;
    }

    public static void renderSuggestions(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int width,
            List<String> suggestions,
            int hoveredIndex) {
        if (suggestions.isEmpty()) {
            return;
        }
        int lineH = 12;
        int h = suggestions.size() * lineH + 4;
        g.fill(x, y, x + width, y + h, 0xC0000000);
        for (int i = 0; i < suggestions.size(); i++) {
            int ly = y + 2 + i * lineH;
            if (i == hoveredIndex) {
                g.fill(x, ly, x + width, ly + lineH, 0xFF397e7e);
            }
            g.drawString(font, suggestions.get(i), x + 4, ly + 2, 0xFFFFFFFF, false);
        }
    }
}
