package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimerSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_timer_data";

    private final Map<UUID, Map<String, PlayerTimerData>> playerData = new HashMap<>();

    public TimerSavedData() {
    }

    public static TimerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TimerSavedData data = new TimerSavedData();

        if (tag.contains("players", Tag.TAG_LIST)) {
            ListTag playerList = tag.getList("players", Tag.TAG_COMPOUND);
            for (int i = 0; i < playerList.size(); i++) {
                CompoundTag playerTag = playerList.getCompound(i);
                PlayerTimerData playerTimerData = PlayerTimerData.fromNbt(playerTag);
                UUID playerId = playerTimerData.getPlayerId();
                String dimensionId = playerTimerData.getDimensionId();
                
                data.playerData
                        .computeIfAbsent(playerId, k -> new HashMap<>())
                        .put(dimensionId, playerTimerData);
            }
        }

        int totalTimers = data.playerData.values().stream()
                .mapToInt(Map::size)
                .sum();
        CobbleSafari.LOGGER.info("Loaded timer data: {} players, {} timers", data.playerData.size(), totalTimers);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag playerList = new ListTag();

        for (Map<String, PlayerTimerData> dimensionTimers : playerData.values()) {
            for (PlayerTimerData timerData : dimensionTimers.values()) {
                playerList.add(timerData.toNbt());
            }
        }

        tag.put("players", playerList);
        int totalTimers = playerData.values().stream()
                .mapToInt(Map::size)
                .sum();
        CobbleSafari.LOGGER.info("Saved timer data: {} players, {} timers", playerData.size(), totalTimers);
        return tag;
    }

    public PlayerTimerData getPlayerData(UUID playerId, String dimensionId) {
        Map<String, PlayerTimerData> dimensionTimers = playerData.get(playerId);
        if (dimensionTimers == null) {
            return null;
        }
        return dimensionTimers.get(dimensionId);
    }

    @Deprecated
    public PlayerTimerData getPlayerData(UUID playerId) {
        return getPlayerData(playerId, SafariTimerConfig.getSafariDimensionId());
    }

    public PlayerTimerData getOrCreatePlayerData(UUID playerId, String dimensionId) {
        return playerData
                .computeIfAbsent(playerId, k -> new HashMap<>())
                .computeIfAbsent(dimensionId, d -> new PlayerTimerData(playerId, dimensionId));
    }

    @Deprecated
    public PlayerTimerData getOrCreatePlayerData(UUID playerId) {
        return getOrCreatePlayerData(playerId, SafariTimerConfig.getSafariDimensionId());
    }

    public void setPlayerData(UUID playerId, String dimensionId, PlayerTimerData data) {
        playerData
                .computeIfAbsent(playerId, k -> new HashMap<>())
                .put(dimensionId, data);
        setDirty();
    }

    @Deprecated
    public void setPlayerData(UUID playerId, PlayerTimerData data) {
        setPlayerData(playerId, data.getDimensionId(), data);
    }

    public Map<String, PlayerTimerData> getPlayerDimensionTimers(UUID playerId) {
        return playerData.getOrDefault(playerId, new HashMap<>());
    }

    public void resetDimensionTimers(String dimensionId) {
        for (Map<String, PlayerTimerData> dimensionTimers : playerData.values()) {
            PlayerTimerData data = dimensionTimers.get(dimensionId);
            if (data != null) {
                data.reset();
                data.setLastSafariBallGrantDay(0);
                data.resetEntryFeePayDay();
                data.setActive(false);
            }
        }
        setDirty();
    }

    public Map<UUID, Map<String, PlayerTimerData>> getAllPlayerData() {
        return playerData;
    }

    public static TimerSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(createFactory(), DATA_NAME);
    }

    private static Factory<TimerSavedData> createFactory() {
        return new Factory<>(TimerSavedData::new, TimerSavedData::load, null);
    }
}
