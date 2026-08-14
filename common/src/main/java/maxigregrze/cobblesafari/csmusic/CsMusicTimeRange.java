package maxigregrze.cobblesafari.csmusic;

import org.jetbrains.annotations.Nullable;

/**
 * Compiled {@code when.time_range} window, in the units of the vanilla {@code /time set} command:
 * a Minecraft day is 24000 ticks, 0 is dawn, 6000 noon, 12000 dusk, 18000 midnight.
 *
 * <p>{@link #start} is inclusive, {@link #end} exclusive. The window <b>wraps</b> when
 * {@code start > end}, so {@code [18000, 6000]} reads "from midnight to noon" - which is the whole
 * point of taking a pair rather than a min/max: a night window is not expressible otherwise.</p>
 *
 * <p>{@code [0, 24000]} is the full day (always true) and is legal; an empty window
 * ({@code start == end}) is not, and is rejected at load. A {@code start} of 24000 needs no
 * normalisation: it can only occur with {@code end < 24000}, i.e. in the wrapping branch, where
 * {@code dayTime >= 24000} is never true and the test collapses to {@code dayTime < end}.</p>
 */
public record CsMusicTimeRange(int start, int end) {

    /** Length of a Minecraft day, in ticks. */
    public static final int DAY_LENGTH = 24000;

    /**
     * Validates and compiles the json pair; returns {@code null} if the array is not exactly two
     * values in {@code [0, 24000]}, or if it describes an empty window.
     */
    @Nullable
    public static CsMusicTimeRange fromJson(int @Nullable [] raw) {
        if (raw == null || raw.length != 2) {
            return null;
        }
        int start = raw[0];
        int end = raw[1];
        if (start < 0 || start > DAY_LENGTH || end < 0 || end > DAY_LENGTH || start == end) {
            return null;
        }
        return new CsMusicTimeRange(start, end);
    }

    /** {@code dayTime} must already be normalised to {@code [0, DAY_LENGTH)}. */
    public boolean matches(long dayTime) {
        return start <= end
                ? dayTime >= start && dayTime < end
                : dayTime >= start || dayTime < end;
    }
}
