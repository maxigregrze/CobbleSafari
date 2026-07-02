package maxigregrze.cobblesafari.chat;

import java.util.List;

/**
 * A repeatable step-series declared inside a {@link ChatConversationDefinition}'s
 * {@code repeatableStepsLists}. Once the conversation's base steps are finished, one series is picked
 * at random (by {@code weight}) and played as a direct continuation of the conversation; when it
 * resolves, another is rolled.
 *
 * <ul>
 *   <li>{@code isUnique} — once completed (last reward obtained), this series is never offered again.</li>
 *   <li>{@code isTimed} — must be fully completed before the next daily reset after it starts, else it
 *       fails. The loader forbids {@code waitNextDay} steps inside a timed series.</li>
 *   <li>{@code failMessage} — lang key shown as a transition bubble when a timed series fails.</li>
 *   <li>{@code doDisapear} — once resolved, this series' messages are hidden from the transcript from
 *       the next daily reset on (kept in memory for {@code isUnique}, just not sent to the client).</li>
 * </ul>
 */
public record RepeatableSeriesDefinition(
        String id,
        int weight,
        boolean isUnique,
        boolean isTimed,
        String failMessage,
        boolean doDisapear,
        List<ChatStepDefinition> steps) {

    public boolean hasFailMessage() {
        return failMessage != null && !failMessage.isEmpty();
    }

    public ChatStepDefinition step(int index) {
        if (index < 0 || index >= steps.size()) {
            return null;
        }
        return steps.get(index);
    }
}
