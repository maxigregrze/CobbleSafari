package maxigregrze.cobblesafari.warpride.logic;

import java.util.List;

/**
 * One wave on the track. Waves are spaced by <em>distance</em>, not time, which is what makes high
 * energy simultaneously faster and harder: 4 waves a second at 64 ly/s, one a second at 16.
 */
public final class Wave {

    public static final long NOT_CROSSED = Long.MIN_VALUE;

    private final int index;
    private final double trackPos;
    private final List<WaveObject> objects;
    private boolean resolved;
    private long crossTick;

    public Wave(int index, double trackPos, List<WaveObject> objects) {
        this.index = index;
        this.trackPos = trackPos;
        this.objects = List.copyOf(objects);
        this.resolved = false;
        this.crossTick = NOT_CROSSED;
    }

    /** Tick the odometer first reached this wave. Impacts resolve later but are judged as of here. */
    public long crossTick() {
        return crossTick;
    }

    public boolean hasCrossed() {
        return crossTick != NOT_CROSSED;
    }

    public void markCrossed(long tick) {
        if (crossTick == NOT_CROSSED) {
            this.crossTick = tick;
        }
    }

    public int index() {
        return index;
    }

    public double trackPos() {
        return trackPos;
    }

    public List<WaveObject> objects() {
        return objects;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void markResolved() {
        this.resolved = true;
    }

    /** Relative depth in front of the rider; crosses 0 at the impact instant. */
    public double depth(double odometer) {
        return trackPos - odometer;
    }

    public boolean hasPortal() {
        for (WaveObject o : objects) {
            if (o.isPortal()) {
                return true;
            }
        }
        return false;
    }

    public WaveObject portal() {
        for (WaveObject o : objects) {
            if (o.isPortal()) {
                return o;
            }
        }
        return null;
    }
}
