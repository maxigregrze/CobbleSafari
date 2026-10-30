package maxigregrze.cobblesafari.dungeon.hyperspace;

import maxigregrze.cobblesafari.network.HyperspaceSkyMapPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Samples a {@value HyperspaceSkyMapPayload#MAP_SIZE}-block square of a level, one block per pixel,
 * into vanilla map colours (action plan 197, section 5.2).
 *
 * <ol>
 *   <li>loaded chunks are sampled as soon as the job is created;</li>
 *   <li>the others are read from disk, asynchronously, to learn whether they were ever fully
 *       generated;</li>
 *   <li>those that were are loaded, a few per tick, and sampled;</li>
 *   <li>after that, or after {@link #MAX_TICKS}, the colours are shaded and encoded.</li>
 * </ol>
 * A chunk that was never generated is never loaded: generating terrain for a sky picture would cost an
 * unpredictable amount with worldgen mods and write chunks nobody visited (decision 5.2).
 */
final class HyperspaceSkyMapJob {

    private static final int SIZE = HyperspaceSkyMapPayload.MAP_SIZE;
    private static final int LOADS_PER_TICK = 4;
    private static final int MAX_TICKS = 200;

    private final ServerLevel level;
    private final int minX;
    private final int minZ;

    private final byte[] colorIds = new byte[SIZE * SIZE];
    private final int[] heights = new int[SIZE * SIZE];
    private final byte[] waterDepths = new byte[SIZE * SIZE];
    private final BitSet sampled = new BitSet(SIZE * SIZE);

    private final Map<ChunkPos, CompletableFuture<Optional<CompoundTag>>> diskReads = new HashMap<>();
    private final Deque<ChunkPos> loadQueue = new ArrayDeque<>();
    private final Map<ChunkPos, CompletableFuture<ChunkResult<ChunkAccess>>> loads = new HashMap<>();
    private int ticks;
    private boolean finished;

    private HyperspaceSkyMapJob(ServerLevel level, int centerX, int centerZ) {
        this.level = level;
        this.minX = centerX - SIZE / 2;
        this.minZ = centerZ - SIZE / 2;
    }

    /** Creates the job and samples every chunk of the square that is already loaded. */
    static HyperspaceSkyMapJob start(ServerLevel level, int centerX, int centerZ) {
        HyperspaceSkyMapJob job = new HyperspaceSkyMapJob(level, centerX, centerZ);
        ServerChunkCache chunkSource = level.getChunkSource();
        for (ChunkPos chunkPos : job.chunksInSquare()) {
            ChunkAccess chunk = chunkSource.getChunkNow(chunkPos.x, chunkPos.z);
            if (chunk != null) {
                job.sampleChunk(chunk);
            } else {
                job.diskReads.put(chunkPos, chunkSource.chunkMap.read(chunkPos));
            }
        }
        return job;
    }

    boolean isFinished() {
        return finished;
    }

    void tick() {
        if (finished) {
            return;
        }
        ticks++;
        pollDiskReads();
        pollLoads();
        startLoads();
        if (ticks >= MAX_TICKS || (diskReads.isEmpty() && loadQueue.isEmpty() && loads.isEmpty())) {
            finished = true;
            diskReads.clear();
            loadQueue.clear();
            loads.clear();
        }
    }

    /** Share of the square with no data, in [0, 1]. Meaningful once finished. */
    double missingRatio() {
        int present = 0;
        for (int i = sampled.nextSetBit(0); i >= 0; i = sampled.nextSetBit(i + 1)) {
            if (colorIds[i] != 0) {
                present++;
            }
        }
        return 1.0 - present / (double) (SIZE * SIZE);
    }

    /**
     * Packed map colours, row by row from the north-west corner, shaded like a vanilla map at scale 0:
     * relief from the height of the pixel to the north, a checkerboard by depth for water.
     * 0 means no data.
     */
    byte[] encode() {
        byte[] packed = new byte[SIZE * SIZE];
        for (int z = 0; z < SIZE; z++) {
            for (int x = 0; x < SIZE; x++) {
                int index = z * SIZE + x;
                int colorId = colorIds[index] & 0xFF;
                if (!sampled.get(index) || colorId == 0) {
                    continue;
                }
                packed[index] = MapColor.byId(colorId).getPackedId(brightness(x, z, index, colorId));
            }
        }
        return packed;
    }

    private MapColor.Brightness brightness(int x, int z, int index, int colorId) {
        int checker = (x + z) & 1;
        if (colorId == MapColor.WATER.id) {
            double shade = waterDepths[index] * 0.1 + checker * 0.2;
            if (shade < 0.5) return MapColor.Brightness.HIGH;
            return shade > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        }
        int north = index - SIZE;
        int northHeight = z > 0 && sampled.get(north) ? heights[north] : heights[index];
        double relief = (heights[index] - northHeight) * 4.0 / 5.0 + (checker - 0.5) * 0.4;
        if (relief > 0.6) return MapColor.Brightness.HIGH;
        return relief < -0.6 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
    }

    private void pollDiskReads() {
        ServerChunkCache chunkSource = level.getChunkSource();
        Iterator<Map.Entry<ChunkPos, CompletableFuture<Optional<CompoundTag>>>> it = diskReads.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkPos, CompletableFuture<Optional<CompoundTag>>> entry = it.next();
            if (!entry.getValue().isDone()) {
                continue;
            }
            it.remove();
            ChunkPos chunkPos = entry.getKey();
            ChunkAccess loaded = chunkSource.getChunkNow(chunkPos.x, chunkPos.z);
            if (loaded != null) {
                sampleChunk(loaded);
                continue;
            }
            Optional<CompoundTag> tag = entry.getValue().exceptionally(e -> Optional.empty()).join();
            if (tag.isPresent() && ChunkSerializer.getChunkTypeFromTag(tag.get()) == ChunkType.LEVELCHUNK) {
                loadQueue.add(chunkPos);
            }
        }
    }

    private void pollLoads() {
        Iterator<Map.Entry<ChunkPos, CompletableFuture<ChunkResult<ChunkAccess>>>> it = loads.entrySet().iterator();
        while (it.hasNext()) {
            CompletableFuture<ChunkResult<ChunkAccess>> future = it.next().getValue();
            if (!future.isDone()) {
                continue;
            }
            it.remove();
            ChunkResult<ChunkAccess> result = future.exceptionally(e -> ChunkResult.error("failed")).join();
            ChunkAccess chunk = result.orElse(null);
            if (chunk != null) {
                sampleChunk(chunk);
            }
        }
    }

    private void startLoads() {
        ServerChunkCache chunkSource = level.getChunkSource();
        int started = 0;
        while (started < LOADS_PER_TICK && !loadQueue.isEmpty()) {
            ChunkPos chunkPos = loadQueue.poll();
            // FULL on a chunk already saved as full: loaded from disk, never generated.
            loads.put(chunkPos, chunkSource.getChunkFuture(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true));
            started++;
        }
    }

    private List<ChunkPos> chunksInSquare() {
        List<ChunkPos> chunks = new ArrayList<>();
        int maxX = minX + SIZE - 1;
        int maxZ = minZ + SIZE - 1;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                chunks.add(new ChunkPos(cx, cz));
            }
        }
        return chunks;
    }

    private void sampleChunk(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int localX = 0; localX < 16; localX++) {
            int x = chunkPos.getMinBlockX() + localX - minX;
            if (x < 0 || x >= SIZE) continue;
            for (int localZ = 0; localZ < 16; localZ++) {
                int z = chunkPos.getMinBlockZ() + localZ - minZ;
                if (z < 0 || z >= SIZE) continue;
                sampleColumn(chunk, pos, localX, localZ, z * SIZE + x);
            }
        }
    }

    /** Vanilla {@code MapItem.update} for one column, at one block per pixel. */
    private void sampleColumn(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int localX, int localZ, int index) {
        int worldX = chunk.getPos().getMinBlockX() + localX;
        int worldZ = chunk.getPos().getMinBlockZ() + localZ;
        int minY = chunk.getMinBuildHeight();
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) + 1;
        sampled.set(index);
        if (y <= minY + 1) {
            return; // nothing but void: no data
        }

        BlockState state;
        do {
            y--;
            pos.set(worldX, y, worldZ);
            state = chunk.getBlockState(pos);
        } while (state.getMapColor(chunk, pos) == MapColor.NONE && y > minY);

        int depth = 0;
        if (y > minY && !state.getFluidState().isEmpty()) {
            BlockPos.MutableBlockPos below = pos.mutable();
            int belowY = y - 1;
            BlockState belowState;
            do {
                below.setY(belowY--);
                belowState = chunk.getBlockState(below);
                depth++;
            } while (belowY > minY && !belowState.getFluidState().isEmpty());
            FluidState fluid = state.getFluidState();
            if (!state.isFaceSturdy(chunk, pos, Direction.UP)) {
                state = fluid.createLegacyBlock();
            }
        }

        colorIds[index] = (byte) state.getMapColor(chunk, pos).id;
        heights[index] = y;
        waterDepths[index] = (byte) Math.min(depth, 127);
    }
}
