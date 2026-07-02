package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

public class DungeonRegionClearer {

    private static final int SECTION_HEIGHT = 16;
    private static final int CHUNK_LOAD_TICKET_LEVEL = 2;
    private static final TicketType<ChunkPos> DUNGEON_CLEAR = TicketType.create(
            "cobblesafari_clear", Comparator.comparingLong(ChunkPos::toLong), 600);

    private static final int CHUNK_CLEAR_PER_TICK = 32;
    private static final int CHUNK_CLEAR_PER_TICK_DRAIN = 256;

    /** Ticks a job may wait for its chunks before yielding its queue slot to the next pending job (~10 s). */
    private static final int AWAIT_YIELD_TICKS = 200;

    private static final Queue<RegionClearJob> PENDING_JOBS = new ArrayDeque<>();

    private DungeonRegionClearer() {}

    public static void scheduleRegionClear(ServerLevel dungeonLevel,
                                           int chunkMinX, int chunkMinZ,
                                           int chunkMaxX, int chunkMaxZ,
                                           int structureY,
                                           int sectionsBelow, int sectionsAbove) {
        scheduleRegionClear(dungeonLevel, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                structureY, sectionsBelow, sectionsAbove, null);
    }

    public static void scheduleRegionClear(ServerLevel dungeonLevel,
                                           int chunkMinX, int chunkMinZ,
                                           int chunkMaxX, int chunkMaxZ,
                                           int structureY,
                                           int sectionsBelow, int sectionsAbove,
                                           @Nullable Runnable onFullyCleared) {
        List<ChunkPos> positions = buildChunkPositionList(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ);
        if (positions.isEmpty()) {
            CobbleSafari.LOGGER.warn("DungeonRegionClearer.scheduleRegionClear: empty chunk range, skipping");
            if (onFullyCleared != null) {
                dungeonLevel.getServer().execute(onFullyCleared);
            }
            return;
        }
        PENDING_JOBS.add(new RegionClearJob(dungeonLevel, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                structureY, sectionsBelow, sectionsAbove, positions, onFullyCleared));
    }

    public static void tick(MinecraftServer server) {
        RegionClearJob job = PENDING_JOBS.peek();
        if (job == null) {
            return;
        }

        ServerLevel level = job.level();
        if (level.getServer() != server || server.isStopped()) {
            PENDING_JOBS.poll();
            CobbleSafari.LOGGER.warn("DungeonRegionClearer: dropping stale region clear job (server mismatch or stopping)");
            return;
        }

        switch (job.phase()) {
            case ADD_TICKETS -> addAllDungeonClearTickets(job);
            case AWAIT_LOADED -> awaitChunksLoaded(job);
            case CLEAR_CHUNKS -> clearChunkBatch(server, job);
            case FINALIZE -> finalizeJob(job);
        }
    }

    /**
     * Per-tick clear budget. When no player is connected (notably the startup drain, where pending
     * clears are resumed before players join) we can afford to clear far more aggressively; under
     * player load we stay discreet so the clear never weighs on a tick.
     */
    private static int clearBudgetPerTick(MinecraftServer server) {
        return server.getPlayerCount() == 0 ? CHUNK_CLEAR_PER_TICK_DRAIN : CHUNK_CLEAR_PER_TICK;
    }

    private static void addAllDungeonClearTickets(RegionClearJob job) {
        int minClearY = job.structureY() - (job.sectionsBelow() * SECTION_HEIGHT);
        int maxClearY = job.structureY() + (job.sectionsAbove() * SECTION_HEIGHT);
        CobbleSafari.LOGGER.info(
                "Dungeon region clear started: dimension {}, chunks [{},{}] to [{},{}], {} chunks, vertical bounds Y [{},{}]",
                job.level().dimension().location(),
                job.chunkMinX(), job.chunkMinZ(), job.chunkMaxX(), job.chunkMaxZ(),
                job.positions().size(),
                minClearY, maxClearY);

        ServerLevel level = job.level();
        for (ChunkPos cp : job.positions()) {
            level.getChunkSource().addRegionTicket(DUNGEON_CLEAR, cp, CHUNK_LOAD_TICKET_LEVEL, cp);
        }
        job.setPhase(RegionClearJob.Phase.AWAIT_LOADED);
    }

