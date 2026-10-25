package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.data.TimerScope;
import maxigregrze.cobblesafari.manager.TimerManager;
import net.minecraft.network.chat.Component;
import maxigregrze.cobblesafari.data.DungeonInstanceRecord;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
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

    /** Sentinel for "the allocated slot is not known here" — the caller falls back to deriving it. */
    public static final int UNKNOWN_SLOT = -1;

    private DungeonInstanceCleanup() {}

    /**
     * @param allocatedSlot the slot {@code allocateSlot} handed out, or {@link #UNKNOWN_SLOT} to
     *                      derive it from {@code structurePos}
     */
    public static void registerActiveInstance(
            MinecraftServer server,
            UUID portalId,
            DungeonConfig config,
            BlockPos structurePos,
            int chunkMinX,
            int chunkMinZ,
            int chunkMaxX,
            int chunkMaxZ,
            int allocatedSlot
    ) {
        DungeonPositionSavedData data = DungeonPositionSavedData.get(server);
        DungeonInstanceRecord existing = data.findInstance(portalId, structurePos, config.getDimensionId());
        if (existing != null) {
            return;
        }
        int slot = allocatedSlot >= 0
                ? allocatedSlot
                : DungeonPositionSavedData.slotFromPos(structurePos, config.getZoneSize());
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
        // Must run BEFORE reconciliation, which reads these counters for each slot's last anchor offset.
        data.normalizeSlotZOffsets();
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

    // --- Feature A: detach, then release by occupancy (action plan 182) -------------------------

    /**
     * Marks the instance behind {@code portalEntity} "entrance closed" without erasing anything, then
     * tries an immediate release (A1). A natural portal expiry no longer takes the region with it.
     */
    public static void detachInstanceForPortal(MinecraftServer server, DungeonPortalBlockEntity portalEntity) {
        BlockPos structurePos = portalEntity.getDungeonStructurePos();
        String dimensionId = resolveDimensionId(portalEntity.getDungeonDimensionId());
        if (structurePos == null || dimensionId == null) {
            return;
        }
        DungeonInstanceRecord record = DungeonPositionSavedData.get(server)
                .findInstance(portalEntity.getPortalId(), structurePos, dimensionId);
        detach(server, record);
    }

    /** Same, for a tracked portal whose block is unavailable (unloaded chunk). */
    public static void detachInstanceForActivePortal(MinecraftServer server, PortalSpawnManager.ActivePortal portal) {
        if (portal.dungeonStructurePos() == null) {
            return;
        }
        DungeonConfig config = DungeonPositionSavedData.resolveDungeonConfig(portal.dungeonDimensionId());
        if (config == null) {
            return;
        }
        DungeonInstanceRecord record = DungeonPositionSavedData.get(server)
                .findInstance(portal.id(), portal.dungeonStructurePos(), config.getDimensionId());
        detach(server, record);
    }

    private static String resolveDimensionId(String dungeonDimId) {
        if (dungeonDimId == null) return null;
        DungeonConfig config = DungeonDimensions.getDungeonById(dungeonDimId);
        if (config == null) {
            config = DungeonPositionSavedData.resolveDungeonConfig(dungeonDimId);
        }
        return config == null ? null : config.getDimensionId();
    }

    private static void detach(MinecraftServer server, DungeonInstanceRecord record) {
        if (record == null || record.pendingDeletion()) {
            return;
        }
        DungeonPositionSavedData data = DungeonPositionSavedData.get(server);
        if (!record.entranceClosed()) {
            data.updateInstance(record.withEntranceClosed(true));
            CobbleSafari.LOGGER.info("Detached dungeon instance {} in {} (entrance closed, region kept)",
                    record.instanceId(), record.dimensionId());
            notifyEntranceClosed(server, record);
        }
        // An instance nobody ever inhabited dies at the moment its entrance closes - exactly as it
        // did before this plan. Only the occupied case changes.
        maybeReleaseInstance(server, record.instanceId());
    }

    /** Action bar, not chat and not a title: read without interrupting the run (plan 182 section 8). */
    private static void notifyEntranceClosed(MinecraftServer server, DungeonInstanceRecord record) {
        for (ServerPlayer player : occupantsOf(server, record)) {
            player.displayClientMessage(
                    Component.translatable("cobblesafari.dungeon.entrance_closed"), true);
        }
    }

    /**
     * Releases {@code instanceId} if its entrance is closed and nobody is in it. Idempotent: safe to
     * call from the sweep, from a detach, and from a disconnect.
     */
    public static void maybeReleaseInstance(MinecraftServer server, UUID instanceId) {
        DungeonInstanceRecord record = DungeonPositionSavedData.get(server).getInstance(instanceId);
        if (record == null || record.pendingDeletion()) {
            return;
        }
        // A sub-instance has no entrance to close - its device stands in another dimension - so it
        // is releasable from the moment it is empty (action plan 183, section 5.5).
        if (!record.entranceClosed() && !record.isSubInstance()) {
            return;
        }
        if (isOccupied(server, record)) {
            return;
        }
        CobbleSafari.LOGGER.info("Releasing detached dungeon instance {} in {} (no occupants left)",
                record.instanceId(), record.dimensionId());
        markPendingAndSchedule(server, instanceId);
    }

    /**
     * Unconditional release, used only by the boot sweep of action plan 183 section 7.4: nobody can
     * be inside a sub-instance across a restart, so occupancy has nothing to say.
     */
    public static void forceReleaseInstance(MinecraftServer server, UUID instanceId) {
        DungeonInstanceRecord record = DungeonPositionSavedData.get(server).getInstance(instanceId);
        if (record == null || record.pendingDeletion()) {
            return;
        }
        markPendingAndSchedule(server, instanceId);
    }

    /**
     * The guarantee (plan 182 section 5.1). Hooking release to the departure events instead - timer
     * expiry, death, exit portal, disconnect - would be a guard by vector: exact only while the list
     * stays exhaustive, and Feature B will add an exit to it. This asks the invariant instead, so no
     * departure site has to be modified and none can be forgotten.
     */
    public static void sweepDetachedInstances(MinecraftServer server) {
        for (DungeonInstanceRecord record : DungeonPositionSavedData.get(server).getInstancesSnapshot()) {
            if (record.entranceClosed() && !record.pendingDeletion()) {
                maybeReleaseInstance(server, record.instanceId());
            }
        }
    }

    private static boolean isOccupied(MinecraftServer server, DungeonInstanceRecord record) {
        return !occupantsOf(server, record).isEmpty();
    }

    /**
     * How many players count as being in this instance - <b>the same reading the release sweep
     * uses</b>, deliberately exposed rather than reimplemented.
     * <p>
     * An earlier version had {@code /cs dungeon list} count occupants with its own loop over
     * clause (a) only. It reported zero while the sweep still saw an occupant through clause (c),
     * so the command said "empty" about a region that would never be released - two answers to one
     * question, and the operator had no way to tell which was lying.
     */
    public static int countOccupants(MinecraftServer server, DungeonInstanceRecord record) {
        return occupantsOf(server, record).size();
    }

    /**
     * Who counts as being in this instance (plan 182 section 5.2).
     * <p>
     * Occupancy is read with plan 181's scope resolver, never with the record's AABB: plan 179
     * established that captured bounds can be <em>narrower</em> than the walkable jigsaw, so a player
     * exploring far from the anchor would fall outside the box and the region would be erased under
     * them - plan 43's flaw 1, verbatim.
     * <p>
     * The three clauses distinguish <em>absent</em> from <em>gone</em>. Nobody is absent-not-gone
     * today; Feature B's nominal path is, which is why the hook is built now rather than left as a
     * note that would go silently stale.
     */
    private static List<ServerPlayer> occupantsOf(MinecraftServer server, DungeonInstanceRecord record) {
        String key = TimerScope.of(record.dimensionId(), record.instanceId()).key();
        List<ServerPlayer> occupants = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String dim = player.level().dimension().location().toString();
            // Outside a dungeon dimension a player has left. The overworld holds nothing.
            // This asks whether they are GONE, not whether the dimension is one of ours: two
            // different questions, and clause (c) below is where the difference bites.
            if (!DungeonDimensions.isDungeonDimension(dim)) {
                continue;
            }

            TimerScope scope = TimerManager.currentScope(player);

            // (a) standing in it. Covers bypassed staff, who hold no session at all.
            if (scope != null && key.equals(scope.key())) {
                occupants.add(player);
                continue;
            }

            if (scope != null) {
                // (b) away in a nested visit that names this one as its parent.
                PlayerTimerData current = TimerManager.session(player, scope);
                if (current != null && key.equals(current.getParentScopeId())) {
                    occupants.add(player);
                }
            } else if (!dim.equals(record.dimensionId())
                    && DungeonDimensions.isInstancedDungeonDimension(dim)
                    && TimerManager.session(player, TimerScope.parse(key)) != null) {
                // (c) FAILSAFE: holds a live session here, and stands in an instanced dungeon
                // dimension that is NOT this one, whose own instance does not resolve. We cannot
                // say where they are, so we do not erase under them.
                //
                // isInstancedDungeonDimension, not isDungeonDimension (plan 182, correctif C10).
                // A null scope carries TWO different facts and only one of them is "we lost
                // them": in a dimension with no timer entry currentScope returns null
                // structurally, always, on its very first branch - and every externally managed
                // dungeon is such a dimension, because DungeonRegistrationAPI skips
                // ensureDimensionEntry for them. Without this condition every player standing in
                // a Raid Den counted as an occupant of every detached instance they still held a
                // session for, and the pin fed itself: the session retained the instance, and the
                // instance retained the session, since purgeInstance runs only from
                // scheduleRegionClear. This predicate asks the question the clause means to ask -
                // could an instance of ours exist where this player stands?
                //
                // The "not this one" condition is what stops the clause swallowing an ordinary
                // departure. A session outlives the visit by design (plan 181), so a player who
                // left, then teleported back into this dimension far from any structure, still
                // holds the session - and without this check they would pin the region forever
                // while the command line correctly reported zero occupants. Inside this very
                // dimension, proximity is already the answer: clause (a) resolves anyone who is
                // genuinely in the structure, via the footprint, pass 3, or the sticky net.
                occupants.add(player);
            }
        }
        return occupants;
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

        
        maxigregrze.cobblesafari.objectives.ObjectivesManager.purgeInstance(
                server, record.dimensionId(), record.instanceId());
        maxigregrze.cobblesafari.data.HyperspaceZoneSavedData.get(server).remove(record.instanceId());

        // Order matters (plan 181 § 7): evacuate FIRST, purge the timer sessions after. The
        // evacuation reads each player's session to send them back to their own entry point, so
        // purging first would strand them at world spawn instead.
        evacuatePlayersFromRegion(dungeonLevel, record);

        maxigregrze.cobblesafari.manager.TimerManager.purgeInstance(
                server, record.dimensionId(), record.instanceId());

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

        // Collect first, teleport after (same as PortalSpawnManager): the teleport removes the
        // player from dungeonLevel.players(), which must not be iterated while that happens.
        List<ServerPlayer> playersInRegion = new ArrayList<>();
        for (ServerPlayer player : dungeonLevel.players()) {
            BlockPos pos = player.blockPosition();
            if (pos.getX() >= blockMinX && pos.getX() <= blockMaxX
                    && pos.getZ() >= blockMinZ && pos.getZ() <= blockMaxZ
                    && pos.getY() >= minY && pos.getY() <= maxY) {
                playersInRegion.add(player);
            }
        }
        for (ServerPlayer player : playersInRegion) {
            // Scope resolved "by record" (plan 181 § 3.5): a player at the edge of a region
            // being erased is precisely the one whose position resolves least reliably.
            // A bypassed player is evacuated too - this is not a permission check, these
            // blocks are about to stop existing.
            TimerManager.expireSessionAndTeleport(
                    player,
                    TimerScope.of(record.dimensionId(), record.instanceId()),
                    true
            );
        }
    }

}
