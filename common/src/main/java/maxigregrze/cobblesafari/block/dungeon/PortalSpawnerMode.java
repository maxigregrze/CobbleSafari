package maxigregrze.cobblesafari.block.dungeon;

/** How a Portal Spawner decides when to open its next portal (action plan 188, section 12). */
public enum PortalSpawnerMode {
    /** A fixed delay after the previous portal closed. */
    INTERVAL("interval"),
    /** A delay rolled between a min and a max after the previous portal closed. */
    RANDOM_INTERVAL("random_interval"),
    /** One portal per rising edge of a redstone signal of at least the configured strength. */
    REDSTONE("redstone");

    private final String id;

    PortalSpawnerMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "gui.cobblesafari.portal_spawner.mode." + id;
    }

    /** Wire / NBT decoding; anything out of range falls back to {@link #INTERVAL}. */
    public static PortalSpawnerMode byOrdinal(int ordinal) {
        PortalSpawnerMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : INTERVAL;
    }
}
