package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.objectives.ObjectiveAssignment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent store of player → assignment-key → {@link ObjectiveAssignment} (plan 118 §1),
 * plus the last daily-reset epoch day for the non-instanced reroll scheduler.
 */
public class ObjectivesSavedData extends SavedData {

    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_dimensional_objectives";
    private static final String KEY_PLAYERS = "Players";
    private static final String KEY_LAST_RESET = "LastDailyResetEpochDay";

    private final Map<UUID, Map<String, ObjectiveAssignment>> byPlayer = new HashMap<>();
    private long lastDailyResetEpochDay = -1L;

    public ObjectivesSavedData() {
        // Required by the SavedData factory; state is populated in load().
    }

    public static ObjectivesSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ObjectivesSavedData data = new ObjectivesSavedData();
        data.lastDailyResetEpochDay = tag.contains(KEY_LAST_RESET) ? tag.getLong(KEY_LAST_RESET) : -1L;
        if (tag.contains(KEY_PLAYERS, Tag.TAG_LIST)) {
            ListTag players = tag.getList(KEY_PLAYERS, Tag.TAG_COMPOUND);
            for (int i = 0; i < players.size(); i++) {
                CompoundTag playerTag = players.getCompound(i);
                UUID uuid = playerTag.getUUID("Uuid");
                Map<String, ObjectiveAssignment> byKey = new LinkedHashMap<>();
                ListTag assigns = playerTag.getList("Assignments", Tag.TAG_COMPOUND);
                for (int j = 0; j < assigns.size(); j++) {
                    ObjectiveAssignment a = ObjectiveAssignment.fromNbt(assigns.getCompound(j));
                    byKey.put(a.key(), a);
                }
                if (!byKey.isEmpty()) {
                    data.byPlayer.put(uuid, byKey);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong(KEY_LAST_RESET, lastDailyResetEpochDay);
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Map<String, ObjectiveAssignment>> entry : byPlayer.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Uuid", entry.getKey());
            ListTag assigns = new ListTag();
            for (ObjectiveAssignment a : entry.getValue().values()) {
                assigns.add(a.toNbt());
            }
            playerTag.put("Assignments", assigns);
            players.add(playerTag);
        }
        tag.put(KEY_PLAYERS, players);
        return tag;
    }

    public static ObjectivesSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(ObjectivesSavedData::new, ObjectivesSavedData::load, null),
                DATA_NAME);
    }

    public long getLastDailyResetEpochDay() {
        return lastDailyResetEpochDay;
    }

    public void setLastDailyResetEpochDay(long epochDay) {
        this.lastDailyResetEpochDay = epochDay;
        setDirty();
    }

    public ObjectiveAssignment get(UUID player, String key) {
        Map<String, ObjectiveAssignment> byKey = byPlayer.get(player);
        return byKey == null ? null : byKey.get(key);
    }

    public void put(UUID player, ObjectiveAssignment assignment) {
        byPlayer.computeIfAbsent(player, k -> new LinkedHashMap<>()).put(assignment.key(), assignment);
        setDirty();
    }

    public void remove(UUID player, String key) {
        Map<String, ObjectiveAssignment> byKey = byPlayer.get(player);
        if (byKey != null && byKey.remove(key) != null) {
            if (byKey.isEmpty()) {
                byPlayer.remove(player);
            }
            setDirty();
        }
    }

    /** Removes the assignment for {@code key} from every player (instance purge, plan 118 §7). */
    public void removeKeyForAll(String key) {
        boolean changed = false;
        for (Map<String, ObjectiveAssignment> byKey : byPlayer.values()) {
            changed |= byKey.remove(key) != null;
        }
        byPlayer.values().removeIf(Map::isEmpty);
        if (changed) {
            setDirty();
        }
    }

    /** Snapshot of (player, assignment) pairs whose key does NOT contain an instance separator. */
    public List<PlayerAssignment> getNonInstancedAssignments() {
        List<PlayerAssignment> out = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, ObjectiveAssignment>> e : byPlayer.entrySet()) {
            for (ObjectiveAssignment a : e.getValue().values()) {
                if (a.instanceId() == null) {
                    out.add(new PlayerAssignment(e.getKey(), a));
                }
            }
        }
        return out;
    }

    public record PlayerAssignment(UUID player, ObjectiveAssignment assignment) {}
}
