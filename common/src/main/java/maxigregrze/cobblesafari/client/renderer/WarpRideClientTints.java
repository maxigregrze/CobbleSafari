package maxigregrze.cobblesafari.client.renderer;

import maxigregrze.cobblesafari.warpride.network.WarpRidePayloads;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side {@code typeId -> tint} table, fed by {@code DestinationSyncPayload} at login and on
 * datapack reload (action plan 183, section 4.2 bis).
 *
 * <p>It lives here rather than on the mini-game screen because the exit portal has to be tinted
 * <b>outside</b> any ride - a portal standing in a sub-instance is drawn whenever a player looks at
 * it. Only the colour ever crosses the wire; structure pools and species arrays stay server-side.</p>
 */
public final class WarpRideClientTints {

    private WarpRideClientTints() {}

    /** Sentinel for "no colour known": the renderer then draws the untinted source texture. */
    public static final int NO_TINT = -1;

    private static final Map<Integer, Integer> TINTS = new HashMap<>();

    public static void accept(List<WarpRidePayloads.TintData> tints) {
        TINTS.clear();
        for (WarpRidePayloads.TintData tint : tints) {
            TINTS.put(tint.typeId(), tint.tint());
        }
    }

    /**
     * @return the destination's colour, or {@link #NO_TINT} when the id is unknown - a destination
     *         removed from the datapack while one of its portals still stands. The portal then
     *         renders white instead of coloured: visible, and harmless.
     */
    public static int tintFor(int typeId) {
        Integer tint = TINTS.get(typeId);
        return tint == null ? NO_TINT : tint;
    }
}
