package maxigregrze.cobblesafari.chat;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.WonderTradeSettings;
import maxigregrze.cobblesafari.data.ChatProgressSavedData;
import maxigregrze.cobblesafari.data.ChatProgressSavedData.Phase;
import maxigregrze.cobblesafari.data.ChatProgressSavedData.ProgressEntry;
import maxigregrze.cobblesafari.gts.GtsService;
import maxigregrze.cobblesafari.init.ModStats;
import maxigregrze.cobblesafari.network.ChatAppResultPayload;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side logic for the Rotom Phone chat questlines (cf. action plan 114 §5-7, §11).
 */
public final class ChatConversationService {

    private ChatConversationService() {}

    private static final ZoneId ZONE = ZoneId.systemDefault();

    public enum ClaimResult { SUCCESS, NOT_FOUND, NOT_IN_TASK, NOT_COMPLETE }

    /** Resolved task progress for the bubble. {@code den == 0} = binary bar, no counter (§11.1). */
    public record ProgressInfo(int num, int den, boolean done) {}

    // ---------------------------------------------------------------- state read

    /**
     * Builds the per-player transcript snapshot for one conversation (null if unknown). Carries every
     * step from 0 up to the current one so the full history stays visible (only the current is partial).
     */
    public static ChatAppResultPayload.StateData buildState(ServerPlayer player, String convId) {
        ChatConversationDefinition conv = ChatConversationRegistry.get(convId);
        if (conv == null) {
            return null;
        }
        ChatProgressSavedData data = ChatProgressSavedData.get(player.server);
        ProgressEntry e = data.getOrInit(player.getUUID(), convId);

        int current = Math.min(e.stepIndex, conv.steps().size() - 1);
        List<ChatAppResultPayload.StepView> views = new ArrayList<>();
        for (int i = 0; i <= current; i++) {
            ChatStepDefinition step = conv.step(i);
            String titleKey = taskTitleKey(step);
            int beforeSize = step.messagesBefore().size();
            int afterSize = step.messagesAfter().size();
            if (i < current) {
                // Fully-shown, completed past step.
                views.add(new ChatAppResultPayload.StepView(
                        step.messagesBefore(), step.messagesAfter(), titleKey,
                        1, 0, true, step.hasRewardItems(), step.hasRewardPersonalTrade(),
                        beforeSize, afterSize, true, false));
            } else {
                ProgressInfo info = computeProgress(player, step, e, data);
                int phase = e.phase.ordinal();
                int beforeShown;
                int afterShown;
                boolean taskVisible;
                boolean done;
                if (phase == Phase.BEFORE.ordinal()) {
                    beforeShown = Math.min(e.messageIndex, beforeSize);
                    afterShown = 0;
                    taskVisible = false;
                    done = info.done();
                } else if (phase == Phase.TASK.ordinal()) {
                    beforeShown = beforeSize;
                    afterShown = 0;
                    taskVisible = true;
                    done = info.done();
                } else if (phase == Phase.AFTER.ordinal()) {
                    beforeShown = beforeSize;
                    afterShown = Math.min(e.messageIndex, afterSize);
                    taskVisible = true;
                    done = true;
                } else { // WAIT_NEXT_DAY / DONE
                    beforeShown = beforeSize;
                    afterShown = afterSize;
                    taskVisible = true;
                    done = true;
                }
                views.add(new ChatAppResultPayload.StepView(
                        step.messagesBefore(), step.messagesAfter(), titleKey,
                        info.num(), info.den(), done, step.hasRewardItems(), step.hasRewardPersonalTrade(),
                        beforeShown, afterShown, taskVisible, true));
            }
        }
        return new ChatAppResultPayload.StateData(convId, current, e.phase.ordinal(), e.claimed, views);
    }

