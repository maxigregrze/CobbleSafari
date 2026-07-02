package maxigregrze.cobblesafari.chat;

import java.util.List;

/**
 * A chat questline loaded from {@code data/<ns>/chat_conversation/*.json}.
 * Immutable; validated and built by {@link ChatConversationDataLoader}.
 *
 * <p>If {@code repeatableStepsLists} is non-empty, the conversation enters a repeatable phase once its
 * base {@code steps} are finished: series are rolled by weight and played as continuations.
 */
public record ChatConversationDefinition(
        int schemaVersion,
        String id,
        String displayName,
        int displayPriority,
        String textureFile,
        boolean unlockedFromStart,
        String unlockingAdvancement, // nullable when unlockedFromStart
        List<ChatStepDefinition> steps,
        List<RepeatableSeriesDefinition> repeatableStepsLists) {

    public ChatStepDefinition step(int index) {
        if (index < 0 || index >= steps.size()) {
            return null;
        }
        return steps.get(index);
    }

    public boolean isLastStep(int index) {
        return index == steps.size() - 1;
    }

    public boolean usesRepeatables() {
        return repeatableStepsLists != null && !repeatableStepsLists.isEmpty();
    }

    /** Repeatable series by id, or {@code null}. */
    public RepeatableSeriesDefinition series(String seriesId) {
        if (seriesId == null || seriesId.isEmpty() || repeatableStepsLists == null) {
            return null;
        }
        for (RepeatableSeriesDefinition s : repeatableStepsLists) {
            if (s.id().equals(seriesId)) {
                return s;
            }
        }
        return null;
    }
}
