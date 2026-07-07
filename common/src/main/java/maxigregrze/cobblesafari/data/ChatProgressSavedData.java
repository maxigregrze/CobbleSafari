package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player progress for the Rotom Phone chat questlines. Attached to the
 * Overworld storage like {@code StatProgressSavedData}; light and robust — entries whose conversation
 * id no longer exists are simply ignored (never purged destructively), and nothing here ever touches
 * the vanilla {@code player.dat} (so removing the mod cannot corrupt player data).
 */
public class ChatProgressSavedData extends SavedData {

    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_chat_progress";

    /** Message-streaming / questline phase for one conversation. Int values used on the wire. */
    public enum Phase {
        BEFORE, // 0 — streaming messagesBefore
        TASK, // 1 — task bubble shown, awaiting claim
        AFTER, // 2 — streaming messagesAfter
        WAIT_NEXT_DAY, // 3 — step done, waiting for the next reset
        DONE; // 4 — conversation finished

        public static Phase fromInt(int i) {
            Phase[] v = values();
            return (i >= 0 && i < v.length) ? v[i] : BEFORE;
        }
    }

    /**
     * Mutable progress record for a single (player, conversation) pair.
     *
     * <p>The {@code stepIndex/messageIndex/phase/claimed/statBaseline} fields track the <em>active
     * section</em>: the base conversation steps while {@code !baseComplete}, otherwise the active
     * repeatable series identified by {@code activeSeriesId} ({@code ""} = idle, no active series).
     */
    /** Upper bound on the transcript log kept per (player, conversation) to avoid unbounded growth (A3). */
    public static final int MAX_HISTORY = 50;

    public static final class ProgressEntry {
        public int stepIndex;
        public int messageIndex;
        public Phase phase = Phase.BEFORE;
        public boolean claimed;
        /** {@code Long.MIN_VALUE} = unset; lazily snapshotted on first progress read of a stat-gated step. */
        public long statBaseline = Long.MIN_VALUE;
        public long lastResetEpochDay = Long.MIN_VALUE;

        /** True once the base {@code steps} are finished (the conversation entered the repeatable phase). */
        public boolean baseComplete;
        /** Active repeatable series id, or {@code ""} for the base section / idle. */
        public String activeSeriesId = "";
        /** Epoch-day the active series started (for {@code isTimed}); {@code MIN_VALUE} = unset. */
        public long seriesStartEpochDay = Long.MIN_VALUE;
        /** Chronological log of resolved repeatable series (for the transcript + doDisapear). */
        public final List<ResolvedSeries> history = new ArrayList<>();
        /** Ids of {@code isUnique} series already completed (excluded from future rolls). */
        public final Set<String> completedUnique = new HashSet<>();

        /**
         * Appends a resolved series to the transcript while keeping {@link #history} bounded (A3):
         * first drops entries that are permanently hidden ({@code doDisapear} resolved on a prior day —
         * never rendered again), then trims the oldest entries beyond {@link #MAX_HISTORY}. The
         * {@link #completedUnique} set is tracked separately and is never affected.
         */
        public void appendHistory(ResolvedSeries rs, long today) {
            history.removeIf(h -> h.doDisapear && today > h.resolvedEpochDay);
            history.add(rs);
            trimHistory();
        }

        /** Trims the oldest transcript entries so {@link #history} never exceeds {@link #MAX_HISTORY}. */
        public void trimHistory() {
            while (history.size() > MAX_HISTORY) {
                history.remove(0);
            }
        }
    }

    /** A resolved (completed or failed) repeatable series instance, for rendering the transcript. */
    public static final class ResolvedSeries {
        public String seriesId = "";
        public boolean completed;
        /** Step index reached at resolution (the failed step for a failure). */
        public int stepReached;
        public long resolvedEpochDay = Long.MIN_VALUE;
        /** Frozen {@code doDisapear} flag of the series at resolution time. */
        public boolean doDisapear;
    }

    private final Map<UUID, Map<String, ProgressEntry>> byPlayer = new ConcurrentHashMap<>();
    private long lastDailyResetEpochDay = -1L;

    public ChatProgressSavedData() {
        // Required by the SavedData factory; state populated in load().
    }

    public ProgressEntry getOrInit(UUID playerId, String convId) {
        return byPlayer.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(convId, k -> new ProgressEntry());
    }

    /** Returns the entry or {@code null} if the player never opened this conversation. */
    public ProgressEntry peek(UUID playerId, String convId) {
        Map<String, ProgressEntry> m = byPlayer.get(playerId);
        return m == null ? null : m.get(convId);
    }

    public Map<UUID, Map<String, ProgressEntry>> all() {
        return byPlayer;
    }

    public long getLastDailyResetEpochDay() {
        return lastDailyResetEpochDay;
    }

    public void setLastDailyResetEpochDay(long epochDay) {
        this.lastDailyResetEpochDay = epochDay;
        setDirty();
    }

