package maxigregrze.cobblesafari.warpride.logic;

import com.cobblemon.mod.common.pokemon.Species;
import maxigregrze.cobblesafari.onboarding.SpeciesAvailability;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One encounter pool of a destination: weighted Cobblemon property strings plus an optional
 * fallback (action plan 190, E1 / E3).
 *
 * <p>The draw <b>filters before it rolls</b>: entries whose species cannot be used are removed
 * first and the weights of the rest renormalise, the fallback only answering once nothing is left.
 * Replacing a failed pick after the roll would skew the odds as soon as an addon supplied only some
 * of a pool's species - see decision 4 bis.6 b.</p>
 *
 * @param entries  weighted entries; a normal pool's all weigh 1
 * @param fallback what the pool gives when none of its entries is usable, or {@code null}
 */
public record WarpRideEncounterPool(List<Entry> entries, @Nullable String fallback) {

    /** No entries and no fallback: a pool that never gives anything. */
    public static final WarpRideEncounterPool EMPTY = new WarpRideEncounterPool(List.of(), null);

    /** One Cobblemon property string ({@code "weezing galarian"}) and its weight. */
    public record Entry(String pokemon, int weight) {}

    /** The result of a draw, and whether it came from the fallback. */
    public record Draw(String pokemon, boolean fromFallback) {}

    /** Why an entry can or cannot be spawned. */
    public enum Availability {
        USABLE,
        /** No species by that name: an unknown id, a typo, or the addon that adds it is absent. */
        NOT_FOUND,
        /** The species exists but Cobblemon flags it as not implemented: it has no model. */
        NOT_IMPLEMENTED
    }

    public WarpRideEncounterPool {
        entries = List.copyOf(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty() && fallback == null;
    }

    /**
     * Draws one usable entry, else the usable fallback, else {@code null}. Availability is checked
     * here, at the roll, rather than trusted from load: species data and destinations are two
     * datapacks reloaded independently, and only the roll is guaranteed to see both current.
     */
    @Nullable
    public Draw draw(RandomSource random) {
        List<Entry> usable = new ArrayList<>(entries.size());
        int total = 0;
        for (Entry entry : entries) {
            if (availability(entry.pokemon()) == Availability.USABLE) {
                usable.add(entry);
                total += entry.weight();
            }
        }
        if (total > 0) {
            int roll = random.nextInt(total);
            for (Entry entry : usable) {
                roll -= entry.weight();
                if (roll < 0) {
                    return new Draw(entry.pokemon(), false);
                }
            }
        }
        if (fallback != null && availability(fallback) == Availability.USABLE) {
            return new Draw(fallback, true);
        }
        return null;
    }

    /**
     * Whether a property string names a species that can be spawned: found <b>and</b> implemented.
     * The second test is the one Ultra Beasts trip: Cobblemon ships species data for all of them,
     * but only flags the ones it has a model for as implemented, and an addon that adds a model sets
     * the flag (decision 4 bis.6 a).
     */
    public static Availability availability(String spec) {
        Species species = SpeciesAvailability.resolveSpecies(spec);
        if (species == null) {
            return Availability.NOT_FOUND;
        }
        return species.getImplemented() ? Availability.USABLE : Availability.NOT_IMPLEMENTED;
    }
}
