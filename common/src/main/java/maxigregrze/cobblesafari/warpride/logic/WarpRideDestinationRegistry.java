package maxigregrze.cobblesafari.warpride.logic;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry of {@link WarpRideDestination} by {@code typeId}. Only typeId to tint reaches a client. */
public final class WarpRideDestinationRegistry {

    private WarpRideDestinationRegistry() {}

    private static final Map<Integer, WarpRideDestination> BY_TYPE_ID = new LinkedHashMap<>();

    public static void clear() {
        BY_TYPE_ID.clear();
    }

    /** @return false if a destination with this {@code typeId} is already registered */
    public static boolean register(WarpRideDestination destination) {
        if (BY_TYPE_ID.containsKey(destination.typeId())) {
            return false;
        }
        BY_TYPE_ID.put(destination.typeId(), destination);
        return true;
    }

    public static boolean isEmpty() {
        return BY_TYPE_ID.isEmpty();
    }

    public static int size() {
        return BY_TYPE_ID.size();
    }

    public static List<WarpRideDestination> all() {
        return List.copyOf(BY_TYPE_ID.values());
    }

    /** Resolves a {@code typeId}, falling back to the lowest-numbered destination with a warning. */
    public static WarpRideDestination byTypeId(int typeId) {
        WarpRideDestination direct = BY_TYPE_ID.get(typeId);
        if (direct != null) {
            return direct;
        }
        if (BY_TYPE_ID.isEmpty()) {
            return null;
        }
        int lowest = Collections.min(BY_TYPE_ID.keySet());
        WarpRideDestination fallback = BY_TYPE_ID.get(lowest);
        CobbleSafari.LOGGER.warn("[WarpRide] Unknown destination typeId={}; falling back to typeId={}",
                typeId, lowest);
        return fallback;
    }

    /** Weighted roll over the registered destinations. Returns -1 when none are registered. */
    public static int rollTypeId(RandomSource random) {
        int total = 0;
        for (WarpRideDestination d : BY_TYPE_ID.values()) {
            total += Math.max(0, d.weight());
        }
        if (total <= 0) {
            return BY_TYPE_ID.isEmpty() ? -1 : Collections.min(BY_TYPE_ID.keySet());
        }
        int roll = random.nextInt(total);
        int acc = 0;
        for (WarpRideDestination d : BY_TYPE_ID.values()) {
            acc += Math.max(0, d.weight());
            if (roll < acc) {
                return d.typeId();
            }
        }
        return Collections.min(BY_TYPE_ID.keySet());
    }

    /** The lean {@code typeId → tint} view sent to clients. */
    public static List<int[]> tintSyncView() {
        List<int[]> out = new ArrayList<>(BY_TYPE_ID.size());
        for (WarpRideDestination d : BY_TYPE_ID.values()) {
            out.add(new int[]{d.typeId(), d.tint()});
        }
        return out;
    }
}
