package maxigregrze.cobblesafari.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {

    public static final int SERVICE_GTS = 1;
    public static final int SERVICE_WONDER = 2;
    public static final int SERVICE_UNION = 3;
    public static final int SERVICE_UNDERGROUND = 4;

    private static final Map<UUID, Map<Integer, Long>> LAST_HIT_MS = new ConcurrentHashMap<>();

    private RateLimiter() {}

    public static int key(int service, int action) {
        return (service << 16) | (action & 0xFFFF);
    }

    /** @return true if the call is allowed; false if too soon since last hit. */
    public static boolean allow(UUID playerId, int actionKey, long minGapMs) {
        long now = System.currentTimeMillis();
        Map<Integer, Long> perAction =
                LAST_HIT_MS.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Long last = perAction.get(actionKey);
        if (last != null && now - last < minGapMs) {
            return false;
        }
        perAction.put(actionKey, now);
        return true;
    }

    public static void clear(UUID playerId) {
        LAST_HIT_MS.remove(playerId);
    }
}
