package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
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
    private static final String NBT_INSTANCES = "Instances";

    private final Map<String, Set<Integer>> usedSlots = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Integer>> slotUseCounts = new ConcurrentHashMap<>();
    private final Map<UUID, DungeonInstanceRecord> instances = new ConcurrentHashMap<>();

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

        if (tag.contains(NBT_INSTANCES, Tag.TAG_LIST)) {
            ListTag instanceList = tag.getList(NBT_INSTANCES, Tag.TAG_COMPOUND);
            for (int i = 0; i < instanceList.size(); i++) {
                DungeonInstanceRecord record = DungeonInstanceRecord.load(instanceList.getCompound(i));
                data.instances.put(record.instanceId(), record);
            }
        }

        int totalSlots = data.usedSlots.values().stream().mapToInt(Set::size).sum();
        int pendingClears = data.countPendingDeletions();
        CobbleSafari.LOGGER.info(
                "Loaded dungeon position data: {} dimensions, {} active slots, {} tracked instances ({} pending deletion)",
                data.usedSlots.size(), totalSlots, data.instances.size(), pendingClears);
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

        if (!instances.isEmpty()) {
            ListTag instanceList = new ListTag();
            for (DungeonInstanceRecord record : instances.values()) {
                instanceList.add(record.save());
            }
            tag.put(NBT_INSTANCES, instanceList);
        }

        int totalSlots = usedSlots.values().stream().mapToInt(Set::size).sum();
        CobbleSafari.LOGGER.debug("Saved dungeon position data: {} dimensions, {} active slots, {} instances",
                usedSlots.size(), totalSlots, instances.size());
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
        dimCounts.put(slot, zOffset + 16);
        setDirty();
        CobbleSafari.LOGGER.debug("Slot {} Z offset {} (next will be {}) for dimension {}", slot, zOffset, zOffset + 16, dimensionId);
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

    public void removeUsedPosition(String dimensionId, BlockPos pos, int zoneSize) {
        int slot = slotFromPos(pos, zoneSize);
        CobbleSafari.LOGGER.debug("Freeing slot {} (from pos {}) for dimension {}", slot, pos, dimensionId);
        freeSlot(dimensionId, slot);
    }

    public static int slotFromPos(BlockPos pos, int zoneSize) {
        int col = pos.getX() / zoneSize;
        int row = pos.getZ() / zoneSize;
        return col + row * 100;
    }

    public void putInstance(DungeonInstanceRecord record) {
        instances.put(record.instanceId(), record);
        setDirty();
    }

    public void updateInstance(DungeonInstanceRecord record) {
        instances.put(record.instanceId(), record);
        setDirty();
    }

    public DungeonInstanceRecord getInstance(UUID instanceId) {
        return instances.get(instanceId);
    }

    public DungeonInstanceRecord removeInstance(UUID instanceId) {
        DungeonInstanceRecord removed = instances.remove(instanceId);
        if (removed != null) {
            setDirty();
        }
        return removed;
    }

    public List<DungeonInstanceRecord> getInstancesSnapshot() {
        return List.copyOf(instances.values());
    }

    /**
     * Resolves the live dungeon instance occupying the slot at {@code pos} in {@code dimensionId}
     *. Returns {@code null} if the dimension is not a dungeon or no live instance
     * (non-pending) matches the slot.
     */
    public DungeonInstanceRecord findInstanceByPosition(String dimensionId, BlockPos pos) {
        DungeonConfig config = resolveDungeonConfig(dimensionId);
        if (config == null || config.getZoneSize() <= 0) {
            return null;
        }
        int slot = slotFromPos(pos, config.getZoneSize());
        for (DungeonInstanceRecord record : instances.values()) {
            if (!record.pendingDeletion() && record.slot() == slot
                    && dimensionId.equals(record.dimensionId())) {
                return record;
            }
        }
        return null;
    }

    public List<DungeonInstanceRecord> getPendingDeletionInstances() {
        return instances.values().stream()
                .filter(DungeonInstanceRecord::pendingDeletion)
                .toList();
    }

    public int countPendingDeletions() {
        return (int) instances.values().stream().filter(DungeonInstanceRecord::pendingDeletion).count();
    }

    public DungeonInstanceRecord findInstance(UUID portalId, BlockPos structurePos, String dimensionId) {
        for (DungeonInstanceRecord record : instances.values()) {
            if (!record.dimensionId().equals(dimensionId)) {
                continue;
            }
            if (portalId != null && portalId.equals(record.portalId())) {
                return record;
            }
            if (structurePos != null && structurePos.equals(record.structurePos())) {
                return record;
            }
        }
        return null;
    }

    /**
     * Creates pending-deletion instance records for used slots that have no tracked instance
     * (e.g. after a crash or a portal removed without cleanup).
     */
    public void reconcileOrphanedSlotsToInstances() {
        for (Map.Entry<String, Set<Integer>> entry : usedSlots.entrySet()) {
            String dimensionId = entry.getKey();
            DungeonConfig config = resolveDungeonConfig(dimensionId);
            if (config == null || config.isExternallyManaged()) {
                continue;
            }

            int zoneSize = config.getZoneSize();
            Map<Integer, Integer> generations = slotUseCounts.getOrDefault(dimensionId, Map.of());

            for (int slot : entry.getValue()) {
                boolean hasInstance = instances.values().stream()
                        .anyMatch(r -> r.dimensionId().equals(dimensionId) && r.slot() == slot);
                if (hasInstance) {
                    continue;
                }

                int zOffset = generations.getOrDefault(slot, 0);
                if (zOffset > 0) {
                    zOffset -= 16;
                }
                BlockPos structurePos = structurePosFromSlot(slot, zOffset, zoneSize);
                int[] bounds = chunkBoundsFromStructure(structurePos, zoneSize);
                DungeonInstanceRecord orphan = DungeonInstanceRecord.create(
                        null,
                        dimensionId,
                        slot,
                        structurePos,
                        bounds[0],
                        bounds[1],
                        bounds[2],
                        bounds[3],
                        structurePos.getY(),
                        config.getClearSectionsBelow(),
                        config.getClearSectionsAbove(),
                        zoneSize
                ).withPendingDeletion(true);
                instances.put(orphan.instanceId(), orphan);
                CobbleSafari.LOGGER.info(
                        "Reconciled orphaned dungeon slot {} at {} for dimension {} (pending deletion)",
                        slot, structurePos, dimensionId);
            }
        }
        setDirty();
    }

    private static BlockPos structurePosFromSlot(int slot, int zOffset, int zoneSize) {
        int x = (slot % 100) * zoneSize;
        int z = (slot / 100) * zoneSize + zOffset;
        int y = (slot % 2 == 0) ? 96 : 32;
        return new BlockPos(x, y, z);
    }

    public static int[] chunkBoundsFromStructure(BlockPos structurePos, int zoneSize) {
        int regionRadius = zoneSize / 2 - 1;
        return new int[]{
                (structurePos.getX() - regionRadius) >> 4,
                (structurePos.getZ() - regionRadius) >> 4,
                (structurePos.getX() + regionRadius) >> 4,
                (structurePos.getZ() + regionRadius) >> 4
        };
    }

    public static DungeonConfig resolveDungeonConfig(String dimensionIdOrDungeonId) {
        if (dimensionIdOrDungeonId == null) {
            return null;
        }
        DungeonConfig config = DungeonDimensions.getDungeonById(dimensionIdOrDungeonId);
        if (config != null) {
            return config;
        }
        for (DungeonConfig dungeon : DungeonDimensions.getAllDungeons()) {
            if (dungeon.getDimensionId().equals(dimensionIdOrDungeonId)
                    || dungeon.getId().equals(dimensionIdOrDungeonId)) {
                return dungeon;
            }
        }
        return null;
    }

    public void clearDimension(String dimensionId) {
        usedSlots.remove(dimensionId);
        slotUseCounts.remove(dimensionId);
        setDirty();
        CobbleSafari.LOGGER.info("Cleared used slots for dimension: {}", dimensionId);
    }

    /**
     * Clears slot metadata only. Does NOT remove instance records — use
     * {@link maxigregrze.cobblesafari.dungeon.DungeonInstanceCleanup#scheduleResetForAllInstances}
     * instead to properly clear structures and free slots.
     */
    public void clearAllDungeons() {
        usedSlots.entrySet().removeIf(entry -> entry.getKey().startsWith("cobblesafari:dungeon"));
        slotUseCounts.keySet().removeIf(id -> id.startsWith("cobblesafari:dungeon"));
        setDirty();
        CobbleSafari.LOGGER.info("Cleared used slots for all dungeon dimensions (instances preserved)");
    }

    public void clearAll() {
        usedSlots.clear();
        slotUseCounts.clear();
        instances.clear();
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
