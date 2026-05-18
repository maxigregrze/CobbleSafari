package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.UnionRoomSavedData;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.unionroom.UnionRoomManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class UnionAppServerHandler {

    private UnionAppServerHandler() {}

    public static void handle(ServerPlayer player, UnionAppPayload payload) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        switch (payload.actionType()) {
            case UnionAppPayload.ACTION_REQUEST_STATE -> sendSnapshot(player, "");
            case UnionAppPayload.ACTION_CREATE -> {
                UnionRoomManager.CreateResult r = UnionRoomManager.createSession(player);
                if (r == UnionRoomManager.CreateResult.OK) {
                    sendCloseGui(player);
                } else {
                    sendSnapshot(player, mapCreateError(r));
                }
            }
            case UnionAppPayload.ACTION_JOIN -> {
                int[] code = payload.code();
                if (code == null || code.length != 4) {
                    sendSnapshot(player, "gui.cobblesafari.rotomphone.union.error.notfound");
                    return;
                }
                for (int d : code) {
                    if (d < 1 || d > 6) {
                        sendSnapshot(player, "gui.cobblesafari.rotomphone.union.error.notfound");
                        return;
                    }
                }
                if (UnionRoomManager.joinSession(player, code)) {
                    sendCloseGui(player);
                } else {
                    sendSnapshot(player, "gui.cobblesafari.rotomphone.union.error.notfound");
                }
            }
            case UnionAppPayload.ACTION_CLOSE -> {
                UnionRoomSavedData data = UnionRoomSavedData.get(server);
                if (data == null) {
                    sendSnapshot(player, "cobblesafari.unionroom.error.dimension_not_found");
                    return;
                }
                data.findSessionByHost(player.getUUID()).ifPresentOrElse(
                        s -> UnionRoomManager.handlePlayerExit(player, s.instanceId),
                        () -> sendSnapshot(player, "cobblesafari.unionroom.error.invalid_code"));
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

    public static void sendSnapshot(ServerPlayer player, String errorKey) {
        MinecraftServer server = player.getServer();
        UnionRoomSavedData data = server == null ? null : UnionRoomSavedData.get(server);

        int instancesMax = MiscConfig.getUnionRoomMaxInstances();
        int instancesUsed = data == null ? 0 : data.getAllSessions().size();
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
