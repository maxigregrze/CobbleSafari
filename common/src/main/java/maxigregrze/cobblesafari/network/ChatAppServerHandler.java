package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.chat.ChatConversationDefinition;
import maxigregrze.cobblesafari.chat.ChatConversationRegistry;
import maxigregrze.cobblesafari.chat.ChatConversationService;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.rotomphone.ChatConversationSync;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes {@link ChatAppPayload} (C2S) to {@link ChatConversationService}.
 * All conversation ids are validated against the registry and the per-player unlock state; the claim
 * action is debounced against double-fire ().
 */
public final class ChatAppServerHandler {

    private ChatAppServerHandler() {}

    private static final long MUTATION_DEBOUNCE_MS = 2000L;
    private static final ConcurrentHashMap<UUID, Long> LAST_CLAIM_MS = new ConcurrentHashMap<>();

    /** Releases the per-player claim debounce entry; call on disconnect to avoid unbounded growth (C2). */
    public static void clear(UUID playerId) {
        LAST_CLAIM_MS.remove(playerId);
    }

    public static void handle(ServerPlayer player, ChatAppPayload payload) {
        if (!maxigregrze.cobblesafari.rotomphone.RotomPhoneServerHandler.hasPhone(player)) {
            return; // server-authoritative possession check (C1)
        }
        switch (payload.actionType()) {
            case ChatAppPayload.ACTION_REQUEST_CONTACTS -> ChatConversationSync.syncToPlayer(player);
            case ChatAppPayload.ACTION_OPEN, ChatAppPayload.ACTION_POLL_TASK -> sendState(player, payload.convId());
            case ChatAppPayload.ACTION_ADVANCE_MESSAGE -> {
                if (validate(player, payload.convId())) {
                    ChatConversationService.setMessageIndex(player, payload.convId(), payload.intArg());
                }
            }
            case ChatAppPayload.ACTION_CLAIM -> doClaim(player, payload.convId());
            case ChatAppPayload.ACTION_AFTER_DONE -> {
                if (validate(player, payload.convId())) {
                    ChatConversationService.onAfterMessagesDone(player, payload.convId());
                    sendState(player, payload.convId());
                }
            }
            default -> { /* unknown action */ }
        }
    }

    private static void doClaim(ServerPlayer player, String convId) {
        if (!validate(player, convId)) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_CLAIM_MS.get(player.getUUID());
        if (last != null && now - last < MUTATION_DEBOUNCE_MS) {
            return; // ignore double-fire
        }
        LAST_CLAIM_MS.put(player.getUUID(), now);

        ChatConversationService.ClaimResult r = ChatConversationService.tryClaim(player, convId);
        if (r == ChatConversationService.ClaimResult.SUCCESS) {
            sendState(player, convId);
        } else if (r == ChatConversationService.ClaimResult.NOT_COMPLETE) {
            LAST_CLAIM_MS.remove(player.getUUID()); // allow immediate retry after a benign failure
            Services.PLATFORM.sendPayloadToPlayer(player,
                    ChatAppResultPayload.error("gui.cobblesafari.rotomphone.chat.error.not_complete"));
        } else {
            LAST_CLAIM_MS.remove(player.getUUID());
            sendState(player, convId);
        }
    }

    private static void sendState(ServerPlayer player, String convId) {
        if (!validate(player, convId)) {
            return;
        }
        ChatAppResultPayload.StateData state = ChatConversationService.buildState(player, convId);
        if (state == null) {
            return;
        }
        Services.PLATFORM.sendPayloadToPlayer(player, ChatAppResultPayload.state(state));
    }

    /** Conversation exists and is unlocked for this player. */
    private static boolean validate(ServerPlayer player, String convId) {
        if (convId == null || convId.isEmpty()) {
            return false;
        }
        ChatConversationDefinition conv = ChatConversationRegistry.get(convId);
        return conv != null && ChatConversationRegistry.isUnlockedByPlayer(player, conv);
    }
}
