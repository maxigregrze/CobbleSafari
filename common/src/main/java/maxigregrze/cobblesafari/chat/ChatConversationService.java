package maxigregrze.cobblesafari.chat;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.data.ChatProgressSavedData;
import maxigregrze.cobblesafari.data.ChatProgressSavedData.Phase;
import maxigregrze.cobblesafari.data.ChatProgressSavedData.ProgressEntry;
import maxigregrze.cobblesafari.data.ChatProgressSavedData.ResolvedSeries;
import maxigregrze.cobblesafari.data.RotomPhoneUnlockSavedData;
import maxigregrze.cobblesafari.gts.GtsService;
import maxigregrze.cobblesafari.init.ModStats;
import maxigregrze.cobblesafari.network.ChatAppResultPayload;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneConfigSync;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinDefinition;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneSkinRegistry;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
 * Server-side logic for the Rotom Phone chat questlines.
 *
 * <p>A conversation runs through its base {@code steps}; if it declares {@code repeatableStepsLists},
 * it then plays weight-rolled repeatable series as continuations (see {@link RepeatableSeriesDefinition}).
 * The {@link ProgressEntry} tracks the active section (base or the active series) plus a history of
 * resolved series for the transcript.
 */
public final class ChatConversationService {

    private ChatConversationService() {}

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final String DEFAULT_FALLBACK = "cobblesafari:gifts/rotom_fallback";

    public enum ClaimResult { SUCCESS, NOT_FOUND, NOT_IN_TASK, NOT_COMPLETE }

    /** Resolved task progress for the bubble. {@code den == 0} = binary bar, no counter. */
    public record ProgressInfo(int num, int den, boolean done) {}

    // ---------------------------------------------------------------- active section helpers

    /** Steps of the active section: base steps while {@code !baseComplete}, else the active series (or empty = idle). */
    private static List<ChatStepDefinition> activeSteps(ChatConversationDefinition conv, ProgressEntry e) {
        if (!e.baseComplete) {
            return conv.steps();
        }
        if (e.activeSeriesId == null || e.activeSeriesId.isEmpty()) {
            return List.of();
        }
        RepeatableSeriesDefinition s = conv.series(e.activeSeriesId);
        return s == null ? List.of() : s.steps();
    }

    /**
     * Repairs an entry against the current definition (migration / datapack drift). Idempotent; sets
     * dirty only on change. Guarantees a coherent, playable state with no out-of-range indices.
     */
    private static void sanitize(ChatConversationDefinition conv, ProgressEntry e, ChatProgressSavedData data) {
        boolean dirty = false;
        if (e.activeSeriesId == null) {
            e.activeSeriesId = "";
            dirty = true;
        }
        // Old save that finished the base conversation in the pre-142 system.
        if (!e.baseComplete && e.phase == Phase.DONE) {
            e.baseComplete = true;
            e.activeSeriesId = "";
            dirty = true;
        }
        // Active series id no longer exists → drop to idle (a new one rolls at open/reset).
        if (e.baseComplete && !e.activeSeriesId.isEmpty() && conv.series(e.activeSeriesId) == null) {
            e.activeSeriesId = "";
            e.phase = Phase.DONE;
            e.claimed = true;
            e.messageIndex = 0;
            e.stepIndex = 0;
            dirty = true;
        }
        List<ChatStepDefinition> steps = activeSteps(conv, e);
        if (!steps.isEmpty()) {
            if (e.stepIndex >= steps.size()) {
                e.stepIndex = steps.size() - 1;
                dirty = true;
            }
            if (e.stepIndex < 0) {
                e.stepIndex = 0;
                dirty = true;
            }
        }
        if (dirty) {
            data.setDirty();
        }
    }

    // ---------------------------------------------------------------- state read

