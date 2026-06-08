package maxigregrze.cobblesafari.chat;

import java.util.List;

/**
 * One step of a {@link ChatConversationDefinition} questline (cf. action plan 114 §2.2).
 *
 * <p>A step is either <em>advancement-gated</em> (non repeatable, progress = advancement criteria
 * ratio) or <em>statistic-gated</em> (repeatable, progress = stat delta from a per-player baseline).
 * The loader guarantees the relevant fields are present (§2.3.1), so {@code advancement} is non-null
 * for non-repeatable steps and {@code statistic}/{@code statisticAmount} are valid for repeatable ones.
 */
public record ChatStepDefinition(
        List<String> messagesBefore,
        List<String> messagesAfter,
        String advancement,          // nullable (repeatable steps have none)
        String statistic,            // nullable (only repeatable steps)
        int statisticAmount,         // only meaningful when repeatable
        String rewardItems,          // nullable loot-table id
        String rewardPersonalTrade,  // nullable GTS unique-offer template id
        boolean waitNextDay,
        boolean repeatable) {

    public boolean hasAdvancement() {
        return advancement != null && !advancement.isEmpty();
    }

    public boolean hasRewardItems() {
        return rewardItems != null && !rewardItems.isEmpty();
    }

    public boolean hasRewardPersonalTrade() {
        return rewardPersonalTrade != null && !rewardPersonalTrade.isEmpty();
    }
}
