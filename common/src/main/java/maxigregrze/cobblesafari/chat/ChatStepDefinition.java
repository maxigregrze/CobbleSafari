package maxigregrze.cobblesafari.chat;

import java.util.List;

/**
 * One step of a {@link ChatConversationDefinition} questline (base step or a step of a
 * {@link RepeatableSeriesDefinition}).
 *
 * <p>Gating is decided by which field is present: a step is <em>statistic-gated</em> when
 * {@code statistic} is set (progress = stat delta from a per-player baseline, re-snapshotted each time
 * the step becomes active), otherwise <em>advancement-gated</em> (progress = advancement criteria ratio).
 *
 * <p>{@code unlockApp} is applied at the start of the step; {@code rewardSkin}/{@code rewardSkinTag}/
 * {@code rewardPersonalTrade}/{@code rewardPersonalTradeTag} are granted on claim. When a tag-based
 * reward is requested but the pool is exhausted, {@code fallbackReward} (a loot-table id) is granted
 * instead (defaulting to the global rotom fallback pool).
 */
public record ChatStepDefinition(
        List<String> messagesBefore,
        List<String> messagesAfter,
        String advancement, // nullable (statistic-gated steps have none)
        String statistic, // nullable (only statistic-gated steps)
        int statisticAmount, // only meaningful when statistic-gated
        String rewardItems, // nullable loot-table id (always granted)
        String rewardPersonalTrade, // nullable GTS unique-offer template id
        String rewardPersonalTradeTag, // nullable GTS unique-offer tag (random pick) granted on claim
        String unlockApp, // nullable rotom-phone app id unlocked at the start of this step
        String rewardSkin, // nullable rotom-phone skin id granted on claim
        String rewardSkinTag, // nullable rotom-phone skin tag (random pick) granted on claim
        String fallbackReward, // nullable loot-table id used when a tag reward is exhausted
        boolean waitNextDay) {

    public boolean isStatGated() {
        return statistic != null && !statistic.isEmpty();
    }

    public boolean hasAdvancement() {
        return advancement != null && !advancement.isEmpty();
    }

    public boolean hasRewardItems() {
        return rewardItems != null && !rewardItems.isEmpty();
    }

    public boolean hasRewardPersonalTrade() {
        return rewardPersonalTrade != null && !rewardPersonalTrade.isEmpty();
    }

    public boolean hasRewardPersonalTradeTag() {
        return rewardPersonalTradeTag != null && !rewardPersonalTradeTag.isEmpty();
    }

    public boolean hasUnlockApp() {
        return unlockApp != null && !unlockApp.isEmpty();
    }

    public boolean hasRewardSkin() {
        return rewardSkin != null && !rewardSkin.isEmpty();
    }

    public boolean hasRewardSkinTag() {
        return rewardSkinTag != null && !rewardSkinTag.isEmpty();
    }

    public boolean hasFallbackReward() {
        return fallbackReward != null && !fallbackReward.isEmpty();
    }
}
