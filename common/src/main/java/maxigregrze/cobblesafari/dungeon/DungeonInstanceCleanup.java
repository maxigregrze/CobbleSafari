package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.data.DungeonInstanceRecord;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Coordinates persistent dungeon instance deletion using the same async {@link DungeonRegionClearer}
 * pipeline as natural portal expiry. Survives restarts via {@link DungeonPositionSavedData}.
 */
public final class DungeonInstanceCleanup {

    private static final Set<UUID> IN_FLIGHT_CLEARS = new HashSet<>();

    private DungeonInstanceCleanup() {}

    public static void registerActiveInstance(
            MinecraftServer server,
            UUID portalId,
            DungeonConfig config,
            BlockPos structurePos,
            int chunkMinX,
            int chunkMinZ,
            int chunkMaxX,
            int chunkMaxZ
    ) {
        DungeonPositionSavedData data = DungeonPositionSavedData.get(server);
        DungeonInstanceRecord existing = data.findInstance(portalId, structurePos, config.getDimensionId());
        if (existing != null) {
            return;
        }
        int slot = DungeonPositionSavedData.slotFromPos(structurePos, config.getZoneSize());
        DungeonInstanceRecord record = DungeonInstanceRecord.create(
                portalId,
                config.getDimensionId(),
                slot,
                structurePos,
                chunkMinX,
                chunkMinZ,
                chunkMaxX,
                chunkMaxZ,
                structurePos.getY(),
                config.getClearSectionsBelow(),
                config.getClearSectionsAbove(),
                config.getZoneSize()
        );
        data.putInstance(record);
        CobbleSafari.LOGGER.debug("Registered dungeon instance {} at {} for dimension {}",
                record.instanceId(), structurePos, config.getDimensionId());
    }

    public static void scheduleDeletionForPortal(MinecraftServer server, DungeonPortalBlockEntity portalEntity) {
        if (!portalEntity.hasDungeonChunkBounds()) {
            return;
        }

        BlockPos structurePos = portalEntity.getDungeonStructurePos();
        if (structurePos == null) {
            return;
        }

        DungeonConfig config = DungeonPositionSavedData.resolveDungeonConfig(portalEntity.getDungeonDimensionId());
        if (config == null) {
            return;
        }

        DungeonPositionSavedData data = DungeonPositionSavedData.get(server);
        DungeonInstanceRecord existing = data.findInstance(portalEntity.getPortalId(), structurePos, config.getDimensionId());
        if (existing == null) {
            int slot = DungeonPositionSavedData.slotFromPos(structurePos, config.getZoneSize());
            existing = DungeonInstanceRecord.create(
                    portalEntity.getPortalId(),
                    config.getDimensionId(),
                    slot,
                    structurePos,
                    portalEntity.getDungeonChunkMinX(),
                    portalEntity.getDungeonChunkMinZ(),
                    portalEntity.getDungeonChunkMaxX(),
                    portalEntity.getDungeonChunkMaxZ(),
                    structurePos.getY(),
                    config.getClearSectionsBelow(),
                    config.getClearSectionsAbove(),
                    config.getZoneSize()
            );
            data.putInstance(existing);
        }

        markPendingAndSchedule(server, existing.instanceId());
    }

    public static void scheduleDeletionForActivePortal(MinecraftServer server, PortalSpawnManager.ActivePortal portal) {
        if (!portal.hasDungeonChunkBounds() || portal.dungeonStructurePos() == null) {
            return;
        }

        DungeonConfig config = DungeonPositionSavedData.resolveDungeonConfig(portal.dungeonDimensionId());
        if (config == null) {
            return;
        }

        BlockPos structurePos = portal.dungeonStructurePos();
        DungeonPositionSavedData data = DungeonPositionSavedData.get(server);
        DungeonInstanceRecord existing = data.findInstance(portal.id(), structurePos, config.getDimensionId());
        if (existing == null) {
            int slot = DungeonPositionSavedData.slotFromPos(structurePos, config.getZoneSize());
            existing = DungeonInstanceRecord.create(
                    portal.id(),
                    config.getDimensionId(),
                    slot,
                    structurePos,
                    portal.dungeonChunkMinX(),
                    portal.dungeonChunkMinZ(),
                    portal.dungeonChunkMaxX(),
                    portal.dungeonChunkMaxZ(),
                    structurePos.getY(),
                    config.getClearSectionsBelow(),
                    config.getClearSectionsAbove(),
                    config.getZoneSize()
            );
            data.putInstance(existing);
        }

        markPendingAndSchedule(server, existing.instanceId());
    }

    /**
     * Marks every known instance for deletion and schedules async region clears.
     * Slots are freed when each clear completes (not immediately).
     */
    public static void scheduleResetForAllInstances(MinecraftServer server) {
        DungeonPositionSavedData data = DungeonPositionSavedData.get(server);

        for (PortalSpawnManager.ActivePortal portal : PortalSpawnManager.getActivePortals()) {
            scheduleDeletionForActivePortal(server, portal);
        }

        data.reconcileOrphanedSlotsToInstances();

        for (DungeonInstanceRecord record : data.getInstancesSnapshot()) {
            if (!record.pendingDeletion()) {
                data.updateInstance(record.withPendingDeletion(true));
            }
            scheduleRegionClear(server, record.instanceId());
        }

        int pending = data.countPendingDeletions();
        CobbleSafari.LOGGER.info(
                "Dungeon reset: scheduled {} instance region clear(s) (async, persisted until complete)",
                pending);
    }

