package maxigregrze.cobblesafari.warpride.network;

import maxigregrze.cobblesafari.security.RateLimiter;
import maxigregrze.cobblesafari.warpride.WarpRideManager;
import maxigregrze.cobblesafari.warpride.WarpRideSession;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side handlers for the three client-to-server payloads. Every handler clears the same three
 * gates first: ownership, rate limit, phase. A session id is not a secret; ownership is the control.
 */
public final class WarpRideNetworking {

    private WarpRideNetworking() {}

    /** Just under the 25 ms turn cooldown, so the gameplay gate always bites first. */
    private static final long INPUT_MIN_GAP_MS = 20L;

    private static final long READY_MIN_GAP_MS = 500L;

    public static void handleReady(ServerPlayer player, WarpRidePayloads.ReadyPayload payload) {
        WarpRideSession session = resolve(player, payload.sessionId());
        if (session == null) {
            return;
        }
        if (!RateLimiter.allow(player.getUUID(),
                RateLimiter.key(RateLimiter.SERVICE_WARPRIDE, 1), READY_MIN_GAP_MS)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        // onReady() refuses anything but AWAITING_READY, so a replayed ready is inert - and starts
        // the music only once.
        if (session.onReady(server.getTickCount())) {
            WarpRideManager.onRideStarted(player);
        }
    }

    public static void handleInput(ServerPlayer player, WarpRidePayloads.InputPayload payload) {
        WarpRideSession session = resolve(player, payload.sessionId());
        if (session == null) {
            return;
        }
        if (!RateLimiter.allow(player.getUUID(),
                RateLimiter.key(RateLimiter.SERVICE_WARPRIDE, 0), INPUT_MIN_GAP_MS)) {
            return;
        }
        int input = payload.input();
        if (input < WarpRidePayloads.INPUT_LEFT || input > WarpRidePayloads.INPUT_JUMP) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        session.onInput(input, payload.clientTick(), server.getTickCount());
    }

    public static void handleClose(ServerPlayer player, WarpRidePayloads.ClosePayload payload) {
        WarpRideSession session = resolve(player, payload.sessionId());
        if (session == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        WarpRideManager.endSession(server, player.getUUID(), WarpRidePayloads.END_REASON_ABORTED);
    }

    /** @return the caller's own live session, or null for anyone else's, unknown, or ended */
    private static WarpRideSession resolve(ServerPlayer player, java.util.UUID sessionId) {
        WarpRideSession session = WarpRideManager.getSession(player.getUUID());
        if (session == null) {
            return null;
        }
        if (!player.getUUID().equals(session.getOwnerId())) {
            return null;
        }
        if (!session.getSessionId().equals(sessionId)) {
            return null;
        }
        return session;
    }
}
