package maxigregrze.cobblesafari.config;

public class DimensionTimerEntry {
    private String dimensionId;
    private int timerDurationSeconds;
    /** {@code null} ⇒ inherit the server-wide {@code dailySystemResetHour}. */
    private Integer resetHour;
    private boolean returnToSpawn;
    private Boolean allowReset;
    /**
     * {@code null} ⇒ derived from the dungeon registry on every read; non-{@code null} ⇒ an explicit
     * operator override. Deliberately never written by {@link #initializeDefaults()} - see
     * {@link #isInstanced()}.
     */
    private Boolean instanced;

    public DimensionTimerEntry() {
        this.dimensionId = "cobblesafari:domedimension";
        this.timerDurationSeconds = 900;
        this.resetHour = null;
        this.returnToSpawn = false;
        this.allowReset = true;
    }

    /** Creates an entry whose reset hour inherits {@code dailySystemResetHour} (resetHour unset). */
    public DimensionTimerEntry(String dimensionId, int timerDurationSeconds) {
        this.dimensionId = dimensionId;
        this.timerDurationSeconds = timerDurationSeconds;
        this.resetHour = null;
        this.returnToSpawn = false;
        this.allowReset = defaultAllowReset(dimensionId);
    }

    public DimensionTimerEntry(String dimensionId, int timerDurationSeconds, int resetHour) {
        this.dimensionId = dimensionId;
        this.timerDurationSeconds = timerDurationSeconds;
        this.resetHour = resetHour;
        this.returnToSpawn = false;
        this.allowReset = defaultAllowReset(dimensionId);
    }

    public DimensionTimerEntry(String dimensionId, int timerDurationSeconds, int resetHour, boolean returnToSpawn) {
        this.dimensionId = dimensionId;
        this.timerDurationSeconds = timerDurationSeconds;
        this.resetHour = resetHour;
        this.returnToSpawn = returnToSpawn;
        this.allowReset = defaultAllowReset(dimensionId);
    }

    private static boolean defaultAllowReset(String dimensionId) {
        return !dimensionId.contains("dungeon_");
    }

    public boolean isAllowReset() {
        if (allowReset != null) return allowReset;
        return defaultAllowReset(dimensionId);
    }

    /**
     * Whether this dimension keeps <em>one timer per visit</em> (a session) rather than one per
     * player (an account) - action plan 181, V1.
     * <p>
     * The default is <b>derived from the dungeon registry on every read and never persisted</b>.
     * Stamping it once would be wrong two ways: {@code SafariTimerConfig.ensureDimensionEntry} - the
     * path addon dungeons take - creates entries without ever calling {@link #initializeDefaults()},
     * so a stamped default would permanently miss them; and the default is not a constant but a
     * function of the registry, so freezing it would stop it self-correcting when a dungeon is added,
     * removed, or reclassified as externally managed. A {@code null} field already means "nobody
     * decided"; writing the default would destroy exactly that distinction.
     * <p>
     * Safe to call at any point timer logic runs: {@code DungeonDimensions.register()} completes at
     * {@code CobbleSafari.initLogic()} before any timer is ticked, and nothing in
     * {@code SafariTimerConfig.load()} reads this flag.
     */
    public boolean isInstanced() {
        if (instanced != null) return instanced;
        return maxigregrze.cobblesafari.dungeon.DungeonDimensions.isInstancedDungeonDimension(dimensionId);
    }

    public boolean initializeDefaults() {
        if (allowReset == null) {
            allowReset = defaultAllowReset(dimensionId);
            return true;
        }
        return false;
    }

    public String getDimensionId() {
        return dimensionId;
    }

    public int getTimerDurationSeconds() {
        return timerDurationSeconds;
    }

    public int getTimerDurationTicks() {
        return timerDurationSeconds * 20;
    }

    public int getResetHour() {
        return resetHour != null
                ? resetHour
                : maxigregrze.cobblesafari.config.MiscConfig.getDailySystemResetHour();
    }

    public boolean isReturnToSpawn() {
        return returnToSpawn;
    }
}
