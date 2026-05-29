package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.UnionRoomSavedData;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.security.JoinThrottle;
import maxigregrze.cobblesafari.security.RateLimiter;
import maxigregrze.cobblesafari.unionroom.UnionRoomManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public final class UnionAppServerHandler {

    private UnionAppServerHandler() {}

    private static boolean checkReadRateLimit(UUID playerId, int action) {
        long gap = switch (action) {
            case UnionAppPayload.ACTION_REQUEST_STATE -> 250L;
            case UnionAppPayload.ACTION_CREATE -> 2_000L;
            case UnionAppPayload.ACTION_CLOSE -> 1_000L;
            default -> 0L;
        };
        if (gap == 0L) {
            return true;
        }
        return RateLimiter.allow(playerId, RateLimiter.key(RateLimiter.SERVICE_UNION, action), gap);
    }

    public static void handle(ServerPlayer player, UnionAppPayload payload) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (!checkReadRateLimit(player.getUUID(), payload.actionType())) {
            return;
        }
        switch (payload.actionType()) {
            case UnionAppPayload.ACTION_REQUEST_STATE -> sendSnapshot(player, "");
            case UnionAppPayload.ACTION_CREATE -> {
                int[] createCode = payload.code();
                String type = (createCode != null && createCode.length >= 1 && createCode[0] == 1)
                        ? "plaza" : "room";
                UnionRoomManager.CreateResult r = UnionRoomManager.createSession(player, type);
                if (r == UnionRoomManager.CreateResult.OK) {
                    sendCloseGui(player);
                } else {
                    sendSnapshot(player, mapCreateError(r));
                }
            }
            case UnionAppPayload.ACTION_JOIN -> {
                if (!JoinThrottle.tryAcquire(player.getUUID())) {
                    return;
                }
                int[] code = payload.code();
                if (code == null || code.length != 4) {
                    JoinThrottle.recordFailure(player.getUUID());
                    sendSnapshot(player, "gui.cobblesafari.rotomphone.union.error.notfound");
                    return;
                }
                for (int d : code) {
                    if (d < 1 || d > 6) {
                        JoinThrottle.recordFailure(player.getUUID());
                        sendSnapshot(player, "gui.cobblesafari.rotomphone.union.error.notfound");
                        return;
                    }
                }
                UnionRoomManager.JoinResult jr = UnionRoomManager.joinSession(player, code);
                if (jr == UnionRoomManager.JoinResult.OK) {
                    JoinThrottle.recordSuccess(player.getUUID());
                    sendCloseGui(player);
                } else {
                    if (jr == UnionRoomManager.JoinResult.INVALID_CODE
                            || jr == UnionRoomManager.JoinResult.HOST_UNAVAILABLE
                            || jr == UnionRoomManager.JoinResult.FAILED) {
                        JoinThrottle.recordFailure(player.getUUID());
                    }
                    sendSnapshot(player, mapJoinError(jr));
                }
            }
            case UnionAppPayload.ACTION_CLOSE -> {
                UnionRoomSavedData data = UnionRoomSavedData.get(server);
                if (data == null) {
                    sendSnapshot(player, "cobblesafari.unionroom.error.dimension_not_found");
                    return;
                }
                Optional<UnionRoomSavedData.SessionData> host = data.findSessionByHost(player.getUUID());
                if (host.isPresent()) {
                    UnionRoomManager.handlePlayerExit(player, host.get().instanceId);
                    return;
                }
                Optional<UnionRoomSavedData.SessionData> guest =
                        data.findSessionContainingGuest(player.getUUID());
                if (guest.isPresent()) {
                    UnionRoomManager.handlePlayerExit(player, guest.get().instanceId);
                    return;
                }
                sendSnapshot(player, "gui.cobblesafari.rotomphone.union.error.not_in_session");
            }
            default -> {
                // Unknown action type; ignore
            }
        }
    }

    private static String mapCreateError(UnionRoomManager.CreateResult r) {
        return switch (r) {
            case ALREADY_IN_SESSION -> "gui.cobblesafari.rotomphone.union.error.alreadycreated";
            case MAX_INSTANCES -> "gui.cobblesafari.rotomphone.union.error.noroom";
            case BANNED_DIMENSION -> "cobblesafari.unionroom.error.banned_dimension";
            case DIMENSION_NOT_FOUND -> "cobblesafari.unionroom.error.dimension_not_found";
            case CREATION_FAILED -> "cobblesafari.unionroom.error.creation_failed";
            case OK -> "";
        };
    }

    private static String mapJoinError(UnionRoomManager.JoinResult r) {
        return switch (r) {
            case SESSION_FULL -> "gui.cobblesafari.rotomphone.union.error.session_full";
            case ALREADY_IN_SESSION -> "gui.cobblesafari.rotomphone.union.error.alreadycreated";
            case INVALID_CODE, FAILED -> "gui.cobblesafari.rotomphone.union.error.notfound";
            case BANNED_DIMENSION -> "cobblesafari.unionroom.error.banned_dimension";
            case DIMENSION_NOT_FOUND -> "cobblesafari.unionroom.error.dimension_not_found";
            case HOST_UNAVAILABLE -> "cobblesafari.unionroom.error.host_unavailable";
            case OWN_SESSION, ALREADY_JOINED -> "gui.cobblesafari.rotomphone.union.error.notfound";
            case OK -> "";
        };
    }

    public static void sendSnapshot(ServerPlayer player, String errorKey) {
        MinecraftServer server = player.getServer();
        UnionRoomSavedData data = server == null ? null : UnionRoomSavedData.get(server);

        int instancesMax = MiscConfig.getUnionRoomMaxInstances();
        int instancesUsed = data == null ? 0 : data.countActiveRoomSessions();
        int subscreen;
        int[] currentCode = new int[0];

        String dim = player.level().dimension().location().toString();
        boolean isUnionDim = dim.equals(UnionRoomManager.DIMENSION_ID);

        if (isUnionDim) {
            if (data == null) {
                subscreen = UnionAppResultPayload.SUB_ERROR;
            } else {
                var hostSession = data.findSessionByHost(player.getUUID());
                if (hostSession.isPresent()) {
                    subscreen = UnionAppResultPayload.SUB_UNION_HOST;
                    currentCode = hostSession.get().code.clone();
                } else {
                    var guestSession = data.findSessionContainingGuest(player.getUUID());
                    if (guestSession.isPresent()) {
                        subscreen = UnionAppResultPayload.SUB_GUEST;
                        currentCode = guestSession.get().code.clone();
                    } else {
                        subscreen = UnionAppResultPayload.SUB_ERROR;
                    }
                }
            }
        } else if (SafariTimerConfig.hasDimensionTimer(dim)
                || MiscConfig.getUnionRoomBannedDimensions().contains(dim)) {
            subscreen = UnionAppResultPayload.SUB_ERROR;
        } else {
            subscreen = UnionAppResultPayload.SUB_BEGIN;
        }

        Services.PLATFORM.sendPayloadToPlayer(player,
                new UnionAppResultPayload(subscreen, instancesUsed, instancesMax, currentCode,
                        errorKey == null ? "" : errorKey));
    }

    private static void sendCloseGui(ServerPlayer player) {
        Services.PLATFORM.sendPayloadToPlayer(player,
                new UnionAppResultPayload(UnionAppResultPayload.SUB_CLOSE_GUI, 0, 0, new int[0], ""));
    }
}
