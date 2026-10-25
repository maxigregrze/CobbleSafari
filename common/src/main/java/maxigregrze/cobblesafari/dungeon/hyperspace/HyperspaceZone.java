package maxigregrze.cobblesafari.dungeon.hyperspace;

import java.util.List;

/**
 * What one {@code dungeon_paris} instance rolled (action plan 196, § 3.6).
 *
 * @param types         Showdown type ids of the instance, one or two
 * @param targetSpecies namespaced id of the lead species of every resolved cluster, e.g.
 *                      {@code cobblemon:starly}; the realistic targets of a species task
 */
public record HyperspaceZone(int star, List<String> types, List<String> targetSpecies) {

    public HyperspaceZone {
        types = List.copyOf(types);
        targetSpecies = List.copyOf(targetSpecies);
    }
}
