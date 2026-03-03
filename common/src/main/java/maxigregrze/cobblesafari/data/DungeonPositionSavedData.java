package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.DungeonTeleportHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonPositionSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_dungeon_positions";
    private static final String NBT_USED_SLOTS = "UsedSlots";
    private static final String NBT_SLOTS = "Slots";
    private static final String NBT_DIMENSION_ID = "DimensionId";
    private static final String NBT_SLOT_GENERATIONS = "SlotGenerations";

    private final Map<String, Set<Integer>> usedSlots = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Integer>> slotUseCounts = new ConcurrentHashMap<>();

    public DungeonPositionSavedData() {
        // no-args constructor required by SavedData
    }

    public static DungeonPositionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonPositionSavedData data = new DungeonPositionSavedData();

        if (tag.contains("NextInstanceCounter")) {
            CobbleSafari.LOGGER.warn(
                    "Dungeon position data is in old format (counter-based). Resetting to new slot-based system.");
            return data;
        }

        if (tag.contains(NBT_USED_SLOTS, Tag.TAG_LIST)) {
            ListTag dimList = tag.getList(NBT_USED_SLOTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < dimList.size(); i++) {
                CompoundTag dimTag = dimList.getCompound(i);
                String dimensionId = dimTag.getString(NBT_DIMENSION_ID);
                Set<Integer> slots = ConcurrentHashMap.newKeySet();

                if (dimTag.contains(NBT_SLOTS, Tag.TAG_INT_ARRAY)) {
                    int[] slotArray = dimTag.getIntArray(NBT_SLOTS);
                    for (int slot : slotArray) {
                        slots.add(slot);
                    }
                }

                data.usedSlots.put(dimensionId, slots);

                Map<Integer, Integer> generations = new ConcurrentHashMap<>();
                if (dimTag.contains(NBT_SLOT_GENERATIONS, Tag.TAG_COMPOUND)) {
                    CompoundTag genTag = dimTag.getCompound(NBT_SLOT_GENERATIONS);
                    for (String key : genTag.getAllKeys()) {
                        try {
                            int slot = Integer.parseInt(key);
                            generations.put(slot, genTag.getInt(key));
                        } catch (NumberFormatException e) {
                            CobbleSafari.LOGGER.warn("Invalid slot key in SlotGenerations: {}", key);
                        }
                    }
                } else {
                    for (Integer slot : slots) {
                        generations.put(slot, 1);
                    }
                }
                data.slotUseCounts.put(dimensionId, generations);
            }
        }

        int totalSlots = data.usedSlots.values().stream().mapToInt(Set::size).sum();
        CobbleSafari.LOGGER.info("Loaded dungeon position data: {} dimensions, {} active slots",
                data.usedSlots.size(), totalSlots);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag dimList = new ListTag();
        Set<String> dimensionIds = new HashSet<>(usedSlots.keySet());
        dimensionIds.addAll(slotUseCounts.keySet());
        for (String dimensionId : dimensionIds) {
            Set<Integer> slots = usedSlots.get(dimensionId);
            Map<Integer, Integer> generations = slotUseCounts.get(dimensionId);
            if ((slots == null || slots.isEmpty()) && (generations == null || generations.isEmpty())) continue;
            CompoundTag dimTag = new CompoundTag();
            dimTag.putString(NBT_DIMENSION_ID, dimensionId);
            if (slots != null && !slots.isEmpty()) {
                int[] slotArray = slots.stream().mapToInt(Integer::intValue).toArray();
                dimTag.put(NBT_SLOTS, new IntArrayTag(slotArray));
            } else {
                dimTag.put(NBT_SLOTS, new IntArrayTag(new int[0]));
            }
            if (generations != null && !generations.isEmpty()) {
                CompoundTag genTag = new CompoundTag();
                for (Map.Entry<Integer, Integer> e : generations.entrySet()) {
                    genTag.putInt(String.valueOf(e.getKey()), e.getValue());
                }
                dimTag.put(NBT_SLOT_GENERATIONS, genTag);
            }
            dimList.add(dimTag);
        }
        tag.put(NBT_USED_SLOTS, dimList);

        int totalSlots = usedSlots.values().stream().mapToInt(Set::size).sum();
        CobbleSafari.LOGGER.debug("Saved dungeon position data: {} dimensions, {} active slots",
                usedSlots.size(), totalSlots);
        return tag;
    }

    public int allocateSlot(String dimensionId) {
        Set<Integer> slots = usedSlots.computeIfAbsent(dimensionId, k -> ConcurrentHashMap.newKeySet());
        int slot = 0;
        while (slots.contains(slot)) {
            slot++;
        }
        slots.add(slot);
        setDirty();
        CobbleSafari.LOGGER.debug("Allocated slot {} for dimension {}", slot, dimensionId);
        return slot;
    }

    public int getAndIncrementSlotZOffset(String dimensionId, int slot) {
        Map<Integer, Integer> dimCounts = slotUseCounts.computeIfAbsent(dimensionId, k -> new ConcurrentHashMap<>());
        int zOffset = dimCounts.getOrDefault(slot, 0);
        dimCounts.put(slot, zOffset + 1);
        setDirty();
        CobbleSafari.LOGGER.debug("Slot {} Z offset {} (next will be {}) for dimension {}", slot, zOffset, zOffset + 1, dimensionId);
        return zOffset;
    }

    public void freeSlot(String dimensionId, int slot) {
        Set<Integer> slots = usedSlots.get(dimensionId);
        if (slots != null) {
            slots.remove(slot);
            if (slots.isEmpty()) {
                usedSlots.remove(dimensionId);
            }
            setDirty();
            CobbleSafari.LOGGER.debug("Freed slot {} for dimension {}", slot, dimensionId);
        }
    }

    public void removeUsedPosition(String dimensionId, BlockPos pos) {
        int slot = slotFromPos(pos);
        CobbleSafari.LOGGER.debug("Freeing slot {} (from pos {}) for dimension {}", slot, pos, dimensionId);
        freeSlot(dimensionId, slot);
    }

    private static int slotFromPos(BlockPos pos) {
        int zoneSize = DungeonTeleportHandler.ZONE_SIZE;
        int col = pos.getX() / zoneSize;
        int row = pos.getZ() / zoneSize;
        return col + row * 100;
    }

    public void clearDimension(String dimensionId) {
        usedSlots.remove(dimensionId);
        slotUseCounts.remove(dimensionId);
        setDirty();
        CobbleSafari.LOGGER.info("Cleared used slots for dimension: {}", dimensionId);
    }

    public void clearAllDungeons() {
        usedSlots.entrySet().removeIf(entry -> entry.getKey().startsWith("cobblesafari:dungeon"));
        slotUseCounts.keySet().removeIf(id -> id.startsWith("cobblesafari:dungeon"));
        setDirty();
        CobbleSafari.LOGGER.info("Cleared used slots for all dungeon dimensions");
    }

    public void clearAll() {
        usedSlots.clear();
        slotUseCounts.clear();
        setDirty();
        CobbleSafari.LOGGER.info("Cleared all used slots");
    }

    public static DungeonPositionSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(createFactory(), DATA_NAME);
    }

    private static Factory<DungeonPositionSavedData> createFactory() {
        return new Factory<>(DungeonPositionSavedData::new, DungeonPositionSavedData::load, null);
    }
}