    /**
     * Resumes any instance clears that were interrupted by a stop or crash.
     * Should run early during server startup, before new dungeon allocations.
     */
    public static void resumePendingClears(MinecraftServer server) {
        IN_FLIGHT_CLEARS.clear();

        DungeonPositionSavedData data = DungeonPositionSavedData.get(server);
        data.reconcileOrphanedSlotsToInstances();

        List<DungeonInstanceRecord> pending = data.getPendingDeletionInstances();
        if (pending.isEmpty()) {
            return;
        }

        CobbleSafari.LOGGER.info(
                "Resuming {} persisted dungeon region clear(s) from previous session",
                pending.size());

        for (DungeonInstanceRecord record : pending) {
            scheduleRegionClear(server, record.instanceId());
        }
    }

    private static void markPendingAndSchedule(MinecraftServer server, UUID instanceId) {
        DungeonPositionSavedData data = DungeonPositionSavedData.get(server);
        DungeonInstanceRecord record = data.getInstance(instanceId);
        if (record == null) {
            return;
        }
        if (!record.pendingDeletion()) {
            data.updateInstance(record.withPendingDeletion(true));
        }
        scheduleRegionClear(server, instanceId);
    }

    private static void scheduleRegionClear(MinecraftServer server, UUID instanceId) {
        if (!IN_FLIGHT_CLEARS.add(instanceId)) {
            return;
        }

        DungeonPositionSavedData data = DungeonPositionSavedData.get(server);
        DungeonInstanceRecord record = data.getInstance(instanceId);
        if (record == null) {
            IN_FLIGHT_CLEARS.remove(instanceId);
            return;
        }

        DungeonConfig config = DungeonPositionSavedData.resolveDungeonConfig(record.dimensionId());
        if (config == null || config.isExternallyManaged()) {
            completeClear(server, instanceId);
            return;
        }

        ServerLevel dungeonLevel = server.getLevel(config.getDimensionKey());
        if (dungeonLevel == null) {
            completeClear(server, instanceId);
            return;
        }

        // Plan 118 §7 — purge objectives tied to this instance before the chunk clear.
        maxigregrze.cobblesafari.objectives.ObjectivesManager.purgeInstance(
                server, record.dimensionId(), record.instanceId());

        evacuatePlayersFromRegion(dungeonLevel, record);

        Runnable onComplete = () -> server.execute(() -> completeClear(server, instanceId));
        DungeonRegionClearer.scheduleRegionClear(
                dungeonLevel,
                record.chunkMinX(),
                record.chunkMinZ(),
                record.chunkMaxX(),
                record.chunkMaxZ(),
                record.structureY(),
                record.clearSectionsBelow(),
                record.clearSectionsAbove(),
                onComplete
        );

        CobbleSafari.LOGGER.info(
                "Scheduled dungeon region clear for instance {} (dimension {}, structure {})",
                instanceId, record.dimensionId(), record.structurePos());
    }

    private static void completeClear(MinecraftServer server, UUID instanceId) {
        IN_FLIGHT_CLEARS.remove(instanceId);

        DungeonPositionSavedData data = DungeonPositionSavedData.get(server);
        DungeonInstanceRecord record = data.removeInstance(instanceId);
        if (record == null) {
            return;
        }

        data.freeSlot(record.dimensionId(), record.slot());
        CobbleSafari.LOGGER.info(
                "Dungeon instance {} cleared and slot {} freed for dimension {}",
                instanceId, record.slot(), record.dimensionId());
    }

    private static void evacuatePlayersFromRegion(ServerLevel dungeonLevel, DungeonInstanceRecord record) {
        int blockMinX = record.chunkMinX() << 4;
        int blockMinZ = record.chunkMinZ() << 4;
        int blockMaxX = (record.chunkMaxX() << 4) + 15;
        int blockMaxZ = (record.chunkMaxZ() << 4) + 15;
        int minY = record.structureY() - (record.clearSectionsBelow() * 16);
        int maxY = record.structureY() + (record.clearSectionsAbove() * 16);

        for (ServerPlayer player : dungeonLevel.players()) {
            BlockPos pos = player.blockPosition();
            if (pos.getX() >= blockMinX && pos.getX() <= blockMaxX
                    && pos.getZ() >= blockMinZ && pos.getZ() <= blockMaxZ
                    && pos.getY() >= minY && pos.getY() <= maxY) {
                maxigregrze.cobblesafari.manager.TimerManager.expireActiveTimerAndTeleport(
                        player,
                        record.dimensionId(),
                        maxigregrze.cobblesafari.manager.TimerManager.getOrCreateData(player, record.dimensionId()),
                        true
                );
            }
        }
    }

}
