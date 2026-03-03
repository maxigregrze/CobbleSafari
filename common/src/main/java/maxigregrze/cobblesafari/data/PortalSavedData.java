package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_portal_data";

    private final Map<UUID, PortalData> portals = new HashMap<>();

    public PortalSavedData() {
    }

    public static PortalSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PortalSavedData data = new PortalSavedData();

        if (tag.contains("portals", Tag.TAG_LIST)) {
            ListTag portalList = tag.getList("portals", Tag.TAG_COMPOUND);
            for (int i = 0; i < portalList.size(); i++) {
                CompoundTag portalTag = portalList.getCompound(i);
                UUID portalId = portalTag.getUUID("PortalId");
                BlockPos pos = new BlockPos(
                        portalTag.getInt("X"),
                        portalTag.getInt("Y"),
                        portalTag.getInt("Z")
                );
                ResourceKey<Level> dimension = ResourceKey.create(
                        Registries.DIMENSION,
                        ResourceLocation.parse(portalTag.getString("Dimension"))
                );
                long spawnTick = portalTag.getLong("SpawnTick");
                String dungeonId = portalTag.getString("DungeonId");

                String dungeonDimensionId = portalTag.contains("DungeonDimensionId") ? portalTag.getString("DungeonDimensionId") : null;
                BlockPos dungeonStructurePos = null;
                if (portalTag.contains("DungeonStructureX")) {
                    dungeonStructurePos = new BlockPos(
                            portalTag.getInt("DungeonStructureX"),
                            portalTag.getInt("DungeonStructureY"),
                            portalTag.getInt("DungeonStructureZ")
                    );
                }
                boolean hasBounds = portalTag.contains("HasDungeonChunkBounds") && portalTag.getBoolean("HasDungeonChunkBounds");
                int chunkMinX = portalTag.getInt("ChunkMinX");
                int chunkMinZ = portalTag.getInt("ChunkMinZ");
                int chunkMaxX = portalTag.getInt("ChunkMaxX");
                int chunkMaxZ = portalTag.getInt("ChunkMaxZ");

                data.portals.put(portalId, new PortalData(portalId, pos, dimension, spawnTick, dungeonId,
                        dungeonDimensionId, dungeonStructurePos, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ, hasBounds));
            }
        }

        CobbleSafari.LOGGER.info("Loaded portal data: {} portals", data.portals.size());
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag portalList = new ListTag();

        for (PortalData portal : portals.values()) {
            CompoundTag portalTag = new CompoundTag();
            portalTag.putUUID("PortalId", portal.portalId());
            portalTag.putInt("X", portal.pos().getX());
            portalTag.putInt("Y", portal.pos().getY());
            portalTag.putInt("Z", portal.pos().getZ());
            portalTag.putString("Dimension", portal.dimension().location().toString());
            portalTag.putLong("SpawnTick", portal.spawnTick());
            portalTag.putString("DungeonId", portal.dungeonId());
            if (portal.dungeonDimensionId() != null) {
                portalTag.putString("DungeonDimensionId", portal.dungeonDimensionId());
            }
            if (portal.dungeonStructurePos() != null) {
                portalTag.putInt("DungeonStructureX", portal.dungeonStructurePos().getX());
                portalTag.putInt("DungeonStructureY", portal.dungeonStructurePos().getY());
                portalTag.putInt("DungeonStructureZ", portal.dungeonStructurePos().getZ());
            }
            if (portal.hasDungeonChunkBounds()) {
                portalTag.putBoolean("HasDungeonChunkBounds", true);
                portalTag.putInt("ChunkMinX", portal.chunkMinX());
                portalTag.putInt("ChunkMinZ", portal.chunkMinZ());
                portalTag.putInt("ChunkMaxX", portal.chunkMaxX());
                portalTag.putInt("ChunkMaxZ", portal.chunkMaxZ());
            }
            portalList.add(portalTag);
        }

        tag.put("portals", portalList);
        CobbleSafari.LOGGER.info("Saved portal data: {} portals", portals.size());
        return tag;
    }

    public void addPortal(UUID portalId, BlockPos pos, ResourceKey<Level> dimension, long spawnTick, String dungeonId) {
        portals.put(portalId, new PortalData(portalId, pos, dimension, spawnTick, dungeonId,
                null, null, 0, 0, 0, 0, false));
        setDirty();
    }

    public void addPortal(UUID portalId, BlockPos pos, ResourceKey<Level> dimension, long spawnTick, String dungeonId,
                          String dungeonDimensionId, BlockPos dungeonStructurePos,
                          int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ, boolean hasBounds) {
        portals.put(portalId, new PortalData(portalId, pos, dimension, spawnTick, dungeonId,
                dungeonDimensionId, dungeonStructurePos, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ, hasBounds));
        setDirty();
    }

    public void removePortal(UUID portalId) {
        portals.remove(portalId);
        setDirty();
    }

    public void clearAll() {
        portals.clear();
        setDirty();
    }

    public Map<UUID, PortalData> getPortals() {
        return new HashMap<>(portals);
    }

    public static PortalSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(createFactory(), DATA_NAME);
    }

    private static Factory<PortalSavedData> createFactory() {
        return new Factory<>(PortalSavedData::new, PortalSavedData::load, null);
    }

    public record PortalData(
            UUID portalId,
            BlockPos pos,
            ResourceKey<Level> dimension,
            long spawnTick,
            String dungeonId,
            String dungeonDimensionId,
            BlockPos dungeonStructurePos,
            int chunkMinX,
            int chunkMinZ,
            int chunkMaxX,
            int chunkMaxZ,
            boolean hasDungeonChunkBounds
    ) {}
}
