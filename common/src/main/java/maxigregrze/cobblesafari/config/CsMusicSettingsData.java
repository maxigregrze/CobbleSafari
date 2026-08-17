package maxigregrze.cobblesafari.config;

/**
 * DTO for {@code csmusic_settings.json} - global csmusic settings. Where music plays is defined
 * entirely by the definition files {@code data/<ns>/csmusic/definition/*.json}.
 *
 * <p>Replaces the former {@code dimensional_music.json} / {@code DimensionalMusicData}; the
 * migration is handled once by {@link CsMusicSettingsConfig}.</p>
 */
public class CsMusicSettingsData {

    /** Global csmusic toggle. */
    public boolean enabled = true;

    /** Default priority assigned to a music area created without an explicit value. */
    public int defaultAreaPriority = 1;

    /**
     * How long (ticks) a chunk's resolved structure list stays valid. Only relevant for rules
     * using {@code when.structure}. Lower = picks up newly generated structures sooner;
     * higher = fewer chunk lookups. Clamped to a 20-tick minimum.
     */
    public int structureCacheTtlTicks = 600;

    /**
     * How often (in ticks) the music arbitration sweep runs. Transitions fade over one to five
     * seconds, so a value up to ~5 is inaudible. Boss transitions are always immediate, whatever
     * this is. Set to 1 to restore the pre-160 behaviour exactly.
     */
    public int arbitrationIntervalTicks = 5;

    /**
     * How many times the loop of a track drawn from a tag pool plays before another track is
     * drawn. {@code 0} restores the historical behaviour, where the drawn track loops forever.
     * Negative values are read as {@code 0}.
     *
     * <p>Has no effect on a rule targeting an explicit {@code music} id, nor on a pool holding a
     * single track: there is nothing to shuffle to, so the track loops forever instead of
     * replaying its intro every N loops.</p>
     */
    public int repeatBeforeShuffle = 2;
}
