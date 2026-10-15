package maxigregrze.cobblesafari.block.dungeon;

/** What a Portal Spawner is currently doing, as shown in its config GUI (action plan 188, section 12.7). */
public enum PortalSpawnerStatus {
    DISABLED("disabled"),
    OPEN("open"),
    COOLDOWN("cooldown"),
    WAITING_SIGNAL("waiting_signal"),
    BLOCKED("blocked");

    private final String id;

    PortalSpawnerStatus(String id) {
        this.id = id;
    }

    public String translationKey() {
        return "gui.cobblesafari.portal_spawner.status." + id;
    }

    /** Whether the status line shows a countdown. */
    public boolean hasCountdown() {
        return this == OPEN || this == COOLDOWN;
    }

    public static PortalSpawnerStatus byOrdinal(int ordinal) {
        PortalSpawnerStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DISABLED;
    }
}
