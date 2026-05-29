package maxigregrze.cobblesafari.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class JoinThrottle {

    private static final long COOLDOWN_MS = 250L;
    private static final int LOCKOUT_THRESHOLD = 10;
    private static final long LOCKOUT_MS = 60_000L;

    private static final class State {
        long lastAttemptMs;
        int consecutiveFailures;
        long lockoutUntilMs;
    }

    private static final Map<UUID, State> ATTEMPTS = new ConcurrentHashMap<>();

    private JoinThrottle() {}

    /** @return true if the join attempt may proceed; false to drop silently. */
    public static boolean tryAcquire(UUID playerId) {
        long now = System.currentTimeMillis();
        State st = ATTEMPTS.computeIfAbsent(playerId, k -> new State());
        if (now < st.lockoutUntilMs) {
            return false;
        }
        if (now - st.lastAttemptMs < COOLDOWN_MS) {
            return false;
        }
        st.lastAttemptMs = now;
        return true;
    }

    public static void recordFailure(UUID playerId) {
        State st = ATTEMPTS.computeIfAbsent(playerId, k -> new State());
        st.consecutiveFailures++;
        if (st.consecutiveFailures >= LOCKOUT_THRESHOLD) {
            st.lockoutUntilMs = System.currentTimeMillis() + LOCKOUT_MS;
            st.consecutiveFailures = 0;
        }
    }

    public static void recordSuccess(UUID playerId) {
        ATTEMPTS.remove(playerId);
    }

    public static void clear(UUID playerId) {
        ATTEMPTS.remove(playerId);
    }
}
