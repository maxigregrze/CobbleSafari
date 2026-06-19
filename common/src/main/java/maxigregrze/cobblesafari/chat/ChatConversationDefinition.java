package maxigregrze.cobblesafari.chat;

import java.util.List;

/**
 * A chat questline loaded from {@code data/<ns>/chat_conversation/*.json}.
 * Immutable; validated and built by {@link ChatConversationDataLoader}.
 */
public record ChatConversationDefinition(
        int schemaVersion,
        String id,
        String displayName,
        int displayPriority,
        String textureFile,
        boolean unlockedFromStart,
        String unlockingAdvancement, // nullable when unlockedFromStart
        List<ChatStepDefinition> steps) {

    public ChatStepDefinition step(int index) {
        if (index < 0 || index >= steps.size()) {
            return null;
        }
        return steps.get(index);
    }

    public boolean isLastStep(int index) {
        return index == steps.size() - 1;
    }
}
