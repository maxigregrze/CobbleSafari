package maxigregrze.cobblesafari.dungeon.hyperspace;

import kotlin.ranges.IntRange;

import java.util.List;

/**
 * One Hyperspace Wild Zone cluster (action plan 196, § 2.2): a family whose members' rarities
 * share one habitat, e.g. Starly 80 / Staravia 20.
 *
 * @param weight how many Serebii zones of this star and type contain the cluster
 */
public record HyperspaceCluster(String id, int star, String type, int weight, List<Member> members) {

    public HyperspaceCluster {
        members = List.copyOf(members);
    }

    /** The member with the highest rarity - the realistic target of a "catch this species" task. */
    public Member lead() {
        Member best = members.get(0);
        for (Member m : members) {
            if (m.rarity() > best.rarity()) {
                best = m;
            }
        }
        return best;
    }

    /**
     * @param pokemon a {@code PokemonProperties} string: species plus Cobblemon aspects
     */
    public record Member(String pokemon, int rarity, IntRange level, IntRange alphaLevel, String position) {

        /** The species token of {@link #pokemon()}, e.g. {@code meowth} for {@code meowth galarian}. */
        public String speciesName() {
            int space = pokemon.indexOf(' ');
            return space < 0 ? pokemon : pokemon.substring(0, space);
        }
    }
}
