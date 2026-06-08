package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.chat.ChatConversationDefinition;
import maxigregrze.cobblesafari.chat.ChatConversationRegistry;
import maxigregrze.cobblesafari.network.ChatConversationSyncPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and sends {@link ChatConversationSyncPayload} (contact list + per-player unlock flag) to a
 * player at join and after a datapack reload (cf. action plan 114 §13.2). Mirrors
 * {@code RotomPhoneConfigSync}.
 */
public final class ChatConversationSync {

    private ChatConversationSync() {}

    public static void syncToPlayer(ServerPlayer player) {
        List<ChatConversationSyncPayload.Entry> entries = new ArrayList<>();
        for (ChatConversationDefinition c : ChatConversationRegistry.getAllSorted()) {
            boolean unlocked = ChatConversationRegistry.isUnlockedByPlayer(player, c);
            entries.add(new ChatConversationSyncPayload.Entry(
                    c.id(), c.displayName(), c.displayPriority(), c.textureFile(), unlocked));
        }
        Services.PLATFORM.sendPayloadToPlayer(player, new ChatConversationSyncPayload(entries));
    }

    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }
}