    /** Computes the task progress; lazily snapshots a repeatable step's baseline if unset. */
    public static ProgressInfo computeProgress(ServerPlayer player, ChatStepDefinition step,
                                               ProgressEntry e, ChatProgressSavedData data) {
        if (step.repeatable()) {
            // Resolve the *registered* stat id instance: Stats.CUSTOM / the registry use identity
            // lookups, so a freshly parsed ResourceLocation must be canonicalised via the registry.
            ResourceLocation parsed = ResourceLocation.tryParse(step.statistic());
            ResourceLocation statId = parsed == null ? null : BuiltInRegistries.CUSTOM_STAT.get(parsed);
            if (statId == null) {
                CobbleSafari.LOGGER.warn("[Chat] statistic '{}' is not a registered custom stat", step.statistic());
                return new ProgressInfo(0, step.statisticAmount(), false);
            }
            long now = ModStats.value(player, statId);
            if (e.statBaseline == Long.MIN_VALUE) {
                e.statBaseline = now;
                data.setDirty();
            }
            long cur = Math.max(0L, now - e.statBaseline);
            int amount = step.statisticAmount();
            int num = (int) Math.min(cur, amount);
            return new ProgressInfo(num, amount, cur >= amount);
        }

        // Non-repeatable: advancement criteria ratio, binary if mono-criterion (§11.1).
        ResourceLocation advId = ResourceLocation.tryParse(step.advancement());
        if (advId == null) {
            return new ProgressInfo(0, 0, false);
        }
        AdvancementHolder holder = player.server.getAdvancements().get(advId);
        if (holder == null) {
            CobbleSafari.LOGGER.warn("[Chat] advancement '{}' referenced by a conversation does not exist", advId);
            return new ProgressInfo(0, 0, false);
        }
        AdvancementProgress pr = player.getAdvancements().getOrStartProgress(holder);
        int completed = count(pr.getCompletedCriteria());
        int total = completed + count(pr.getRemainingCriteria());
        boolean done = pr.isDone();
        if (total > 1) {
            return new ProgressInfo(completed, total, done);
        }
        // mono-criterion: binary, no counter text (den == 0)
        return new ProgressInfo(done ? 1 : 0, 0, done);
    }

    private static int count(Iterable<String> it) {
        int n = 0;
        for (String ignored : it) {
            n++;
        }
        return n;
    }

    private static String taskTitleKey(ChatStepDefinition step) {
        if (step.repeatable()) {
            ResourceLocation statId = ResourceLocation.tryParse(step.statistic());
            return statId == null ? "" : "stat." + statId.getNamespace() + "." + statId.getPath();
        }
        ResourceLocation advId = ResourceLocation.tryParse(step.advancement());
        return advId == null ? "" : "advancement." + advId.getNamespace() + "." + advId.getPath() + ".title";
    }

    // ---------------------------------------------------------------- mutations

    /** Persists the revealed-message position; flips BEFORE→TASK once all before-messages are shown. */
    public static void setMessageIndex(ServerPlayer player, String convId, int newIndex) {
        ChatConversationDefinition conv = ChatConversationRegistry.get(convId);
        if (conv == null) {
            return;
        }
        ChatProgressSavedData data = ChatProgressSavedData.get(player.server);
        ProgressEntry e = data.getOrInit(player.getUUID(), convId);
        ChatStepDefinition step = conv.step(Math.min(e.stepIndex, conv.steps().size() - 1));
        if (e.phase == Phase.BEFORE) {
            int max = step.messagesBefore().size();
            if (newIndex >= max) {
                e.phase = Phase.TASK;
                e.messageIndex = 0;
            } else {
                e.messageIndex = Math.max(0, newIndex);
            }
            data.setDirty();
        } else if (e.phase == Phase.AFTER) {
            int max = step.messagesAfter().size();
            e.messageIndex = Math.max(0, Math.min(newIndex, max));
            data.setDirty();
        }
    }

    public static ClaimResult tryClaim(ServerPlayer player, String convId) {
        ChatConversationDefinition conv = ChatConversationRegistry.get(convId);
        if (conv == null) {
            return ClaimResult.NOT_FOUND;
        }
        ChatProgressSavedData data = ChatProgressSavedData.get(player.server);
        ProgressEntry e = data.getOrInit(player.getUUID(), convId);
        if (e.phase != Phase.TASK || e.claimed) {
            return ClaimResult.NOT_IN_TASK;
        }
        ChatStepDefinition step = conv.step(Math.min(e.stepIndex, conv.steps().size() - 1));
        ProgressInfo info = computeProgress(player, step, e, data);
        if (!info.done()) {
            return ClaimResult.NOT_COMPLETE;
        }
        giveRewards(player, step);
        e.claimed = true;
        e.phase = Phase.AFTER;
        e.messageIndex = 0;
        data.setDirty();
        return ClaimResult.SUCCESS;
    }

    /** Called once the client finished streaming the AFTER messages. */
    public static void onAfterMessagesDone(ServerPlayer player, String convId) {
        ChatConversationDefinition conv = ChatConversationRegistry.get(convId);
        if (conv == null) {
            return;
        }
        ChatProgressSavedData data = ChatProgressSavedData.get(player.server);
        ProgressEntry e = data.getOrInit(player.getUUID(), convId);
        if (e.phase != Phase.AFTER) {
            return;
        }
        ChatStepDefinition step = conv.step(Math.min(e.stepIndex, conv.steps().size() - 1));
        if (step.waitNextDay()) {
            e.phase = Phase.WAIT_NEXT_DAY;
        } else {
            advanceAfterStep(conv, e);
        }
        data.setDirty();
    }

