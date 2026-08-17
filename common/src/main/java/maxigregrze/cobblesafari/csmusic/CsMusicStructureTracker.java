package maxigregrze.cobblesafari.csmusic;

import it.unimi.dsi.fastutil.longs.LongSet;
import maxigregrze.cobblesafari.config.CsMusicSettingsConfig;
import maxigregrze.cobblesafari.mixin.SinglePoolElementAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Answers "is this player standing inside a naturally generated structure (or one of its pieces)?"
 * for csmusic trigger rules. Resolving the structure starts referenced by a chunk is far too costly
 * for the per-tick condition sweep, so the expensive half is memoized per (dimension, chunk) and
 * shared between players, while the cheap bounding-box test runs live on the exact block position -
 * entering a structure is therefore detected without latency.
 *
 * <p>Only worldgen structures can match: the lookup reads the structure references written by
 * {@code ChunkGenerator.createReferences}, which a hand-built or template-pasted copy never has.</p>
 *
 * <p>Resolution never blocks: references come from the player's own chunk (always loaded) and start
 * chunks are read with {@code getChunkNow}, so an unloaded start chunk counts as "no structure here"
 * instead of being generated on the server thread.</p>
 */
public final class CsMusicStructureTracker {

    /** Fallback TTL (ticks) when the config is not loaded yet. */
    /** Lower bound on the configured TTL, to keep the chunk lookups bounded. */
    private static final int MIN_TTL_TICKS = 20;
    /** Sweep cadence for expired entries, in ticks. */
    private static final int PURGE_INTERVAL_TICKS = 200;
    /** Coarse cap on cache entries. */
    private static final int MAX_ENTRIES = 4096;
    /** Cap on cached pieces across all entries - entries are ~100x bigger once pieces are kept. */
    private static final int MAX_CACHED_PIECES = 200_000;

    /** One identifiable piece of a structure start. */
    private record PieceInfo(String id, BoundingBox box) {}

    /** One naturally generated structure start overlapping a chunk. */
    private record StartInfo(ResourceLocation id, Set<TagKey<Structure>> tags,
                             BoundingBox box, List<PieceInfo> pieces) {}

    private record ChunkKey(ResourceKey<Level> dimension, long chunkPos) {}

    private record CacheEntry(long expiryTick, List<StartInfo> starts, int pieceCount) {}

    /** Access-order map: iteration yields least-recently-used first, for eviction. */
    private static final Map<ChunkKey, CacheEntry> CACHE = new LinkedHashMap<>(64, 0.75f, true);
    /** Canonical id strings, so every piece sharing a template shares one String. */
    private static final Map<ResourceLocation, String> ID_POOL = new LinkedHashMap<>();

    private static int cachedPieces;

    private CsMusicStructureTracker() {}

    /** Row of {@code /cobblesafari csmusic structures}. */
    public record StructureInfo(String id, String tags, boolean insideBounds, boolean insidePiece,
                                List<String> pieceIds) {}

    // --- Condition evaluation --------------------------------------------------

