package maxigregrze.cobblesafari.compat;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import maxigregrze.cobblesafari.block.pokestatue.AbstractPokeStatueBlock;
import maxigregrze.cobblesafari.block.pokestatue.AbstractPokeStatueBlockEntity;
import maxigregrze.cobblesafari.block.pokestatue.PokeStatueBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared tooltip payload + rendering helpers for the Pokémon statue, used by both the WTHIT and the
 * Jade components so the two stay identical.
 *
 * <p>Only the raw species string travels; the localized name is resolved client-side to avoid
 * shipping a serialized component every tick.</p>
 */
public final class PokeStatueTooltipData {

    public static final String KEY_SPECIES = "species";
    public static final String KEY_GLAZED = "glazed";

    private PokeStatueTooltipData() {}

    /** Fills the server-side payload from whichever part of whichever statue is being looked at. */
    public static void write(CompoundTag tag, Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof AbstractPokeStatueBlock)) {
            return;
        }
        AbstractPokeStatueBlockEntity data = AbstractPokeStatueBlockEntity.dataAt(level, pos, state);
        tag.putString(KEY_SPECIES, data == null ? "" : data.getSpecies());
        // Only the regular statue can be waxed; the creative one has no GLAZED property at all.
        tag.putBoolean(KEY_GLAZED, state.getBlock() instanceof PokeStatueBlock
                && state.getValue(PokeStatueBlock.GLAZED));
    }

    /**
     * Localized species name, or the "Default" label when the field is empty or does not resolve
     * (which is exactly when the statue shows Cobblemon's substitute model).
     */
    public static Component speciesLine(CompoundTag data) {
        String raw = data.getString(KEY_SPECIES);
        return Component.translatable("cobblesafari.waila.pokestatue.species", speciesName(raw));
    }

    private static Component speciesName(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.translatable("cobblesafari.waila.pokestatue.species.default");
        }
        try {
            String id = PokemonProperties.Companion.parse(raw).getSpecies();
            if (id != null) {
                ResourceLocation key = id.indexOf(':') >= 0
                        ? ResourceLocation.parse(id)
                        : ResourceLocation.fromNamespaceAndPath("cobblemon", id);
                Species species = PokemonSpecies.INSTANCE.getByIdentifier(key);
                if (species != null) {
                    return species.getTranslatedName();
                }
            }
        } catch (Exception ignored) {
            // Unparseable input falls through to the default label, same as the renderer.
        }
        return Component.translatable("cobblesafari.waila.pokestatue.species.default");
    }

    public static boolean isGlazed(CompoundTag data) {
        return data.getBoolean(KEY_GLAZED);
    }

    public static Component glazedLine() {
        return Component.translatable("cobblesafari.waila.pokestatue.glazed");
    }
}