    /** BEFORE → next step / repeat / DONE (cf. §5.3). */
    private static void advanceAfterStep(ChatConversationDefinition conv, ProgressEntry e) {
        int idx = e.stepIndex;
        ChatStepDefinition cur = conv.step(Math.min(idx, conv.steps().size() - 1));
        if (idx + 1 < conv.steps().size()) {
            e.stepIndex = idx + 1;
            startStep(conv.step(e.stepIndex), e);
        } else if (cur.repeatable()) {
            startStep(cur, e); // re-run the same (last) step
        } else {
            e.phase = Phase.DONE;
            e.messageIndex = 0;
            e.claimed = true;
        }
    }

    private static void startStep(ChatStepDefinition step, ProgressEntry e) {
        e.phase = Phase.BEFORE;
        e.messageIndex = 0;
        e.claimed = false;
        e.statBaseline = step.repeatable() ? Long.MIN_VALUE : 0L; // lazy re-snapshot for repeatable
    }

    private static void giveRewards(ServerPlayer player, ChatStepDefinition step) {
        MinecraftServer server = player.server;
        if (step.hasRewardItems()) {
            ResourceLocation tableId = ResourceLocation.tryParse(step.rewardItems());
            if (tableId != null) {
                ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, tableId);
                LootTable table = server.reloadableRegistries().getLootTable(key);
                if (table == LootTable.EMPTY) {
                    CobbleSafari.LOGGER.warn("[Chat] reward loot table not found: {}", tableId);
                } else {
                    ServerLevel level = player.serverLevel();
                    LootParams params = new LootParams.Builder(level)
                            .withParameter(LootContextParams.ORIGIN, player.position())
                            .withParameter(LootContextParams.THIS_ENTITY, player)
                            .create(LootContextParamSets.GIFT);
                    for (ItemStack stack : table.getRandomItems(params)) {
                        if (stack.isEmpty()) {
                            continue;
                        }
                        if (!player.getInventory().add(stack) || !stack.isEmpty()) {
                            player.drop(stack, false);
                        }
                    }
                }
            }
        }
        if (step.hasRewardPersonalTrade()) {
            GtsService.AddPersonalOfferOutcome r =
                    GtsService.addPersonalOffer(server, player.getUUID(), step.rewardPersonalTrade());
            if (r.result() != GtsService.AddPersonalOfferResult.SUCCESS) {
                CobbleSafari.LOGGER.warn("[Chat] personal trade reward '{}' failed: {}",
                        step.rewardPersonalTrade(), r.result());
            }
        }
    }

    // ---------------------------------------------------------------- daily reset (waitNextDay)

    /** Unblocks every WAIT_NEXT_DAY entry (online or not). Idempotent. */
    public static void onDailyReset(MinecraftServer server) {
        ChatProgressSavedData data = ChatProgressSavedData.get(server);
        if (data == null) {
            return;
        }
        long today = LocalDate.now(ZONE).toEpochDay();
        for (Map.Entry<UUID, Map<String, ProgressEntry>> pe : data.all().entrySet()) {
            for (Map.Entry<String, ProgressEntry> ce : pe.getValue().entrySet()) {
                ProgressEntry e = ce.getValue();
                if (e.phase != Phase.WAIT_NEXT_DAY) {
                    continue;
                }
                ChatConversationDefinition conv = ChatConversationRegistry.get(ce.getKey());
                if (conv == null) {
                    continue;
                }
                advanceAfterStep(conv, e);
                e.lastResetEpochDay = today;
            }
        }
        data.setDirty();
    }

    /** Daily scheduler mirroring {@code GtsService.tickDailyScheduler} (configurable reset hour). */
    public static void tickDailyScheduler(MinecraftServer server) {
        ChatProgressSavedData data = ChatProgressSavedData.get(server);
        if (data == null) {
            return;
        }
        long todayEpoch = LocalDate.now(ZONE).toEpochDay();
        long last = data.getLastDailyResetEpochDay();
        if (last < 0) {
            data.setLastDailyResetEpochDay(todayEpoch - 1);
            return;
        }
        int resetHour = WonderTradeSettings.get().getResetHour();
        boolean pastResetHour = LocalTime.now(ZONE).getHour() >= resetHour;
        boolean shouldRun = (todayEpoch > last && pastResetHour) || (todayEpoch > last + 1);
        if (!shouldRun) {
            return;
        }
        onDailyReset(server);
        data.setLastDailyResetEpochDay(todayEpoch);
    }
}
