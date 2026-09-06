package maxigregrze.cobblesafari.dungeon.sub;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.dungeon.UltraWormholeBlock;
import maxigregrze.cobblesafari.block.dungeon.UltraWormholeBlockEntity;
import maxigregrze.cobblesafari.data.DungeonInstanceRecord;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.DungeonInstanceCleanup;
import maxigregrze.cobblesafari.dungeon.DungeonStructureBounds;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The staged generation of one sub-instance (action plan 183, B3).
 *
 * <p>Deliberately <b>not</b> a reuse of {@code DungeonTeleportHandler}'s five stages: those are
 * keyed on a {@code DungeonPortalBlockEntity} throughout - they index on {@code getPortalId()},
 * write {@code setDungeonStructurePos} and call {@code PortalSpawnManager.updatePortalChunkBounds}
 * - and a wormhole confirm has no portal block. Reopening that file, which carries the fixes of
 * plans 43, 116, 138, 145 C11, 179 and 182, to deliver a new feature is not a defensible trade.
 * The real duplication is four calls to shared code, not four copies.</p>
 *
 * <p>Every phase runs on its own tick. Nothing here may call a blocking chunk API on the server
 * thread (plan 138, V1), and the marker scan in particular is spread one chunk section per tick
 * rather than sweeping the whole captured footprint at once.</p>
 */
public final class SubDungeonGeneration {

    private SubDungeonGeneration() {}

    /** Force-release threshold, matching the portal generation lock's (plan 145, C11). */
    private static final long WATCHDOG_MS = 30_000L;

    /** How much of the footprint the marker scan reads per tick: one chunk section, 4096 states. */
    private static final int SECTION = 16;

    /** Bounded box searched around the anchor for the exit-portal marker. */
    private static final int PORTAL_SCAN_RADIUS = 8;
    private static final int PORTAL_SCAN_DOWN = 4;
    private static final int PORTAL_SCAN_UP = 8;

    private enum Phase { ALLOCATE, LOAD_CHUNKS, PLACE, PORTAL, SCAN_MARKERS, ENCOUNTER, REGISTER }

    /** One in-flight generation. The lock is <b>per player</b>: several riders share one device. */
    private static final Map<UUID, State> IN_FLIGHT = new ConcurrentHashMap<>();

    private static final class State {
        final SubDungeonRequest request;
        final DungeonConfig config;
        final ServerLevel level;
        final long startedMs = System.currentTimeMillis();

        Phase phase = Phase.ALLOCATE;
        int slot = -1;
        BlockPos anchor;
        BoundingBox bounds;
        int chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ;
        BlockPos exitPortal;
        Direction exitFacing = Direction.NORTH;
        /** Set once blocks exist in the world, so an abort can erase them instead of leaking them. */
        boolean structurePlaced;

        // Marker scan cursor, walked one section per tick over the captured footprint.
        int scanX, scanZ, scanY, scanMinY, scanMaxY;
        final List<BlockPos> markers = new ArrayList<>();

        State(SubDungeonRequest request, DungeonConfig config, ServerLevel level) {
            this.request = request;
            this.config = config;
            this.level = level;
        }
    }

    public static boolean isGenerating(UUID playerId) {
        return IN_FLIGHT.containsKey(playerId);
    }

    /**
     * Starts a generation. The caller has already consumed the once-per-run entry and run every
     * check of section 8.1: this method only fails on server-side conditions, and each of those
     * refunds (section 8.4).
     */
    public static boolean begin(MinecraftServer server, SubDungeonRequest request) {
        DungeonConfig config = DungeonDimensions.getDungeonById("dungeon_ultraspace");
        if (config == null) {
            CobbleSafari.LOGGER.error("[SubDungeon] dungeon_ultraspace is not registered");
            return false;
        }
        ServerLevel level = server.getLevel(config.getDimensionKey());
        if (level == null) {
            CobbleSafari.LOGGER.error("[SubDungeon] dimension {} is not loaded", config.getDimensionId());
            return false;
        }
        if (IN_FLIGHT.putIfAbsent(request.playerId(), new State(request, config, level)) != null) {
            CobbleSafari.LOGGER.warn("[SubDungeon] {} already has a generation in flight", request.playerId());
            return false;
        }
        return true;
    }

