package maxigregrze.cobblesafari.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Persistent dungeon structure instance tracked for async region clearing.
 * When {@link #pendingDeletion()} is true, the structure is queued for (or undergoing) removal.
 */
public record DungeonInstanceRecord(
        UUID instanceId,
        UUID portalId,
        String dimensionId,
        int slot,
        BlockPos structurePos,
        int chunkMinX,
        int chunkMinZ,
        int chunkMaxX,
        int chunkMaxZ,
        int structureY,
        int clearSectionsBelow,
        int clearSectionsAbove,
        int zoneSize,
        boolean pendingDeletion
) {
    private static final String NBT_INSTANCE_ID = "InstanceId";
    private static final String NBT_PORTAL_ID = "PortalId";
    private static final String NBT_DIMENSION_ID = "DimensionId";
    private static final String NBT_SLOT = "Slot";
    private static final String NBT_STRUCTURE_X = "StructureX";
    private static final String NBT_STRUCTURE_Y = "StructureY";
    private static final String NBT_STRUCTURE_Z = "StructureZ";
    private static final String NBT_CHUNK_MIN_X = "ChunkMinX";
    private static final String NBT_CHUNK_MIN_Z = "ChunkMinZ";
    private static final String NBT_CHUNK_MAX_X = "ChunkMaxX";
    private static final String NBT_CHUNK_MAX_Z = "ChunkMaxZ";
    private static final String NBT_CLEAR_SECTIONS_BELOW = "ClearSectionsBelow";
    private static final String NBT_CLEAR_SECTIONS_ABOVE = "ClearSectionsAbove";
    private static final String NBT_ZONE_SIZE = "ZoneSize";
    private static final String NBT_PENDING_DELETION = "PendingDeletion";

    public static DungeonInstanceRecord create(
            UUID portalId,
            String dimensionId,
            int slot,
            BlockPos structurePos,
            int chunkMinX,
            int chunkMinZ,
            int chunkMaxX,
            int chunkMaxZ,
            int structureY,
            int clearSectionsBelow,
            int clearSectionsAbove,
            int zoneSize
    ) {
        return new DungeonInstanceRecord(
                UUID.randomUUID(),
                portalId,
                dimensionId,
                slot,
                structurePos,
                chunkMinX,
                chunkMinZ,
                chunkMaxX,
                chunkMaxZ,
                structureY,
                clearSectionsBelow,
                clearSectionsAbove,
                zoneSize,
                false
        );
    }

    public DungeonInstanceRecord withPendingDeletion(boolean pendingDeletion) {
        return new DungeonInstanceRecord(
                instanceId, portalId, dimensionId, slot, structurePos,
                chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                structureY, clearSectionsBelow, clearSectionsAbove, zoneSize,
                pendingDeletion
        );
    }

    /**
     * Whether the persisted bounds are real: a live-pipeline record carries a ±1-chunk margin, so a
     * genuine box spans three chunks or more, and an all-zero box means "bounds unknown".
     */
    public boolean hasUsableBounds() {
        return chunkMaxX - chunkMinX >= 2 && chunkMaxZ - chunkMinZ >= 2;
    }

    /**
     * Whether {@code pos} falls inside this instance's footprint, ignoring Y: a player above the
     * cleanup window still belongs here. Y only breaks ties between overlapping instances.
     */
    public boolean containsColumn(BlockPos pos) {
        int minX = chunkMinX << 4;
        int minZ = chunkMinZ << 4;
        int maxX = (chunkMaxX << 4) + 15;
        int maxZ = (chunkMaxZ << 4) + 15;
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(NBT_INSTANCE_ID, instanceId);
        if (portalId != null) {
            tag.putUUID(NBT_PORTAL_ID, portalId);
        }
        tag.putString(NBT_DIMENSION_ID, dimensionId);
        tag.putInt(NBT_SLOT, slot);
        tag.putInt(NBT_STRUCTURE_X, structurePos.getX());
        tag.putInt(NBT_STRUCTURE_Y, structurePos.getY());
        tag.putInt(NBT_STRUCTURE_Z, structurePos.getZ());
        tag.putInt(NBT_CHUNK_MIN_X, chunkMinX);
        tag.putInt(NBT_CHUNK_MIN_Z, chunkMinZ);
        tag.putInt(NBT_CHUNK_MAX_X, chunkMaxX);
        tag.putInt(NBT_CHUNK_MAX_Z, chunkMaxZ);
        tag.putInt(NBT_CLEAR_SECTIONS_BELOW, clearSectionsBelow);
        tag.putInt(NBT_CLEAR_SECTIONS_ABOVE, clearSectionsAbove);
        tag.putInt(NBT_ZONE_SIZE, zoneSize);
        tag.putBoolean(NBT_PENDING_DELETION, pendingDeletion);
        return tag;
    }

    public static DungeonInstanceRecord load(CompoundTag tag) {
        UUID instanceId = tag.contains(NBT_INSTANCE_ID) ? tag.getUUID(NBT_INSTANCE_ID) : UUID.randomUUID();
        UUID portalId = tag.contains(NBT_PORTAL_ID) ? tag.getUUID(NBT_PORTAL_ID) : null;
        BlockPos structurePos = new BlockPos(
                tag.getInt(NBT_STRUCTURE_X),
                tag.getInt(NBT_STRUCTURE_Y),
                tag.getInt(NBT_STRUCTURE_Z)
        );
        return new DungeonInstanceRecord(
                instanceId,
                portalId,
                tag.getString(NBT_DIMENSION_ID),
                tag.getInt(NBT_SLOT),
                structurePos,
                tag.getInt(NBT_CHUNK_MIN_X),
                tag.getInt(NBT_CHUNK_MIN_Z),
                tag.getInt(NBT_CHUNK_MAX_X),
                tag.getInt(NBT_CHUNK_MAX_Z),
                structurePos.getY(),
                tag.getInt(NBT_CLEAR_SECTIONS_BELOW),
                tag.getInt(NBT_CLEAR_SECTIONS_ABOVE),
                tag.getInt(NBT_ZONE_SIZE),
                tag.getBoolean(NBT_PENDING_DELETION)
        );
    }
}
