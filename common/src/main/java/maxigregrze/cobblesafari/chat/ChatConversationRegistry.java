package maxigregrze.cobblesafari.chat;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side registry of loaded chat conversations. Mirrors
 * {@code RotomPhoneSkinRegistry}: cleared and refilled on (re)load.
 */
public final class ChatConversationRegistry {

    private static final Map<String, ChatConversationDefinition> CONVERSATIONS = new LinkedHashMap<>();

    private ChatConversationRegistry() {}

    public static void clear() {
        CONVERSATIONS.clear();
    }

    public static void register(ChatConversationDefinition def) {
        CONVERSATIONS.put(def.id(), def);
    }

    public static ChatConversationDefinition get(String id) {
        return CONVERSATIONS.get(id);
    }

    public static List<ChatConversationDefinition> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(CONVERSATIONS.values()));
    }

    /** Conversations sorted by {@code displayPriority} asc, then id alphabetically. */
    public static List<ChatConversationDefinition> getAllSorted() {
        List<ChatConversationDefinition> list = new ArrayList<>(CONVERSATIONS.values());
        list.sort(Comparator
                .comparingInt(ChatConversationDefinition::displayPriority)
                .thenComparing(ChatConversationDefinition::id));
        return list;
    }

    /**
     * Same gating logic as {@code RotomPhoneSkinRegistry.isUnlockedByPlayer}: unlocked from start, or
     * the configured advancement is done. Note: per the advancement only matters when
     * {@code unlockedFromStart == false}.
     */
    public static boolean isUnlockedByPlayer(ServerPlayer player, ChatConversationDefinition c) {
        if (c == null) {
            return false;
        }
        if (c.unlockedFromStart()) {
            return true;
        }
        String adv = c.unlockingAdvancement();
        if (adv == null || adv.isEmpty()) {
            return false;
        }
        ResourceLocation advId = ResourceLocation.tryParse(adv);
        if (advId == null) {
            return false;
        }
        AdvancementHolder holder = player.server.getAdvancements().get(advId);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