    /**
     * Aborts and releases the lock.
     *
     * <p>Freeing the slot is not enough once {@code PLACE} has run: the blocks are already in the
     * world, and a slot freed under a standing structure is exactly the ghost region plans 116 and
     * 138 exist to prevent. So an abort past that point registers the region and hands it to the
     * ordinary clear pipeline instead, which erases it and frees the slot on its own.</p>
     */
    public static void abort(MinecraftServer server, UUID playerId, String reason) {
        State state = IN_FLIGHT.remove(playerId);
        if (state == null) {
            return;
        }
        CobbleSafari.LOGGER.warn("[SubDungeon] generation aborted for {}: {}", playerId, reason);
        if (state.structurePlaced) {
            DungeonInstanceRecord orphan = DungeonInstanceRecord.createSub(
                    state.config.getDimensionId(), state.slot, state.anchor,
                    state.chunkMinX, state.chunkMinZ, state.chunkMaxX, state.chunkMaxZ,
                    state.config.getClearSectionsBelow(), state.config.getClearSectionsAbove(),
                    state.config.getZoneSize(),
                    state.request.parentInstanceId(), playerId, 0,
                    state.request.typeId(), state.request.tier(), state.request.distance());
            DungeonPositionSavedData.get(server).putInstance(orphan);
            DungeonInstanceCleanup.forceReleaseInstance(server, orphan.instanceId());
        } else if (state.slot >= 0) {
            DungeonPositionSavedData.get(server).freeSlot(state.config.getDimensionId(), state.slot);
        }
        SubDungeonManager.onGenerationFailed(server, playerId, state.request);
    }

