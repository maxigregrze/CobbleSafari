package maxigregrze.cobblesafari.config;

public class DimensionTimerEntry {
    private String dimensionId;
    private int timerDurationSeconds;
    private int resetHour;
    private boolean returnToSpawn;
    private Boolean allowReset;

    public DimensionTimerEntry() {
        this.dimensionId = "cobblesafari:domedimension";
        this.timerDurationSeconds = 900;
        this.resetHour = 0;
        this.returnToSpawn = false;
        this.allowReset = true;
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
        return resetHour;
    }

    public boolean isReturnToSpawn() {
        return returnToSpawn;
    }
}
