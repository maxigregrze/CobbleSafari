package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player "set" progress backing the achievements that count <em>distinct</em> elements
 * (cf. action plan 94 §5): unique Pokémon species captured in the Safari, and distinct days
 * the player first entered the Safari. Vanilla advancement state does not retain this history,
 * so this {@link SavedData} (attached to the Overworld storage) is the source of truth.
 */
public class StatProgressSavedData extends SavedData {

    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_stat_progress";
    private static final String KEY_SPECIES = "Species";

    private final Map<UUID, Set<String>> safariSpecies = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Long>> safariDays = new ConcurrentHashMap<>();

    public StatProgressSavedData() {
        // Required by the SavedData factory; state is populated in load().
    }

    /** Records a captured species. Returns the new distinct-species count if it was new, else -1. */
    public int recordSafariSpecies(UUID playerId, String speciesId) {
        Set<String> set = safariSpecies.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        if (set.add(speciesId)) {
            setDirty();
            return set.size();
        }
        return -1;
    }

    /** Records a Safari entry day. Returns the new distinct-day count if it was new, else -1. */
    public int recordSafariDay(UUID playerId, long epochDay) {
        Set<Long> set = safariDays.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        if (set.add(epochDay)) {
            setDirty();
            return set.size();
        }
        return -1;
    }

    public static StatProgressSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        StatProgressSavedData data = new StatProgressSavedData();
        if (tag.contains(KEY_SPECIES, Tag.TAG_COMPOUND)) {
            CompoundTag species = tag.getCompound(KEY_SPECIES);
            for (String key : species.getAllKeys()) {
                try {
                    UUID id = UUID.fromString(key);
                    Set<String> set = ConcurrentHashMap.newKeySet();
                    ListTag list = species.getList(key, Tag.TAG_STRING);
                    for (int i = 0; i < list.size(); i++) {
                        set.add(list.getString(i));
                    }
                    data.safariSpecies.put(id, set);
                } catch (IllegalArgumentException ignored) {
                    // Skip entries whose key is not a valid UUID (corrupt/legacy data).
                }
            }
        }
        if (tag.contains("Days", Tag.TAG_COMPOUND)) {
            CompoundTag days = tag.getCompound("Days");
            for (String key : days.getAllKeys()) {
                try {
                    UUID id = UUID.fromString(key);
                    Set<Long> set = ConcurrentHashMap.newKeySet();
                    for (long d : days.getLongArray(key)) {
                        set.add(d);
                    }
                    data.safariDays.put(id, set);
                } catch (IllegalArgumentException ignored) {
                    // Skip entries whose key is not a valid UUID (corrupt/legacy data).
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag species = new CompoundTag();
        for (Map.Entry<UUID, Set<String>> e : safariSpecies.entrySet()) {
            ListTag list = new ListTag();
            for (String s : e.getValue()) {
                list.add(StringTag.valueOf(s));
            }
            species.put(e.getKey().toString(), list);
        }
        tag.put(KEY_SPECIES, species);

        CompoundTag days = new CompoundTag();
        for (Map.Entry<UUID, Set<Long>> e : safariDays.entrySet()) {
            long[] arr = e.getValue().stream().mapToLong(Long::longValue).toArray();
            days.putLongArray(e.getKey().toString(), arr);
        }
        tag.put("Days", days);
        return tag;
    }

    public static StatProgressSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return null;
        }
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(StatProgressSavedData::new, StatProgressSavedData::load, null),
                DATA_NAME);
    }
}
