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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player progress for the Rotom Phone chat questlines (cf. action plan 114 §3). Attached to the
 * Overworld storage like {@code StatProgressSavedData}; light and robust — entries whose conversation
 * id no longer exists are simply ignored (never purged destructively), and nothing here ever touches
 * the vanilla {@code player.dat} (so removing the mod cannot corrupt player data).
 */
public class ChatProgressSavedData extends SavedData {

    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_chat_progress";

    /** Message-streaming / questline phase for one conversation. Int values used on the wire. */
    public enum Phase {
        BEFORE,         // 0 — streaming messagesBefore
        TASK,           // 1 — task bubble shown, awaiting claim
        AFTER,          // 2 — streaming messagesAfter
        WAIT_NEXT_DAY,  // 3 — step done, waiting for the next reset
        DONE;           // 4 — conversation finished

        public static Phase fromInt(int i) {
            Phase[] v = values();
            return (i >= 0 && i < v.length) ? v[i] : BEFORE;
        }
    }

    /** Mutable progress record for a single (player, conversation) pair. */
    public static final class ProgressEntry {
        public int stepIndex;
        public int messageIndex;
        public Phase phase = Phase.BEFORE;
        public boolean claimed;
        /** {@code Long.MIN_VALUE} = unset; lazily snapshotted on first progress read of a repeatable step. */
        public long statBaseline = Long.MIN_VALUE;
        public long lastResetEpochDay = Long.MIN_VALUE;
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