    /** Builds the per-player transcript snapshot for one conversation (null if unknown). */
    public static ChatAppResultPayload.StateData buildState(ServerPlayer player, String convId) {
        ChatConversationDefinition conv = ChatConversationRegistry.get(convId);
        if (conv == null) {
            return null;
        }
        ChatProgressSavedData data = ChatProgressSavedData.get(player.server);
        ProgressEntry e = data.getOrInit(player.getUUID(), convId);
        long today = LocalDate.now(ZONE).toEpochDay();
        RandomSource rng = player.server.overworld().getRandom();
        sanitize(conv, e, data);
        maybeRollIdle(conv, e, today, rng, data);

        List<ChatStepDefinition> active = activeSteps(conv, e);
        if (!active.isEmpty()) {
            applyStepStartEffects(player, active.get(Math.min(e.stepIndex, active.size() - 1)));
        }

        List<ChatAppResultPayload.StepView> views = new ArrayList<>();
        if (!e.baseComplete) {
            appendActiveSection(views, player, conv.steps(), e, data);
        } else {
            for (ChatStepDefinition step : conv.steps()) {
                appendCompletedStep(views, step);
            }
            for (ResolvedSeries rs : e.history) {
                if (rs.doDisapear && today > rs.resolvedEpochDay) {
                    continue; // hidden from the day after resolution
                }
                RepeatableSeriesDefinition s = conv.series(rs.seriesId);
                if (s == null) {
                    continue;
                }
                if (rs.completed) {
                    for (ChatStepDefinition step : s.steps()) {
                        appendCompletedStep(views, step);
                    }
                } else {
                    int failIdx = Math.min(Math.max(0, rs.stepReached), s.steps().size() - 1);
                    for (int i = 0; i < failIdx; i++) {
                        appendCompletedStep(views, s.steps().get(i));
                    }
                    appendFailedStep(views, s.steps().get(failIdx), s.failMessage());
                }
            }
            if (e.activeSeriesId != null && !e.activeSeriesId.isEmpty()) {
                RepeatableSeriesDefinition s = conv.series(e.activeSeriesId);
                if (s != null) {
                    appendActiveSection(views, player, s.steps(), e, data);
                }
            }
        }
        int currentIdx = Math.max(0, views.size() - 1);
        return new ChatAppResultPayload.StateData(convId, currentIdx, e.phase.ordinal(), e.claimed, views);
    }

