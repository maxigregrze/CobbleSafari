package maxigregrze.cobblesafari.warpride.logic;

/**
 * @param lane   0..7 for a lane object; {@link #NO_LANE} for a portal, which sits on the axis
 * @param rank   portals only: 0..3, the {@code tier} output
 * @param typeId portals only: the destination id, the {@code type} output
 */
public record WaveObject(Kind kind, int lane, int rank, int typeId) {

    public static final int NO_LANE = -1;

    public enum Kind {
        POSITIVE,
        NEGATIVE,
        PORTAL
    }

    public static WaveObject positive(int lane) {
        return new WaveObject(Kind.POSITIVE, lane, 0, 0);
    }

    public static WaveObject negative(int lane) {
        return new WaveObject(Kind.NEGATIVE, lane, 0, 0);
    }

    public static WaveObject portal(int rank, int typeId) {
        return new WaveObject(Kind.PORTAL, NO_LANE, rank, typeId);
    }

    public boolean isPortal() {
        return kind == Kind.PORTAL;
    }
}