    public static void tick(MinecraftServer server) {
        if (IN_FLIGHT.isEmpty()) {
            return;
        }
        for (UUID playerId : List.copyOf(IN_FLIGHT.keySet())) {
            State state = IN_FLIGHT.get(playerId);
            if (state == null) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                abort(server, playerId, "player left mid-generation");
                continue;
            }
            try {
                advance(server, player, state);
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[SubDungeon] generation threw for {}", playerId, e);
                abort(server, playerId, "exception during " + state.phase);
            }
        }
    }

    /** Force-releases a lock held too long, so a failed entry cannot soft-lock the run (145 C11). */
    public static void sweepStaleSubGenerations(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, State> entry : Map.copyOf(IN_FLIGHT).entrySet()) {
            if (now - entry.getValue().startedMs > WATCHDOG_MS) {
                abort(server, entry.getKey(), "watchdog: generation exceeded " + (WATCHDOG_MS / 1000) + "s");
            }
        }
    }

    public static void onServerStopping() {
        IN_FLIGHT.clear();
    }

    private static void advance(MinecraftServer server, ServerPlayer player, State state) {
        switch (state.phase) {
            case ALLOCATE -> {
                DungeonPositionSavedData positions = DungeonPositionSavedData.get(server);
                String dimensionId = state.config.getDimensionId();
                int zoneSize = state.config.getZoneSize();
                state.slot = positions.allocateSlot(dimensionId);
                int zOffset = positions.getAndIncrementSlotZOffset(dimensionId, state.slot, zoneSize);
                int x = (state.slot % 100) * zoneSize;
                int z = (state.slot / 100) * zoneSize + zOffset;
                int y = state.config.hasFixedStructureY()
                        ? state.config.getFixedStructureY()
                        : ((state.slot % 2 == 0) ? 96 : 32);
                state.anchor = new BlockPos(x, y, z);
                CobbleSafari.LOGGER.info("[SubDungeon] slot {} at {} for {}", state.slot, state.anchor, dimensionId);
                state.phase = Phase.LOAD_CHUNKS;
            }
            case LOAD_CHUNKS -> {
                // Self-expiring ticket, never TicketType.FORCED (plan 175, D1).
                state.level.getChunkSource().addRegionTicket(
                        TicketType.PORTAL, new ChunkPos(state.anchor), 4, state.anchor);
                state.phase = Phase.PLACE;
            }
            case PLACE -> {
                boolean placed;
                DungeonStructureBounds.beginCapture();
                try {
                    placed = StructurePlacer.placeJigsawStructure(
                            state.level, state.anchor, state.request.structurePool(), state.config.getJigsawDepth());
                    if (placed) {
                        state.bounds = DungeonStructureBounds.consume(state.anchor);
                    }
                } finally {
                    DungeonStructureBounds.endCapture();
                }
                if (!placed) {
                    abort(server, state.request.playerId(),
                            "structure pool " + state.request.structurePool() + " failed to place");
                    return;
                }
                state.structurePlaced = true;
                computeChunkBounds(state);
                prepareScan(state);
                state.phase = Phase.PORTAL;
            }
            case PORTAL -> {
                placeExitPortal(state);
                state.phase = Phase.SCAN_MARKERS;
            }
            case SCAN_MARKERS -> {
                if (scanOneSection(state)) {
                    state.phase = Phase.ENCOUNTER;
                }
            }
            case ENCOUNTER -> {
                SubDungeonEncounter.resolve(state.level, player, state.markers, state.request);
                state.phase = Phase.REGISTER;
            }
            case REGISTER -> {
                DungeonInstanceRecord record = SubDungeonManager.registerAndEnter(
                        server, player, state.request, state.config, state.slot, state.anchor,
                        state.chunkMinX, state.chunkMinZ, state.chunkMaxX, state.chunkMaxZ,
                        state.exitPortal, state.exitFacing);
                IN_FLIGHT.remove(state.request.playerId());
                if (record == null) {
                    // registerAndEnter registered the region before bailing, and handed it to the
                    // clear pipeline: the slot is freed by completeClear, never here.
                    SubDungeonManager.onGenerationFailed(server, state.request.playerId(), state.request);
                } else {
                    // Only now does the exit portal learn which world it belongs to.
                    if (state.exitPortal != null
                            && state.level.getBlockEntity(state.exitPortal) instanceof UltraWormholeBlockEntity portal) {
                        portal.setTypeId(state.request.typeId());
                    }
                    // Generation is what closes the screen - never a client acknowledgement (W6).
                    maxigregrze.cobblesafari.warpride.WarpRideManager.endHandoffSuccess(
                            server, state.request.playerId());
                }
            }
        }
    }

    /** V3 of plan 138: clear exactly the structure's real footprint, with a one-chunk margin. */
    private static void computeChunkBounds(State state) {
        if (state.bounds != null) {
            state.chunkMinX = (state.bounds.minX() >> 4) - 1;
            state.chunkMinZ = (state.bounds.minZ() >> 4) - 1;
            state.chunkMaxX = (state.bounds.maxX() >> 4) + 1;
            state.chunkMaxZ = (state.bounds.maxZ() >> 4) + 1;
        } else {
            int radius = state.config.getZoneSize() / 2 - 1;
            state.chunkMinX = (state.anchor.getX() - radius) >> 4;
            state.chunkMinZ = (state.anchor.getZ() - radius) >> 4;
            state.chunkMaxX = (state.anchor.getX() + radius) >> 4;
            state.chunkMaxZ = (state.anchor.getZ() + radius) >> 4;
        }
    }

    private static void prepareScan(State state) {
        if (state.bounds != null) {
            state.scanX = state.bounds.minX();
            state.scanZ = state.bounds.minZ();
            state.scanMinY = state.bounds.minY();
            state.scanMaxY = state.bounds.maxY();
        } else {
            state.scanX = state.anchor.getX() - PORTAL_SCAN_RADIUS;
            state.scanZ = state.anchor.getZ() - PORTAL_SCAN_RADIUS;
            state.scanMinY = state.anchor.getY() - PORTAL_SCAN_DOWN;
            state.scanMaxY = state.anchor.getY() + PORTAL_SCAN_UP;
        }
        state.scanY = state.scanMinY;
    }

    /**
     * Reads one {@value #SECTION}-cubed block of the footprint, then moves the cursor on.
     *
     * <p>Never generates: {@code getChunk(x, z, FULL, false)} returns {@code null} rather than
     * building a chunk, which is the property - not a guard - that correctif C8 of plan 182 had to
     * impose after a flag let a scan generate 4 225 chunks and take the server down.</p>
     *
     * @return true once the whole footprint has been walked
     */
    private static boolean scanOneSection(State state) {
        int maxX = state.bounds != null ? state.bounds.maxX() : state.anchor.getX() + PORTAL_SCAN_RADIUS;
        int maxZ = state.bounds != null ? state.bounds.maxZ() : state.anchor.getZ() + PORTAL_SCAN_RADIUS;

        int endX = Math.min(state.scanX + SECTION - 1, maxX);
        int endZ = Math.min(state.scanZ + SECTION - 1, maxZ);
        int endY = Math.min(state.scanY + SECTION - 1, state.scanMaxY);

        ChunkAccess chunk = state.level.getChunkSource().getChunk(state.scanX >> 4, state.scanZ >> 4,
                ChunkStatus.FULL, false);
        if (chunk != null) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = state.scanX; x <= endX; x++) {
                for (int z = state.scanZ; z <= endZ; z++) {
                    for (int y = state.scanY; y <= endY; y++) {
                        cursor.set(x, y, z);
                        if (state.level.getBlockState(cursor).is(ModBlocks.ULTRA_SPAWN_ANCHOR)) {
                            state.markers.add(cursor.immutable());
                        }
                    }
                }
            }
        }

        state.scanY += SECTION;
        if (state.scanY > state.scanMaxY) {
            state.scanY = state.scanMinY;
            state.scanX += SECTION;
            if (state.scanX > maxX) {
                state.scanX = state.bounds != null
                        ? state.bounds.minX()
                        : state.anchor.getX() - PORTAL_SCAN_RADIUS;
                state.scanZ += SECTION;
                return state.scanZ > maxZ;
            }
        }
        return false;
    }

    /**
     * Replaces the {@code hoopa_ring_portal} marker with the real exit portal - the same convention
     * the Hoopa dungeons already use. First occurrence wins: two exits in one structure would be a
     * second way out to reason about, for no gain.
     */
    private static void placeExitPortal(State state) {
        BlockPos marker = findPortalMarker(state);
        if (marker == null) {
            CobbleSafari.LOGGER.warn("[SubDungeon] no hoopa_ring_portal marker near {}, using default offset",
                    state.anchor);
            marker = state.anchor.offset(0, 2, -1);
        }
        BlockState existing = state.level.getBlockState(marker);
        Direction facing = existing.hasProperty(UltraWormholeBlock.FACING)
                ? existing.getValue(UltraWormholeBlock.FACING)
                : Direction.NORTH;
        state.exitFacing = facing;
        state.exitPortal = marker;
        state.level.setBlock(marker,
                ModBlocks.ULTRA_WORMHOLE.defaultBlockState().setValue(UltraWormholeBlock.FACING, facing),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
    }

    private static BlockPos findPortalMarker(State state) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -PORTAL_SCAN_DOWN; dy <= PORTAL_SCAN_UP; dy++) {
            for (int dx = -PORTAL_SCAN_RADIUS; dx <= PORTAL_SCAN_RADIUS; dx++) {
                for (int dz = -PORTAL_SCAN_RADIUS; dz <= PORTAL_SCAN_RADIUS; dz++) {
                    cursor.set(state.anchor.getX() + dx, state.anchor.getY() + dy, state.anchor.getZ() + dz);
                    if (state.level.getBlockState(cursor).is(ModBlocks.HOOPA_RING_PORTAL)) {
                        return cursor.immutable();
                    }
                }
            }
        }
        return null;
    }

    /** Where the arriving player is put: one block in front of the exit portal. */
    static BlockPos spawnBeside(BlockPos exitPortal, Direction facing) {
        return exitPortal.relative(facing.getOpposite());
    }

    static float yawFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case EAST -> -90.0f;
            default -> 180.0f;
        };
    }

}