    private static void appendActiveSection(List<ChatAppResultPayload.StepView> views, ServerPlayer player,
                                            List<ChatStepDefinition> steps, ProgressEntry e, ChatProgressSavedData data) {
        if (steps.isEmpty()) {
            return;
        }
        int current = Math.min(Math.max(0, e.stepIndex), steps.size() - 1);
        for (int i = 0; i <= current; i++) {
            ChatStepDefinition step = steps.get(i);
            if (i < current) {
                appendCompletedStep(views, step);
                continue;
            }
            String titleKey = taskTitleKey(step);
            int beforeSize = step.messagesBefore().size();
            int afterSize = step.messagesAfter().size();
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
                    beforeShown, afterShown, taskVisible, true, false));
        }
    }

    private static void appendCompletedStep(List<ChatAppResultPayload.StepView> views, ChatStepDefinition step) {
        views.add(new ChatAppResultPayload.StepView(
                step.messagesBefore(), step.messagesAfter(), taskTitleKey(step),
                1, 0, true, step.hasRewardItems(), step.hasRewardPersonalTrade(),
                step.messagesBefore().size(), step.messagesAfter().size(), true, false, false));
    }

    private static void appendFailedStep(List<ChatAppResultPayload.StepView> views, ChatStepDefinition step, String failMessage) {
        List<String> after = (failMessage == null || failMessage.isEmpty()) ? List.of() : List.of(failMessage);
        views.add(new ChatAppResultPayload.StepView(
                step.messagesBefore(), after, taskTitleKey(step),
                0, 0, false, false, false,
                step.messagesBefore().size(), after.size(), true, false, true));
    }

    /** Computes the task progress; lazily snapshots a stat-gated step's baseline if unset. */
    public static ProgressInfo computeProgress(ServerPlayer player, ChatStepDefinition step,
                                               ProgressEntry e, ChatProgressSavedData data) {
        if (step.isStatGated()) {
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
        if (step.isStatGated()) {
            ResourceLocation statId = ResourceLocation.tryParse(step.statistic());
            return statId == null ? "" : "stat." + statId.getNamespace() + "." + statId.getPath();
        }
        ResourceLocation advId = ResourceLocation.tryParse(step.advancement());
        if (advId == null) {
            return "";
        }
        String path = advId.getPath().replace('/', '.');
        String base = advId.getNamespace().equals("minecraft") ? path : advId.getNamespace() + "." + path;
        return "advancements." + base + ".title";
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
        sanitize(conv, e, data);
        List<ChatStepDefinition> steps = activeSteps(conv, e);
        if (steps.isEmpty()) {
            return;
        }
        ChatStepDefinition step = steps.get(Math.min(e.stepIndex, steps.size() - 1));
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
        sanitize(conv, e, data);
        List<ChatStepDefinition> steps = activeSteps(conv, e);
        if (steps.isEmpty() || e.phase != Phase.TASK || e.claimed) {
            return ClaimResult.NOT_IN_TASK;
        }
        ChatStepDefinition step = steps.get(Math.min(e.stepIndex, steps.size() - 1));
        ProgressInfo info = computeProgress(player, step, e, data);
        if (!info.done()) {
            return ClaimResult.NOT_COMPLETE;
        }
        giveRewards(player, step);
        e.claimed = true;
        e.phase = Phase.AFTER;
        e.messageIndex = 0;
        // isUnique counts as completed the moment the last reward of the series is obtained.
        if (e.baseComplete && !e.activeSeriesId.isEmpty() && e.stepIndex >= steps.size() - 1) {
            RepeatableSeriesDefinition s = conv.series(e.activeSeriesId);
            if (s != null && s.isUnique()) {
                e.completedUnique.add(e.activeSeriesId);
            }
        }
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
        sanitize(conv, e, data);
        if (e.phase != Phase.AFTER) {
            return;
        }
        List<ChatStepDefinition> steps = activeSteps(conv, e);
        if (steps.isEmpty()) {
            return;
        }
        ChatStepDefinition step = steps.get(Math.min(e.stepIndex, steps.size() - 1));
        if (step.waitNextDay()) {
            e.phase = Phase.WAIT_NEXT_DAY;
        } else {
            long today = LocalDate.now(ZONE).toEpochDay();
            advanceAfterStep(conv, e, today, player.server.overworld().getRandom());
        }
        data.setDirty();
    }

    // ---------------------------------------------------------------- section / series progression

    /** Advances past the current step; rolls into / between repeatable series, or finishes. */
    private static void advanceAfterStep(ChatConversationDefinition conv, ProgressEntry e, long today, RandomSource rng) {
        List<ChatStepDefinition> steps = activeSteps(conv, e);
        if (steps.isEmpty()) {
            return; // idle
        }
        int idx = e.stepIndex;
        if (idx + 1 < steps.size()) {
            e.stepIndex = idx + 1;
            startStep(steps.get(e.stepIndex), e);
            return;
        }
        if (!e.baseComplete) {
            e.baseComplete = true;
            if (conv.usesRepeatables()) {
                startRolledSeries(conv, e, today, rng);
            } else {
                finishIdle(conv, e);
            }
        } else {
            recordResolved(conv, e, true, today);
            startRolledSeries(conv, e, today, rng);
        }
    }

    /**
     * From the idle state, roll a series if one is eligible (replenishment / first-roll after migration).
     * Only mutates when a series is actually started, to avoid marking the save dirty on every poll when
     * the pool is genuinely exhausted.
     */
    private static void maybeRollIdle(ChatConversationDefinition conv, ProgressEntry e, long today,
                                      RandomSource rng, ChatProgressSavedData data) {
        if (e.baseComplete && (e.activeSeriesId == null || e.activeSeriesId.isEmpty())
                && conv.usesRepeatables() && e.phase == Phase.DONE) {
            String id = rollSeries(conv, e, rng);
            if (!id.isEmpty()) {
                e.activeSeriesId = id;
                e.seriesStartEpochDay = today;
                e.stepIndex = 0;
                startStep(conv.series(id).steps().get(0), e);
                data.setDirty();
            }
        }
    }

    private static void startRolledSeries(ChatConversationDefinition conv, ProgressEntry e, long today, RandomSource rng) {
        String id = rollSeries(conv, e, rng);
        if (id.isEmpty()) {
            finishIdle(conv, e);
            return;
        }
        e.activeSeriesId = id;
        e.seriesStartEpochDay = today;
        e.stepIndex = 0;
        startStep(conv.series(id).steps().get(0), e);
    }

    private static void finishIdle(ChatConversationDefinition conv, ProgressEntry e) {
        e.activeSeriesId = "";
        e.phase = Phase.DONE;
        e.messageIndex = 0;
        e.claimed = true;
        e.stepIndex = Math.max(0, conv.steps().size() - 1);
    }

    /** Weighted pick among eligible series (excludes completed uniques); {@code ""} if none eligible. */
    private static String rollSeries(ChatConversationDefinition conv, ProgressEntry e, RandomSource rng) {
        List<RepeatableSeriesDefinition> pool = new ArrayList<>();
        for (RepeatableSeriesDefinition s : conv.repeatableStepsLists()) {
            if (s.isUnique() && e.completedUnique.contains(s.id())) {
                continue;
            }
            pool.add(s);
        }
        if (pool.isEmpty()) {
            return "";
        }
        int total = 0;
        for (RepeatableSeriesDefinition s : pool) {
            total += s.weight();
        }
        if (total <= 0) {
            return "";
        }
        int r = rng.nextInt(total);
        for (RepeatableSeriesDefinition s : pool) {
            r -= s.weight();
            if (r < 0) {
                return s.id();
            }
        }
        return pool.get(pool.size() - 1).id();
    }

    private static void recordResolved(ChatConversationDefinition conv, ProgressEntry e, boolean completed, long today) {
        if (e.activeSeriesId == null || e.activeSeriesId.isEmpty()) {
            return;
        }
        RepeatableSeriesDefinition s = conv.series(e.activeSeriesId);
        ResolvedSeries rs = new ResolvedSeries();
        rs.seriesId = e.activeSeriesId;
        rs.completed = completed;
        rs.stepReached = e.stepIndex;
        rs.resolvedEpochDay = today;
        rs.doDisapear = s != null && s.doDisapear();
        e.appendHistory(rs, today);
        if (completed && s != null && s.isUnique()) {
            e.completedUnique.add(e.activeSeriesId);
        }
    }

    private static void startStep(ChatStepDefinition step, ProgressEntry e) {
        e.phase = Phase.BEFORE;
        e.messageIndex = 0;
        e.claimed = false;
        e.statBaseline = step.isStatGated() ? Long.MIN_VALUE : 0L; // lazy snapshot for stat-gated
    }

    /** Idempotent step-start side effects: unlock the app declared by the step, resync if changed. */
    private static void applyStepStartEffects(ServerPlayer player, ChatStepDefinition step) {
        if (step == null || !step.hasUnlockApp()) {
            return;
        }
        RotomPhoneUnlockSavedData store = RotomPhoneUnlockSavedData.get(player.server);
        if (store != null && store.unlockApp(player.getUUID(), step.unlockApp())) {
            RotomPhoneConfigSync.syncToPlayer(player);
        }
    }

    // ---------------------------------------------------------------- rewards

    private static void giveRewards(ServerPlayer player, ChatStepDefinition step) {
        MinecraftServer server = player.server;
        if (step.hasRewardItems()) {
            grantLootTable(player, step.rewardItems());
        }
        // Explicit (specific) rewards: attempted, but do not drive the tag fallback.
        if (step.hasRewardPersonalTrade()) {
            GtsService.AddPersonalOfferOutcome r =
                    GtsService.addPersonalOffer(server, player.getUUID(), step.rewardPersonalTrade());
            if (r.result() != GtsService.AddPersonalOfferResult.SUCCESS) {
                CobbleSafari.LOGGER.warn("[Chat] personal trade reward '{}' failed: {}",
                        step.rewardPersonalTrade(), r.result());
            }
        }
        if (step.hasRewardSkin()) {
            giveSpecificSkin(player, step.rewardSkin());
        }

        // Tag rewards: dedup-aware; if requested and none granted, fall back to a loot pool.
        boolean tagRequested = step.hasRewardSkinTag() || step.hasRewardPersonalTradeTag();
        boolean tagGranted = false;
        if (step.hasRewardPersonalTradeTag()) {
            tagGranted |= givePersonalTradeByTag(server, player, step.rewardPersonalTradeTag());
        }
        if (step.hasRewardSkinTag()) {
            tagGranted |= giveSkinByTag(player, step.rewardSkinTag());
        }
        if (tagRequested && !tagGranted) {
            grantLootTable(player, step.hasFallbackReward() ? step.fallbackReward() : DEFAULT_FALLBACK);
        }
    }

    private static boolean givePersonalTradeByTag(MinecraftServer server, ServerPlayer player, String tag) {
        GtsService.AddPersonalOfferOutcome r = GtsService.addPersonalOfferByTag(server, player.getUUID(), tag);
        if (r.result() != GtsService.AddPersonalOfferResult.SUCCESS) {
            CobbleSafari.LOGGER.info("[Chat] personal trade tag '{}' granted nothing ({})", tag, r.result());
            return false;
        }
        return true;
    }

    private static boolean giveSkinByTag(ServerPlayer player, String tag) {
        String skinId = pickRandomSkinForTag(player, tag);
        return skinId != null && unlockSkin(player, skinId);
    }

    private static void giveSpecificSkin(ServerPlayer player, String skinId) {
        if (RotomPhoneSkinRegistry.getSkin(skinId) == null) {
            CobbleSafari.LOGGER.warn("[Chat] reward skin '{}' is not a registered skin", skinId);
            return;
        }
        unlockSkin(player, skinId);
    }

    /** @return true if the skin was newly unlocked (false if already owned). */
    private static boolean unlockSkin(ServerPlayer player, String skinId) {
        RotomPhoneUnlockSavedData store = RotomPhoneUnlockSavedData.get(player.server);
        if (store != null && store.unlockSkin(player.getUUID(), skinId)) {
            RotomPhoneConfigSync.syncToPlayer(player);
            return true;
        }
        return false;
    }

    /** Random skin carrying {@code tag} not yet unlocked by the player; null if none remain. */
    private static String pickRandomSkinForTag(ServerPlayer player, String tag) {
        List<RotomPhoneSkinDefinition> pool = new ArrayList<>();
        for (RotomPhoneSkinDefinition skin : RotomPhoneSkinRegistry.getSkinsByTag(tag)) {
            if (!RotomPhoneSkinRegistry.isUnlockedByPlayer(player, skin)) {
                pool.add(skin);
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(player.serverLevel().getRandom().nextInt(pool.size())).getId();
    }

    private static void grantLootTable(ServerPlayer player, String tableIdStr) {
        ResourceLocation tableId = ResourceLocation.tryParse(tableIdStr);
        if (tableId == null) {
            CobbleSafari.LOGGER.warn("[Chat] invalid loot table id: {}", tableIdStr);
            return;
        }
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, tableId);
        LootTable table = player.server.reloadableRegistries().getLootTable(key);
        if (table == LootTable.EMPTY) {
            CobbleSafari.LOGGER.warn("[Chat] reward loot table not found: {}", tableId);
            return;
        }
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

    // ---------------------------------------------------------------- daily reset

    /** Natural daily rollover (used by the scheduler): never artificially forces timed-series expiry. */
    public static void onDailyReset(MinecraftServer server) {
        onDailyReset(server, false);
    }

    /**
     * Advances WAIT_NEXT_DAY steps, fails overdue timed series, and replenishes idle pools. Idempotent.
     *
     * @param forceTimedExpiry when {@code true}, every active incomplete timed series fails regardless of
     *     its start date (manual "roll to next day"); when {@code false}, only genuinely overdue series
     *     ({@code seriesStartEpochDay < today}) fail.
     */
    public static void onDailyReset(MinecraftServer server, boolean forceTimedExpiry) {
        ChatProgressSavedData data = ChatProgressSavedData.get(server);
        if (data == null) {
            return;
        }
        long today = LocalDate.now(ZONE).toEpochDay();
        RandomSource rng = server.overworld().getRandom();
        for (Map.Entry<UUID, Map<String, ProgressEntry>> pe : data.all().entrySet()) {
            for (Map.Entry<String, ProgressEntry> ce : pe.getValue().entrySet()) {
                ProgressEntry e = ce.getValue();
                ChatConversationDefinition conv = ChatConversationRegistry.get(ce.getKey());
                if (conv == null) {
                    continue;
                }
                sanitize(conv, e, data);

                // 1. timed series past their deadline → fail and roll the next.
                if (e.baseComplete && e.activeSeriesId != null && !e.activeSeriesId.isEmpty()) {
                    RepeatableSeriesDefinition s = conv.series(e.activeSeriesId);
                    if (s != null && s.isTimed()) {
                        boolean rewardComplete = e.stepIndex >= s.steps().size() - 1 && e.claimed;
                        boolean overdue = e.seriesStartEpochDay != Long.MIN_VALUE
                                && e.seriesStartEpochDay < today;
                        if (!rewardComplete && (forceTimedExpiry || overdue)) {
                            recordResolved(conv, e, false, today);
                            startRolledSeries(conv, e, today, rng);
                            e.lastResetEpochDay = today;
                            continue;
                        }
                    }
                }

                // 2. waitNextDay steps unblock.
                if (e.phase == Phase.WAIT_NEXT_DAY) {
                    advanceAfterStep(conv, e, today, rng);
                    e.lastResetEpochDay = today;
                    continue;
                }

                // 3. idle pool replenishment (e.g. datapack added series).
                if (e.baseComplete && (e.activeSeriesId == null || e.activeSeriesId.isEmpty())
                        && conv.usesRepeatables() && e.phase == Phase.DONE) {
                    startRolledSeries(conv, e, today, rng);
                    e.lastResetEpochDay = today;
                }
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
        int resetHour = maxigregrze.cobblesafari.config.MiscConfig.getDailySystemResetHour();
        boolean pastResetHour = LocalTime.now(ZONE).getHour() >= resetHour;
        boolean shouldRun = (todayEpoch > last && pastResetHour) || (todayEpoch > last + 1);
        if (!shouldRun) {
            return;
        }
        onDailyReset(server);
        data.setLastDailyResetEpochDay(todayEpoch);
    }
}
