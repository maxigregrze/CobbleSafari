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
 * Per-player Rotom Phone unlock state: which apps and which skins a player has unlocked through a
 * channel that is <em>not</em> "enabled by default" / "unlocked from start" / advancement — i.e. via
 * a consumable item, a chat questline step, or an admin command.
 *
 * <p>Attached to the Overworld storage like {@code ChatProgressSavedData}; never touches the vanilla
 * {@code player.dat}. Entries referencing an app/skin id that no longer exists are simply ignored at
 * read-time (never purged destructively).
 */
public class RotomPhoneUnlockSavedData extends SavedData {

    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_rotomphone_unlocks";

    private final Map<UUID, Set<String>> unlockedApps = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> unlockedSkins = new ConcurrentHashMap<>();

    public RotomPhoneUnlockSavedData() {
        // Required by the SavedData factory; state populated in load().
    }

    // ---------------------------------------------------------------- apps

    /** @return {@code true} if the set actually changed (so callers only resync when needed). */
    public boolean unlockApp(UUID playerId, String appId) {
        boolean changed = unlockedApps.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(appId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean lockApp(UUID playerId, String appId) {
        Set<String> set = unlockedApps.get(playerId);
        boolean changed = set != null && set.remove(appId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean isAppUnlocked(UUID playerId, String appId) {
        Set<String> set = unlockedApps.get(playerId);
        return set != null && set.contains(appId);
    }

    public Set<String> getUnlockedApps(UUID playerId) {
        return new HashSet<>(unlockedApps.getOrDefault(playerId, Set.of()));
    }

    // ---------------------------------------------------------------- skins

    public boolean unlockSkin(UUID playerId, String skinId) {
        boolean changed = unlockedSkins.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(skinId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean lockSkin(UUID playerId, String skinId) {
        Set<String> set = unlockedSkins.get(playerId);
        boolean changed = set != null && set.remove(skinId);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean isSkinUnlocked(UUID playerId, String skinId) {
        Set<String> set = unlockedSkins.get(playerId);
        return set != null && set.contains(skinId);
    }

    public Set<String> getUnlockedSkins(UUID playerId) {
        return new HashSet<>(unlockedSkins.getOrDefault(playerId, Set.of()));
    }

    // ---------------------------------------------------------------- persistence

    public static RotomPhoneUnlockSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        RotomPhoneUnlockSavedData data = new RotomPhoneUnlockSavedData();
        readSection(tag, "Apps", data.unlockedApps);
        readSection(tag, "Skins", data.unlockedSkins);
        return data;
    }

    private static void readSection(CompoundTag tag, String key, Map<UUID, Set<String>> into) {
        if (!tag.contains(key, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag section = tag.getCompound(key);
        for (String uuidKey : section.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ignored) {
                continue; // corrupt/legacy key
            }
            Set<String> set = ConcurrentHashMap.newKeySet();
            ListTag list = section.getList(uuidKey, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String value = list.getString(i);
                if (!value.isEmpty()) {
                    set.add(value);
                }
            }
            if (!set.isEmpty()) {
                into.put(id, set);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Apps", writeSection(unlockedApps));
        tag.put("Skins", writeSection(unlockedSkins));
        return tag;
    }

    private static CompoundTag writeSection(Map<UUID, Set<String>> from) {
        CompoundTag section = new CompoundTag();
        for (Map.Entry<UUID, Set<String>> entry : from.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            ListTag list = new ListTag();
            for (String value : entry.getValue()) {
                list.add(StringTag.valueOf(value));
            }
            section.put(entry.getKey().toString(), list);
        }
        return section;
    }

    public static RotomPhoneUnlockSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return null;
        }
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(RotomPhoneUnlockSavedData::new, RotomPhoneUnlockSavedData::load, null),
                DATA_NAME);
    }
}
