package maxigregrze.cobblesafari.config;

/**
 * DTO for {@code dimensional_music.json} — global csmusic settings.
 * {@code enabled}: global csmusic toggle. {@code defaultAreaPriority}: fallback priority for a
 * music area created without an explicit value. Where music plays is defined entirely by
 * {@code data/<ns>/csmusic/definition/*.json} trigger files (dimension, biome, battle, …).
 */
public class DimensionalMusicData {

    public boolean enabled = true;
    /** Default priority assigned to a music area when created without an explicit value. */
    public int defaultAreaPriority = 1;
}
