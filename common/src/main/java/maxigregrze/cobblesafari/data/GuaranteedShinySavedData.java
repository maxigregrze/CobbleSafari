package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.power.GuaranteedShinyRequest;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class GuaranteedShinySavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_guaranteed_shiny";
    private static final String KEY_PLAYERS = "Players";

    private final Map<UUID, Map<String, GuaranteedShinyRequest>> requests = new HashMap<>();

    public GuaranteedShinySavedData() {
        // Required by the SavedData factory; state is populated in load().
    }

    public static GuaranteedShinySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GuaranteedShinySavedData data = new GuaranteedShinySavedData();
        if (tag.contains(KEY_PLAYERS, Tag.TAG_LIST)) {
            ListTag players = tag.getList(KEY_PLAYERS, Tag.TAG_COMPOUND);
            for (int i = 0; i < players.size(); i++) {
                CompoundTag playerTag = players.getCompound(i);
                UUID uuid = playerTag.getUUID("Uuid");
                Map<String, GuaranteedShinyRequest> byKey = new LinkedHashMap<>();
                ListTag reqList = playerTag.getList("Requests", Tag.TAG_COMPOUND);
                for (int j = 0; j < reqList.size(); j++) {
                    GuaranteedShinyRequest req = GuaranteedShinyRequest.fromNbt(reqList.getCompound(j));
                    byKey.put(req.key(), req);
                }
                if (!byKey.isEmpty()) {
                    data.requests.put(uuid, byKey);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Map<String, GuaranteedShinyRequest>> entry : requests.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Uuid", entry.getKey());
            ListTag reqList = new ListTag();
            for (GuaranteedShinyRequest req : entry.getValue().values()) {
                reqList.add(req.toNbt());
            }
            playerTag.put("Requests", reqList);
            players.add(playerTag);
        }
        tag.put(KEY_PLAYERS, players);
        return tag;
    }

    public static GuaranteedShinySavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(GuaranteedShinySavedData::new, GuaranteedShinySavedData::load, null),
                DATA_NAME);
    }

    public Map<String, GuaranteedShinyRequest> getRequests(UUID uuid) {
        Map<String, GuaranteedShinyRequest> byKey = requests.get(uuid);
        return byKey == null ? Map.of() : byKey;
    }

    public void put(UUID uuid, GuaranteedShinyRequest req) {
        requests.computeIfAbsent(uuid, k -> new LinkedHashMap<>()).put(req.key(), req);
        setDirty();
    }

    public void remove(UUID uuid, String key) {
        Map<String, GuaranteedShinyRequest> byKey = requests.get(uuid);
        if (byKey != null && byKey.remove(key) != null) {
            if (byKey.isEmpty()) {
                requests.remove(uuid);
            }
            setDirty();
        }
    }

    /**
     * Removes every request past its {@code expiryGameTime} across all players and drops the resulting
     * empty sub-maps. Requests without an expiry ({@code expiryGameTime < 0}) are left untouched. Returns
     * the number of requests removed (C7).
     */
    public int purgeExpired(long now) {
        int removed = 0;
        java.util.Iterator<Map.Entry<UUID, Map<String, GuaranteedShinyRequest>>> pit = requests.entrySet().iterator();
        while (pit.hasNext()) {
            Map<String, GuaranteedShinyRequest> byKey = pit.next().getValue();
            java.util.Iterator<GuaranteedShinyRequest> rit = byKey.values().iterator();
            while (rit.hasNext()) {
                GuaranteedShinyRequest req = rit.next();
                if (req.expiryGameTime() >= 0 && now > req.expiryGameTime()) {
                    rit.remove();
                    removed++;
                }
            }
            if (byKey.isEmpty()) {
                pit.remove();
            }
        }
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    public void removeByPrefix(UUID uuid, String prefix) {
        Map<String, GuaranteedShinyRequest> byKey = requests.get(uuid);
        if (byKey == null) {
            return;
        }
        if (byKey.keySet().removeIf(k -> k.startsWith(prefix))) {
            if (byKey.isEmpty()) {
                requests.remove(uuid);
            }
            setDirty();
        }
    }
}
