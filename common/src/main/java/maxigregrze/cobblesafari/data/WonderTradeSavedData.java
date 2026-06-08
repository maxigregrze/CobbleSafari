package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.wondertrade.WonderTradePoolEntry;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WonderTradeSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_wonder_trade";
    private static final String KEY_POOL = "Pool";
    private static final String KEY_CREDITS = "Credits";
    private static final String KEY_LAST_DAILY_EPOCH_DAY = "LastDailyEpochDay";
    private static final String KEY_EVENT = "Event";

    /** Fresh file: no NBT key; {@link WonderTradeService} will set before the first tick. */
    private long lastDailyResetEpochDay = -1L;

    private final List<WonderTradePoolEntry> pool = new ArrayList<>();
    private final Map<UUID, Integer> tradeCreditsRemaining = new HashMap<>();

    private String activeEventId = "";
    private byte activeEventMode = 0;
    private int activeEventDaysLeft = 0;

    public WonderTradeSavedData() {
        // Required by the SavedData factory; state is populated in load().
    }

    public static WonderTradeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WonderTradeSavedData data = new WonderTradeSavedData();
        if (tag.contains(KEY_POOL, Tag.TAG_LIST)) {
            ListTag list = tag.getList(KEY_POOL, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                data.pool.add(WonderTradePoolEntry.fromNbt(list.getCompound(i)));
            }
        }
        if (tag.contains(KEY_CREDITS, Tag.TAG_COMPOUND)) {
            CompoundTag credits = tag.getCompound(KEY_CREDITS);
            for (String key : credits.getAllKeys()) {
                try {
                    UUID id = UUID.fromString(key);
                    data.tradeCreditsRemaining.put(id, credits.getInt(key));
                } catch (IllegalArgumentException ignored) {
                    // Skip entries whose key is not a valid UUID (corrupt/legacy data).
                }
            }
        }
        if (tag.contains(KEY_LAST_DAILY_EPOCH_DAY)) {
            data.lastDailyResetEpochDay = tag.getLong(KEY_LAST_DAILY_EPOCH_DAY);
        }
        if (tag.contains(KEY_EVENT, Tag.TAG_COMPOUND)) {
            CompoundTag ev = tag.getCompound(KEY_EVENT);
            data.activeEventId = ev.getString("Id");
            data.activeEventMode = ev.getByte("Mode");
            data.activeEventDaysLeft = ev.getInt("DaysLeft");
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (WonderTradePoolEntry e : pool) {
            list.add(e.toNbt());
        }
        tag.put(KEY_POOL, list);

        CompoundTag credits = new CompoundTag();
        for (Map.Entry<UUID, Integer> e : tradeCreditsRemaining.entrySet()) {
            credits.putInt(e.getKey().toString(), e.getValue());
        }
        tag.put(KEY_CREDITS, credits);
        tag.putLong(KEY_LAST_DAILY_EPOCH_DAY, lastDailyResetEpochDay);

        CompoundTag ev = new CompoundTag();
        ev.putString("Id", activeEventId);
        ev.putByte("Mode", activeEventMode);
        ev.putInt("DaysLeft", activeEventDaysLeft);
        tag.put(KEY_EVENT, ev);
        return tag;
    }

    public static WonderTradeSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(new Factory<>(WonderTradeSavedData::new, WonderTradeSavedData::load, null), DATA_NAME);
    }

    public List<WonderTradePoolEntry> getPool() {
        return pool;
    }

    public int getPoolSize() {
        return pool.size();
    }

    public void addPoolEntry(WonderTradePoolEntry entry) {
        pool.add(entry);
        setDirty();
    }

    public void clearPool() {
        pool.clear();
        setDirty();
    }

    public WonderTradePoolEntry removePoolEntry(int index) {
        WonderTradePoolEntry removed = pool.remove(index);
        setDirty();
        return removed;
    }

    public void setPoolEntry(int index, WonderTradePoolEntry entry) {
        pool.set(index, entry);
        setDirty();
    }

    public long getLastDailyResetEpochDay() {
        return lastDailyResetEpochDay;
    }

    public void setLastDailyResetEpochDay(long lastDailyResetEpochDay) {
        this.lastDailyResetEpochDay = lastDailyResetEpochDay;
        setDirty();
    }

    public int getCredits(UUID playerId) {
        return tradeCreditsRemaining.getOrDefault(playerId, Integer.MIN_VALUE);
    }

    public void setCredits(UUID playerId, int value) {
        tradeCreditsRemaining.put(playerId, value);
        setDirty();
    }

    public void clearTradeCredits() {
        tradeCreditsRemaining.clear();
        setDirty();
    }

    public boolean hasActiveEvent() {
        return activeEventMode != 0 && !activeEventId.isEmpty();
    }

    public String getActiveEventId() {
        return activeEventId;
    }

    public byte getActiveEventMode() {
        return activeEventMode;
    }

    public int getActiveEventDaysLeft() {
        return activeEventDaysLeft;
    }

    public void startEventNextReset(String eventId) {
        this.activeEventId = eventId;
        this.activeEventMode = 1;
        this.activeEventDaysLeft = 0;
        setDirty();
    }

    public void startEventCountdown(String eventId, int days) {
        this.activeEventId = eventId;
        this.activeEventMode = 2;
        this.activeEventDaysLeft = days;
        setDirty();
    }

    public void clearEvent() {
        this.activeEventId = "";
        this.activeEventMode = 0;
        this.activeEventDaysLeft = 0;
        setDirty();
    }

    public void decrementEventDaysLeft() {
        if (activeEventMode == 2) {
            activeEventDaysLeft--;
        }
        setDirty();
    }
}
