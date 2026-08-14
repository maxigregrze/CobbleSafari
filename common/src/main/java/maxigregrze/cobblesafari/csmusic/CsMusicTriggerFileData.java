package maxigregrze.cobblesafari.csmusic;

import java.util.ArrayList;
import java.util.List;

/**
 * Gson DTO for a trigger file {@code data/<ns>/csmusic/definition/*.json}. A file holds a list of
 * cumulable rules; each rule matches when all declared conditions ({@link When}) hold.
 */
public class CsMusicTriggerFileData {

    public List<Rule> rules = new ArrayList<>();

    public static class Rule {
        /** Explicit csmusic id (exclusive with {@link #tag}). */
        public String music;
        /** Random pool by csmusic tag (exclusive with {@link #music}). */
        public String tag;
        public int priority = 1;
        public When when = new When();
    }

    public static class When {
        public String dimension;
        public String biome;
        public String biome_tag;
        /**
         * Window of the Minecraft day the rule plays in, as {@code [start, end]} in the units of the
         * vanilla {@code /time set} command (0 dawn, 6000 noon, 12000 dusk, 18000 midnight; a day is
         * 24000 ticks). Both bounds in {@code [0, 24000]}, start inclusive, end exclusive; the window
         * wraps when start > end, so {@code [18000, 6000]} is "midnight to noon". In a dimension with
         * a fixed time, the fixed value is tested, not the world clock. Omitted ⇒ no constraint.
         */
        public int[] time_range;
        /**
         * Weather states the rule plays in, ORed together: {@code "none"}, {@code "rain"},
         * {@code "snow"}, {@code "thunder"}. The four are mutually exclusive, so
         * {@code ["rain", "snow", "thunder"]} is how you say "any weather at all".
         *
         * <p>Resolved at the player's position: {@code "rain"} and {@code "snow"} follow the biome's
         * precipitation, so a dry biome (desert, badlands, any biome with
         * {@code has_precipitation: false}) stays {@code "none"} through a downpour, and a cold biome
         * reports {@code "snow"}. {@code "thunder"} is a sky state and covers the whole dimension,
         * dry biomes included. Dimensions without weather (no skylight, or with a ceiling) always
         * report {@code "none"}. Omitted or empty ⇒ no constraint.</p>
         */
        public List<String> weather;
        /**
         * Id of a csmusic area (as created by {@code /cobblesafari csmusic area create}) the player
         * must be standing in. Areas are per-dimension world data, so this implies the dimension.
         * Omitted ⇒ no constraint.
         */
        public String area;
        /**
         * Tag carried by an area the player is standing in. Lets one rule cover every town, every
         * cave, … without naming them. Omitted ⇒ no constraint.
         */
        public String area_tag;
        /** never | any | in_battle | wild | npc | pvp (default: any). Folds the old battle_type in. */
        public String battle;
        public String species;
        public String form;
        /**
         * Naturally generated structure the player must stand in: a structure id
         * ({@code minecraft:village_plains}), a structure tag ({@code #minecraft:village}),
         * or {@code *} / {@code any} for any structure. Omitted ⇒ no constraint.
         */
        public String structure;
        /**
         * Piece of a naturally generated structure the player must stand in, as a pattern over the
         * piece id where {@code *} matches any run of characters (including {@code /}). Jigsaw
         * pieces are identified by their template ({@code minecraft:village/plains/houses/...}),
         * legacy pieces by their piece type ({@code minecraft:mscorridor}). Omitted ⇒ no constraint.
         */
        public String structure_piece;
    }
}