    /** True if the player stands inside the bounds of a start accepted by {@code filter}. */
    public static synchronized boolean matches(ServerPlayer player, CsMusicStructureFilter filter) {
        BlockPos pos = player.blockPosition();
        for (StartInfo info : startsForChunkOf(player.serverLevel(), pos)) {
            if (info.box().isInside(pos) && filter.accepts(info.id(), info.tags())) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if the player stands inside a piece matching {@code piecePattern}. When
     * {@code structureFilter} is set, the piece must belong to a start that filter accepts - not
     * merely to some start that happens to overlap the same chunk.
     */
    public static synchronized boolean matchesPiece(ServerPlayer player,
                                                    @Nullable CsMusicStructureFilter structureFilter,
                                                    CsMusicPiecePattern piecePattern) {
        BlockPos pos = player.blockPosition();
        for (StartInfo info : startsForChunkOf(player.serverLevel(), pos)) {
            if (structureFilter != null && !structureFilter.accepts(info.id(), info.tags())) {
                continue;
            }
            for (PieceInfo piece : info.pieces()) {
                if (piece.box().isInside(pos) && piecePattern.matches(piece.id())) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Cache -----------------------------------------------------------------

    private static List<StartInfo> startsForChunkOf(ServerLevel level, BlockPos pos) {
        ChunkKey key = new ChunkKey(
                level.dimension(), ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        long now = level.getServer().getTickCount();
        CacheEntry cached = CACHE.get(key);      // access-order get: also refreshes the LRU rank
        if (cached != null && cached.expiryTick() > now) {
            return cached.starts();
        }
        List<StartInfo> resolved = resolve(level, pos);
        int pieces = 0;
        for (StartInfo info : resolved) {
            pieces += info.pieces().size();
        }
        CacheEntry previous = CACHE.put(key, new CacheEntry(now + ttlTicks(), resolved, pieces));
        cachedPieces += pieces - (previous == null ? 0 : previous.pieceCount());
        evictIfNeeded();
        return resolved;
    }

    private static List<StartInfo> resolve(ServerLevel level, BlockPos pos) {
        StructureManager manager = level.structureManager();
        // Reads the player's own chunk only (always loaded): structure -> chunks holding the start.
        Map<Structure, LongSet> references = manager.getAllStructuresAt(pos);
        if (references.isEmpty()) {
            return List.of();
        }
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ServerChunkCache chunks = level.getChunkSource();
        boolean withPieces = CsMusicTriggerRegistry.usesPieces();
        List<StartInfo> list = new ArrayList<>();
        for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
            Structure structure = entry.getKey();
            ResourceLocation id = registry.getKey(structure);
            if (id == null) {
                continue;
            }
            Set<TagKey<Structure>> tags = registry.wrapAsHolder(structure)
                    .tags().collect(Collectors.toUnmodifiableSet());
            for (long packed : entry.getValue()) {
                // Never block: an unloaded start chunk counts as "no structure here".
                LevelChunk chunk = chunks.getChunkNow(ChunkPos.getX(packed), ChunkPos.getZ(packed));
                if (chunk == null) {
                    continue;
                }
                StructureStart start = chunk.getStartForStructure(structure);
                if (start == null || !start.isValid()) {
                    continue;
                }
                list.add(new StartInfo(id, tags, start.getBoundingBox(),
                        withPieces ? collectPieces(start) : List.of()));
            }
        }
        return List.copyOf(list);
    }

    private static List<PieceInfo> collectPieces(StructureStart start) {
        List<PieceInfo> out = new ArrayList<>();
        for (StructurePiece piece : start.getPieces()) {
            ResourceLocation id = pieceId(piece);
            if (id != null) {
                out.add(new PieceInfo(canonicalId(id), piece.getBoundingBox()));
            }
        }
        return List.copyOf(out);
    }

    /**
     * Stable id of a structure piece, or null when the piece cannot be named. Jigsaw pieces are
     * named by their template (the piece type is always {@code minecraft:jigsaw}, which could not
     * discriminate two village houses); legacy pieces by their registered piece type.
     */
    @Nullable
    private static ResourceLocation pieceId(StructurePiece piece) {
        if (piece instanceof PoolElementStructurePiece jigsaw
                && jigsaw.getElement() instanceof SinglePoolElement single) {
            // A Right() template is an inline, unnamed structure - skip it.
            return ((SinglePoolElementAccessor) single).getTemplate().left().orElse(null);
        }
        return BuiltInRegistries.STRUCTURE_PIECE.getKey(piece.getType());
    }

    private static String canonicalId(ResourceLocation id) {
        return ID_POOL.computeIfAbsent(id, ResourceLocation::toString);
    }

    private static void evictIfNeeded() {
        Iterator<Map.Entry<ChunkKey, CacheEntry>> it = CACHE.entrySet().iterator();
        while ((CACHE.size() > MAX_ENTRIES || cachedPieces > MAX_CACHED_PIECES) && it.hasNext()) {
            cachedPieces -= it.next().getValue().pieceCount();
            it.remove();
        }
    }

    private static long ttlTicks() {
        return Math.max(MIN_TTL_TICKS, CsMusicSettingsConfig.data.structureCacheTtlTicks);
    }

    /** Drops expired entries. Called from the csmusic server tick. */
    public static synchronized void purge(MinecraftServer server) {
        if ((server.getTickCount() % PURGE_INTERVAL_TICKS) != 0) {
            return;
        }
        long now = server.getTickCount();
        Iterator<Map.Entry<ChunkKey, CacheEntry>> it = CACHE.entrySet().iterator();
        while (it.hasNext()) {
            CacheEntry entry = it.next().getValue();
            if (entry.expiryTick() <= now) {
                cachedPieces -= entry.pieceCount();
                it.remove();
            }
        }
    }

    /** Full reset - datapack reload / server start. */
    public static synchronized void clear() {
        CACHE.clear();
        ID_POOL.clear();
        cachedPieces = 0;
    }

    // --- Diagnostics (/cobblesafari csmusic structures) -------------------------

    /**
     * Fresh, uncached read of the structures around {@code pos}, with both "inside" answers and the
     * pieces containing {@code pos}. Uses the same non-blocking resolution as the condition, so the
     * command never reports a structure the rules cannot see.
     */
    public static synchronized List<StructureInfo> describeAt(ServerLevel level, BlockPos pos) {
        StructureManager manager = level.structureManager();
        Map<Structure, LongSet> references = manager.getAllStructuresAt(pos);
        if (references.isEmpty()) {
            return List.of();
        }
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ServerChunkCache chunks = level.getChunkSource();
        List<StructureInfo> out = new ArrayList<>();
        for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
            Structure structure = entry.getKey();
            ResourceLocation id = registry.getKey(structure);
            if (id == null) {
                continue;
            }
            String tags = registry.wrapAsHolder(structure)
                    .tags().map(t -> "#" + t.location()).collect(Collectors.joining(", "));
            for (long packed : entry.getValue()) {
                LevelChunk chunk = chunks.getChunkNow(ChunkPos.getX(packed), ChunkPos.getZ(packed));
                if (chunk == null) {
                    continue;
                }
                StructureStart start = chunk.getStartForStructure(structure);
                if (start == null || !start.isValid()) {
                    continue;
                }
                List<String> pieceIds = new ArrayList<>();
                for (StructurePiece piece : start.getPieces()) {
                    ResourceLocation pid = pieceId(piece);
                    if (pid != null && piece.getBoundingBox().isInside(pos)) {
                        pieceIds.add(pid.toString());
                    }
                }
                out.add(new StructureInfo(
                        id.toString(),
                        tags,
                        start.getBoundingBox().isInside(pos),
                        manager.structureHasPieceAt(pos, start),
                        List.copyOf(pieceIds)));
            }
        }
        return out;
    }
}