    /**
     * Non-blocking wait. The DUNGEON_CLEAR tickets posted in {@link #addAllDungeonClearTickets} drive
     * the chunks to FULL asynchronously over the following ticks; here we only poll their readiness with
     * {@link net.minecraft.server.level.ServerChunkCache#getChunkNow} — which never blocks nor generates.
     * As long as a chunk isn't ready we simply return and retry next tick, so the main thread is never
     * parked (this is the fix for the ServerHangWatchdog crash caused by the old blocking getChunkFuture).
     */
    private static void awaitChunksLoaded(RegionClearJob job) {
        ServerLevel level = job.level();
        for (ChunkPos cp : job.positions()) {
            if (level.getChunkSource().getChunkNow(cp.x, cp.z) == null) {
                // Not ready yet — the tickets keep loading it in the background. If we have waited a
                // long time and other jobs are queued behind us, rotate this one to the back so a single
                // slow region can't stall the whole queue. The job is never abandoned: it keeps its
                // tickets and the instance stays pendingDeletion, so it is retried (eventually, or via the
                // startup drain). On a healthy server the level-2 tickets resolve well before this fires.
                if (job.incrementAwaitTicks() >= AWAIT_YIELD_TICKS && PENDING_JOBS.size() > 1) {
                    CobbleSafari.LOGGER.warn(
                            "DungeonRegionClearer: region [{},{}]..[{},{}] still loading after {} ticks; yielding to next pending job",
                            job.chunkMinX(), job.chunkMinZ(), job.chunkMaxX(), job.chunkMaxZ(), AWAIT_YIELD_TICKS);
                    job.resetAwaitTicks();
                    PENDING_JOBS.poll();
                    PENDING_JOBS.add(job);
                }
                return;
            }
        }
        beginClearPhase(job);
    }

    private static void beginClearPhase(RegionClearJob job) {
        RegionClearJob peek = PENDING_JOBS.peek();
        if (peek != job) {
            return;
        }
        if (job.phase() != RegionClearJob.Phase.AWAIT_LOADED) {
            return;
        }

        ServerLevel level = job.level();
        int minClearY = job.structureY() - (job.sectionsBelow() * SECTION_HEIGHT);
        int maxClearY = job.structureY() + (job.sectionsAbove() * SECTION_HEIGHT);

        int blockMinX = job.chunkMinX() << 4;
        int blockMinZ = job.chunkMinZ() << 4;
        int blockMaxX = (job.chunkMaxX() << 4) + 15;
        int blockMaxZ = (job.chunkMaxZ() << 4) + 15;
        AABB regionBounds = new AABB(
                blockMinX, minClearY, blockMinZ,
                blockMaxX, maxClearY, blockMaxZ
        );

        List<Player> playersInRegion = level.getEntitiesOfClass(Player.class, regionBounds);
        if (!playersInRegion.isEmpty()) {
            CobbleSafari.LOGGER.warn("WARNING: Clearing dungeon region (Y-bounded) with {} player(s) still inside! Players: {}",
                    playersInRegion.size(),
                    playersInRegion.stream().map(p -> p.getName().getString()).toList());
        }

        discardEntities(level, regionBounds);

        job.setMinClearY(minClearY);
        job.setMaxClearY(maxClearY);
        job.setClearCursor(0);
        job.setPhase(RegionClearJob.Phase.CLEAR_CHUNKS);
    }

    private static void clearChunkBatch(MinecraftServer server, RegionClearJob job) {
        ServerLevel level = job.level();
        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        int minY = job.minClearY();
        int maxY = job.maxClearY();

        int end = Math.min(job.clearCursor() + clearBudgetPerTick(server), job.positions().size());
        for (int i = job.clearCursor(); i < end; i++) {
            ChunkPos cp = job.positions().get(i);
            ChunkAccess access = level.getChunkSource().getChunk(cp.x, cp.z, ChunkStatus.FULL, false);
            if (!(access instanceof LevelChunk chunk)) {
                CobbleSafari.LOGGER.error(
                        "DungeonRegionClearer.clearChunkBatch: chunk ({},{}) not available after load, skipping", cp.x, cp.z);
                continue;
            }
            clearSingleChunkYBounded(level, biomeRegistry, chunk, cp.x, cp.z, minY, maxY);
        }
        job.setClearCursor(end);

        if (job.clearCursor() >= job.positions().size()) {
            job.setPhase(RegionClearJob.Phase.FINALIZE);
        }
    }

