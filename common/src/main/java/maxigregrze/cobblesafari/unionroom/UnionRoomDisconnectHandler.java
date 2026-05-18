package maxigregrze.cobblesafari.unionroom;

import maxigregrze.cobblesafari.data.UnionRoomSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class UnionRoomDisconnectHandler {

    private UnionRoomDisconnectHandler() {}

    public static void onPlayerDisconnect(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        if (!UnionRoomManager.isInUnionRoom(player)) {
            return;
        }
        UUID playerUUID = player.getUUID();
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            return;
        }

        UnionRoomSavedData.SessionData session = data.findSessionByHost(playerUUID).orElse(null);
        if (session != null) {
            Set<UUID> guests = new HashSet<>(session.guestUUIDs);
            for (UUID g : guests) {
                if (server.getPlayerList().getPlayer(g) == null) {
                    data.markReconnectTeleport(g);
                }
            }
            UnionRoomManager.closeSession(server, session.instanceId,
                    "cobblesafari.unionroom.session_closed.host_disconnected", playerUUID);
            data.markReconnectTeleport(playerUUID);
        } else {
            data.removeGuestFromAllSessions(playerUUID);
            data.markReconnectTeleport(playerUUID);
        }
        data.setDirty();
    }

    public static void onPlayerLogin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            return;
        }
        if (!data.isMarkedForReconnectTeleport(player.getUUID())) {
            return;
        }
        UnionRoomManager.teleportPlayerBack(server, player, data,
                "cobblesafari.unionroom.session_closed.disconnected");
        data.clearReconnectMark(player.getUUID());
        data.setDirty();
    }
}
