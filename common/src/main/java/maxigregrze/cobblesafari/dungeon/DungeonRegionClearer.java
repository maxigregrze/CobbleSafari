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
import java.util.concurrent.CompletableFuture;

public class DungeonRegionClearer {

    private static final int SECTION_HEIGHT = 16;
    private static final int CHUNK_LOAD_TICKET_LEVEL = 2;
    private static final TicketType<ChunkPos> DUNGEON_CLEAR = TicketType.create(
            "cobblesafari_clear", Comparator.comparingLong(ChunkPos::toLong), 600);

    private static final int CHUNK_LOAD_SUBMISSIONS_PER_TICK = 12;
    private static final int CHUNK_CLEAR_PER_TICK = 32;

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
            case SUBMIT_LOAD_FUTURES -> submitLoadFuturesBatch(server, job);
            case WAIT_LOAD_RESULT -> {
                return;
            }
            case CLEAR_CHUNKS -> clearChunkBatch(job);
            case FINALIZE -> finalizeJob(job);
        }
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
        job.setPhase(RegionClearJob.Phase.SUBMIT_LOAD_FUTURES);
    }

    private static void submitLoadFuturesBatch(MinecraftServer server, RegionClearJob job) {
        ServerLevel level = job.level();
        int submitted = 0;
        while (job.loadSubmitted() < job.positions().size() && submitted < CHUNK_LOAD_SUBMISSIONS_PER_TICK) {
            ChunkPos cp = job.positions().get(job.loadSubmitted());
            job.loadFutures().add(level.getChunkSource().getChunkFuture(cp.x, cp.z, ChunkStatus.FULL, true));
            job.incrementLoadSubmitted();
            submitted++;
        }

        if (job.loadSubmitted() >= job.positions().size()) {
            job.setPhase(RegionClearJob.Phase.WAIT_LOAD_RESULT);
            CompletableFuture<?>[] arr = job.loadFutures().toArray(new CompletableFuture[0]);
            CompletableFuture.allOf(arr).whenComplete((unused, throwable) -> server.execute(() -> {
                if (throwable != null) {
                    CobbleSafari.LOGGER.error("Dungeon region chunk loading completed with errors", throwable);
                }
                beginClearPhase(job);
            }));
        }
    }

    private static void beginClearPhase(RegionClearJob job) {
        RegionClearJob peek = PENDING_JOBS.peek();
        if (peek != job) {
            return;
        }
        if (job.phase() != RegionClearJob.Phase.WAIT_LOAD_RESULT) {
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

    private static void clearChunkBatch(RegionClearJob job) {
        ServerLevel level = job.level();
        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        int minY = job.minClearY();
        int maxY = job.maxClearY();

        int end = Math.min(job.clearCursor() + CHUNK_CLEAR_PER_TICK, job.positions().size());
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
            SUBMIT_LOAD_FUTURES,
            WAIT_LOAD_RESULT,
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
        private final List<CompletableFuture<?>> loadFutures = new ArrayList<>();
        private final Runnable onFullyCleared;

        private Phase phase = Phase.ADD_TICKETS;
        private int loadSubmitted;
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

        List<CompletableFuture<?>> loadFutures() {
            return loadFutures;
        }

        int loadSubmitted() {
            return loadSubmitted;
        }

        void incrementLoadSubmitted() {
            loadSubmitted++;
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