    private static void finalizeJob(RegionClearJob job) {
        ServerLevel level = job.level();
        removeChunkTickets(level, job.chunkMinX(), job.chunkMinZ(), job.chunkMaxX(), job.chunkMaxZ());

        for (ChunkPos cp : job.positions()) {
            level.getChunkSource().removeRegionTicket(DUNGEON_CLEAR, cp, CHUNK_LOAD_TICKET_LEVEL, cp);
        }

        CobbleSafari.LOGGER.info(
                "Dungeon region clear finished: dimension {}, chunks [{},{}] to [{},{}], vertical bounds Y [{},{}]",
                level.dimension().location(),
                job.chunkMinX(), job.chunkMinZ(), job.chunkMaxX(), job.chunkMaxZ(),
                job.minClearY(), job.maxClearY());

        // V2.3: persist the cleared chunks BEFORE freeing the instance/slot. The clear marked them
        // unsaved; without a flush here, a crash between freeing the instance record and the next
        // autosave would leave the old structure on disk with no instance tracking it (ghost geometry).
        level.getChunkSource().save(false);

        Runnable done = job.onFullyCleared();
        PENDING_JOBS.poll();
        if (done != null) {
            done.run();
        }
    }

    private static List<ChunkPos> buildChunkPositionList(int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ) {
        List<ChunkPos> positions = new ArrayList<>();
        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                positions.add(new ChunkPos(cx, cz));
            }
        }
        return positions;
    }

    public static void clearRegion(ServerLevel dungeonLevel, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ) {
        int chunkCount = (chunkMaxX - chunkMinX + 1) * (chunkMaxZ - chunkMinZ + 1);
        CobbleSafari.LOGGER.info(
                "Dungeon region clear started (sync): dimension {}, chunks [{},{}] to [{},{}], {} chunks, full height",
                dungeonLevel.dimension().location(),
                chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                chunkCount);

        int blockMinX = chunkMinX << 4;
        int blockMinZ = chunkMinZ << 4;
        int blockMaxX = (chunkMaxX << 4) + 15;
        int blockMaxZ = (chunkMaxZ << 4) + 15;
        AABB regionBounds = new AABB(
                blockMinX, dungeonLevel.getMinBuildHeight(), blockMinZ,
                blockMaxX, dungeonLevel.getMaxBuildHeight(), blockMaxZ
        );

        List<Player> playersInRegion = dungeonLevel.getEntitiesOfClass(Player.class, regionBounds);
        if (!playersInRegion.isEmpty()) {
            CobbleSafari.LOGGER.warn("WARNING: Clearing dungeon region with {} player(s) still inside! Players: {}",
                    playersInRegion.size(),
                    playersInRegion.stream().map(p -> p.getName().getString()).toList());
        }

        discardEntities(dungeonLevel, regionBounds);
        clearChunks(dungeonLevel, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ);
        removeChunkTickets(dungeonLevel, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ);

        CobbleSafari.LOGGER.info(
                "Dungeon region clear finished (sync): dimension {}, chunks [{},{}] to [{},{}], full height",
                dungeonLevel.dimension().location(),
                chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ);
    }

    public static void clearRegion(ServerLevel dungeonLevel, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ, int structureY) {
        clearRegion(dungeonLevel, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ, structureY,
                DungeonConfig.DEFAULT_CLEAR_SECTIONS_BELOW, DungeonConfig.DEFAULT_CLEAR_SECTIONS_ABOVE);
    }

    public static void clearRegion(ServerLevel dungeonLevel, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ,
                                   int structureY, int sectionsBelow, int sectionsAbove) {
        int minClearY = structureY - (sectionsBelow * SECTION_HEIGHT);
        int maxClearY = structureY + (sectionsAbove * SECTION_HEIGHT);
        int chunkCount = (chunkMaxX - chunkMinX + 1) * (chunkMaxZ - chunkMinZ + 1);
        CobbleSafari.LOGGER.info(
                "Dungeon region clear started (sync): dimension {}, chunks [{},{}] to [{},{}], {} chunks, vertical bounds Y [{},{}]",
                dungeonLevel.dimension().location(),
                chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                chunkCount,
                minClearY, maxClearY);

        int blockMinX = chunkMinX << 4;
        int blockMinZ = chunkMinZ << 4;
        int blockMaxX = (chunkMaxX << 4) + 15;
        int blockMaxZ = (chunkMaxZ << 4) + 15;
        AABB regionBounds = new AABB(
                blockMinX, minClearY, blockMinZ,
                blockMaxX, maxClearY, blockMaxZ
        );

        List<Player> playersInRegion = dungeonLevel.getEntitiesOfClass(Player.class, regionBounds);
        if (!playersInRegion.isEmpty()) {
            CobbleSafari.LOGGER.warn("WARNING: Clearing dungeon region (Y-bounded) with {} player(s) still inside! Players: {}",
                    playersInRegion.size(),
                    playersInRegion.stream().map(p -> p.getName().getString()).toList());
        }

        discardEntities(dungeonLevel, regionBounds);
        clearChunksYBounded(dungeonLevel, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ, minClearY, maxClearY);
        removeChunkTickets(dungeonLevel, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ);

        CobbleSafari.LOGGER.info(
                "Dungeon region clear finished (sync): dimension {}, chunks [{},{}] to [{},{}], vertical bounds Y [{},{}]",
                dungeonLevel.dimension().location(),
                chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                minClearY, maxClearY);
    }

    private static void discardEntities(ServerLevel level, AABB bounds) {
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, bounds);
        for (Entity entity : entities) {
            if (!(entity instanceof Player)) {
                entity.discard();
            }
        }
    }

    private static void clearChunks(ServerLevel level, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ) {
        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);

        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                ChunkAccess access = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(access instanceof LevelChunk chunk)) {
                    CobbleSafari.LOGGER.error(
                            "DungeonRegionClearer.clearChunks: chunk ({},{}) not available after futures resolved, skipping", cx, cz);
                    continue;
                }

                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section == null) continue;
                    if (!section.hasOnlyAir()) {
                        chunk.getSections()[i] = new LevelChunkSection(
                                new PalettedContainer<>(
                                        Block.BLOCK_STATE_REGISTRY,
                                        Blocks.AIR.defaultBlockState(),
                                        PalettedContainer.Strategy.SECTION_STATES
                                ),
                                biomePaletteForDungeonSection(biomeRegistry, level, cx, cz, i)
                        );
                    }
                }

                for (BlockPos bePos : List.copyOf(chunk.getBlockEntities().keySet())) {
                    chunk.removeBlockEntity(bePos);
                }
                chunk.setUnsaved(true);
            }
        }
    }

    private static void clearChunksYBounded(ServerLevel level, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ, int minY, int maxY) {
        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);

        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                ChunkAccess access = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(access instanceof LevelChunk chunk)) {
                    CobbleSafari.LOGGER.error(
                            "DungeonRegionClearer.clearChunksYBounded: chunk ({},{}) not available after futures resolved, skipping", cx, cz);
                    continue;
                }
                clearSingleChunkYBounded(level, biomeRegistry, chunk, cx, cz, minY, maxY);
            }
        }
    }

    private static void clearSingleChunkYBounded(ServerLevel level, Registry<Biome> biomeRegistry,
                                                  LevelChunk chunk, int cx, int cz, int minY, int maxY) {
        int minBuildHeight = level.getMinBuildHeight();

        LevelChunkSection[] sections = chunk.getSections();
        int minSectionIdx = Math.max(0, (minY - minBuildHeight) >> 4);
        int maxSectionIdx = Math.min(sections.length - 1, (maxY - minBuildHeight) >> 4);

        for (int i = minSectionIdx; i <= maxSectionIdx; i++) {
            LevelChunkSection section = sections[i];
            if (section == null) continue;
            if (!section.hasOnlyAir()) {
                chunk.getSections()[i] = new LevelChunkSection(
                        new PalettedContainer<>(
                                Block.BLOCK_STATE_REGISTRY,
                                Blocks.AIR.defaultBlockState(),
                                PalettedContainer.Strategy.SECTION_STATES
                        ),
                        biomePaletteForDungeonSection(biomeRegistry, level, cx, cz, i)
                );
            }
        }

        for (BlockPos bePos : List.copyOf(chunk.getBlockEntities().keySet())) {
            if (bePos.getY() >= minY && bePos.getY() <= maxY) {
                chunk.removeBlockEntity(bePos);
            }
        }
        chunk.setUnsaved(true);
    }

    private static PalettedContainer<Holder<Biome>> biomePaletteForDungeonSection(
            Registry<Biome> biomeRegistry,
            ServerLevel level,
            int chunkX,
            int chunkZ,
            int sectionIndex
    ) {
        int minBuildHeight = level.getMinBuildHeight();
        int y = minBuildHeight + sectionIndex * SECTION_HEIGHT + 8;
        BlockPos sample = new BlockPos((chunkX << 4) + 8, y, (chunkZ << 4) + 8);
        Holder<Biome> biome = level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                sample.getX() >> 2,
                sample.getY() >> 2,
                sample.getZ() >> 2,
                level.getChunkSource().randomState().sampler()
        );
        return new PalettedContainer<>(
                biomeRegistry.asHolderIdMap(),
                biome,
                PalettedContainer.Strategy.SECTION_BIOMES
        );
    }

    private static void removeChunkTickets(ServerLevel level, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ) {
        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                ChunkPos chunkPos = new ChunkPos(cx, cz);
                level.getChunkSource().removeRegionTicket(
                        TicketType.FORCED, chunkPos, 1, chunkPos
                );
            }
        }
    }

    private static final class RegionClearJob {
        enum Phase {
            ADD_TICKETS,
            AWAIT_LOADED,
            CLEAR_CHUNKS,
            FINALIZE
        }

        private final ServerLevel level;
        private final int chunkMinX;
        private final int chunkMinZ;
        private final int chunkMaxX;
        private final int chunkMaxZ;
        private final int structureY;
        private final int sectionsBelow;
        private final int sectionsAbove;
        private final List<ChunkPos> positions;
        private final Runnable onFullyCleared;

        private Phase phase = Phase.ADD_TICKETS;
        private int awaitTicks;
        private int minClearY;
        private int maxClearY;
        private int clearCursor;

        RegionClearJob(ServerLevel level,
                       int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ,
                       int structureY, int sectionsBelow, int sectionsAbove,
                       List<ChunkPos> positions,
                       @Nullable Runnable onFullyCleared) {
            this.level = level;
            this.chunkMinX = chunkMinX;
            this.chunkMinZ = chunkMinZ;
            this.chunkMaxX = chunkMaxX;
            this.chunkMaxZ = chunkMaxZ;
            this.structureY = structureY;
            this.sectionsBelow = sectionsBelow;
            this.sectionsAbove = sectionsAbove;
            this.positions = positions;
            this.onFullyCleared = onFullyCleared;
        }

        ServerLevel level() {
            return level;
        }

        List<ChunkPos> positions() {
            return positions;
        }

        Phase phase() {
            return phase;
        }

        void setPhase(Phase phase) {
            this.phase = phase;
        }

        int incrementAwaitTicks() {
            return ++awaitTicks;
        }

        void resetAwaitTicks() {
            this.awaitTicks = 0;
        }

        int structureY() {
            return structureY;
        }

        int sectionsBelow() {
            return sectionsBelow;
        }

        int sectionsAbove() {
            return sectionsAbove;
        }

        int chunkMinX() {
            return chunkMinX;
        }

        int chunkMinZ() {
            return chunkMinZ;
        }

        int chunkMaxX() {
            return chunkMaxX;
        }

        int chunkMaxZ() {
            return chunkMaxZ;
        }

        int minClearY() {
            return minClearY;
        }

        void setMinClearY(int minClearY) {
            this.minClearY = minClearY;
        }

        int maxClearY() {
            return maxClearY;
        }

        void setMaxClearY(int maxClearY) {
            this.maxClearY = maxClearY;
        }

        int clearCursor() {
            return clearCursor;
        }

        void setClearCursor(int clearCursor) {
            this.clearCursor = clearCursor;
        }

        @Nullable Runnable onFullyCleared() {
            return onFullyCleared;
        }
    }
}
