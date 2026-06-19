package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.network.ChatConversationSyncPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Client cache of chat conversation definitions (contact list) synced from the server
 *. Mirrors {@code RotomPhoneClientCache}.
 */
public final class ChatConversationClientCache {

    private static List<ChatConversationSyncPayload.Entry> conversations = new ArrayList<>();

    private ChatConversationClientCache() {}

    public static void setConversations(List<ChatConversationSyncPayload.Entry> list) {
        conversations = new ArrayList<>(list);
    }

    public static List<ChatConversationSyncPayload.Entry> getConversations() {
        return conversations;
    }
}