    public static ChatProgressSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ChatProgressSavedData data = new ChatProgressSavedData();
        data.lastDailyResetEpochDay = tag.contains("LastReset") ? tag.getLong("LastReset") : -1L;
        if (tag.contains("Players", Tag.TAG_COMPOUND)) {
            CompoundTag players = tag.getCompound("Players");
            for (String key : players.getAllKeys()) {
                UUID id;
                try {
                    id = UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {
                    continue; // corrupt/legacy key
                }
                Map<String, ProgressEntry> map = new ConcurrentHashMap<>();
                ListTag list = players.getList(key, Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag e = list.getCompound(i);
                    String convId = e.getString("Id");
                    if (convId.isEmpty()) {
                        continue;
                    }
                    ProgressEntry entry = new ProgressEntry();
                    entry.stepIndex = e.getInt("Step");
                    entry.messageIndex = e.getInt("Msg");
                    entry.phase = Phase.fromInt(e.getInt("Phase"));
                    entry.claimed = e.getBoolean("Claimed");
                    entry.statBaseline = e.getLong("Baseline");
                    entry.lastResetEpochDay = e.contains("StepReset") ? e.getLong("StepReset") : Long.MIN_VALUE;
                    // v2 fields (absent in pre-142 saves → defaults; the service repairs the rest).
                    entry.baseComplete = e.getBoolean("BaseDone");
                    entry.activeSeriesId = e.contains("Series") ? e.getString("Series") : "";
                    entry.seriesStartEpochDay = e.contains("SeriesStart") ? e.getLong("SeriesStart") : Long.MIN_VALUE;
                    if (e.contains("History", Tag.TAG_LIST)) {
                        ListTag hist = e.getList("History", Tag.TAG_COMPOUND);
                        for (int h = 0; h < hist.size(); h++) {
                            CompoundTag hc = hist.getCompound(h);
                            String sid = hc.getString("Id");
                            if (sid.isEmpty()) {
                                continue;
                            }
                            ResolvedSeries rs = new ResolvedSeries();
                            rs.seriesId = sid;
                            rs.completed = hc.getBoolean("Done");
                            rs.stepReached = hc.getInt("Step");
                            rs.resolvedEpochDay = hc.getLong("Day");
                            rs.doDisapear = hc.getBoolean("Hide");
                            entry.history.add(rs);
                        }
                        // Compact legacy oversized saves on first load (A3).
                        entry.trimHistory();
                    }
                    if (e.contains("Unique", Tag.TAG_LIST)) {
                        ListTag uniq = e.getList("Unique", Tag.TAG_STRING);
                        for (int u = 0; u < uniq.size(); u++) {
                            String sid = uniq.getString(u);
                            if (!sid.isEmpty()) {
                                entry.completedUnique.add(sid);
                            }
                        }
                    }
                    map.put(convId, entry);
                }
                if (!map.isEmpty()) {
                    data.byPlayer.put(id, map);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("LastReset", lastDailyResetEpochDay);
        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, Map<String, ProgressEntry>> pe : byPlayer.entrySet()) {
            ListTag list = new ListTag();
            for (Map.Entry<String, ProgressEntry> ce : pe.getValue().entrySet()) {
                ProgressEntry entry = ce.getValue();
                CompoundTag e = new CompoundTag();
                e.putString("Id", ce.getKey());
                e.putInt("Step", entry.stepIndex);
                e.putInt("Msg", entry.messageIndex);
                e.putInt("Phase", entry.phase.ordinal());
                e.putBoolean("Claimed", entry.claimed);
                e.putLong("Baseline", entry.statBaseline);
                e.putLong("StepReset", entry.lastResetEpochDay);
                e.putBoolean("BaseDone", entry.baseComplete);
                e.putString("Series", entry.activeSeriesId == null ? "" : entry.activeSeriesId);
                e.putLong("SeriesStart", entry.seriesStartEpochDay);
                if (!entry.history.isEmpty()) {
                    ListTag hist = new ListTag();
                    for (ResolvedSeries rs : entry.history) {
                        CompoundTag hc = new CompoundTag();
                        hc.putString("Id", rs.seriesId);
                        hc.putBoolean("Done", rs.completed);
                        hc.putInt("Step", rs.stepReached);
                        hc.putLong("Day", rs.resolvedEpochDay);
                        hc.putBoolean("Hide", rs.doDisapear);
                        hist.add(hc);
                    }
                    e.put("History", hist);
                }
                if (!entry.completedUnique.isEmpty()) {
                    ListTag uniq = new ListTag();
                    for (String sid : entry.completedUnique) {
                        uniq.add(net.minecraft.nbt.StringTag.valueOf(sid));
                    }
                    e.put("Unique", uniq);
                }
                list.add(e);
            }
            if (!list.isEmpty()) {
                players.put(pe.getKey().toString(), list);
            }
        }
        tag.put("Players", players);
        return tag;
    }

    public static ChatProgressSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return null;
        }
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(ChatProgressSavedData::new, ChatProgressSavedData::load, null),
                DATA_NAME);
    }

    /** Snapshot copy of a player's map (for safe iteration). */
    public Map<String, ProgressEntry> snapshotFor(UUID playerId) {
        Map<String, ProgressEntry> m = byPlayer.get(playerId);
        return m == null ? Map.of() : new HashMap<>(m);
    }
}
