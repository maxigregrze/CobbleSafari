package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.EnumSet;
import java.util.List;

/**
 * Puts a cleared chunk's worldgen back (action plan 187, section 6).
 *
 * <p>{@link DungeonRegionClearer} can only <em>blank</em>: it swaps whole {@code LevelChunkSection}s
 * for air and re-derives the biome palette. That is enough while a dungeon floats clear of the
 * ground, and completely insufficient once it rests on it - the clear window then necessarily
 * reaches through the terrain and the decoration structures, and a slot reused afterwards would
 * generate over a crater. This class is what makes the low layout survivable.</p>
 *
 * <p><b>Why a per-chunk replay is faithful here, and would not be in an overworld.</b> The terrain
 * of these dimensions is evaluated <em>per column</em> - flat layers in two of them, a bounded
 * density function and surface rules in the others, with no carvers, no aquifers and no ore veins -
 * and their only decorative content is structure sets, which {@code placeInChunk} clips to the
 * chunk's own column. Regenerating one chunk in isolation therefore reproduces what vanilla would
 * have produced. That is not true of an overworld, and it is why
 * {@link DungeonConfig#restoresWorldgen()} is opt-in.</p>
 *
 * <p><b>Why this does not build a {@code WorldGenRegion}.</b> The obvious route is to call
 * {@code ChunkGenerator.applyBiomeDecoration}, which is what vanilla runs at the {@code FEATURES}
 * step - but it wants a {@code WorldGenRegion}, and a region is the wrong tool over chunks that are
 * already loaded and full. It resolves neighbours through {@code GenerationChunkHolder}s and demands
 * each one expose the status the step declares - the {@code FEATURES} step requires
 * {@code STRUCTURE_STARTS} out to a radius of <b>8</b> - and a chunk that was loaded from disk rather
 * than generated this session carries none of those per-status futures. The access throws before a
 * single structure is placed, which leaves exactly the failure that is easy to misread: the terrain
 * comes back, and nothing that stands on it does.</p>
 *
 * <p>So the structures are placed directly instead. {@code ServerLevel} is itself a
 * {@code WorldGenLevel}, the chunk still carries the structure references the clear never touched,
 * and {@code StructureStart.placeInChunk} is public - no region, no holders, no generation statuses,
 * and nothing that behaves differently for a loaded chunk than for a fresh one.</p>
 */
public final class DungeonWorldgenRestorer {

    private DungeonWorldgenRestorer() {}

    /**
     * Rebuilds one chunk: terrain, then the decoration structures standing on it, then light.
     *
     * <p>Returns quietly when anything it needs is missing rather than throwing - a chunk that
     * cannot be restored is a cosmetic loss on a region no player is standing in, and the clear
     * must still be allowed to finish and free its slot.</p>
     *
     * @return true when the chunk was rebuilt
     */
    public static boolean restoreChunk(ServerLevel level, ChunkPos pos) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        ChunkAccess access = level.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        if (!(access instanceof LevelChunk chunk)) {
            return false;
        }

        try {
            if (generator instanceof FlatLevelSource flat) {
                stampLayers(level, flat, chunk);
            } else if (generator instanceof NoiseBasedChunkGenerator noise) {
                stampTerrain(level, noise, chunk);
            } else {
                // Guarded by DungeonConfig.restoresWorldgen(), so reaching this means a dimension
                // was opted in whose generator cannot be replayed per chunk. Say so, don't guess.
                CobbleSafari.LOGGER.warn("[Restore] {} has a generator this cannot replay ({}); skipped",
                        level.dimension().location(), generator.getClass().getSimpleName());
                return false;
            }

            // Before placing anything that resolves a height: every placement modifier reads these,
            // and they are stale after the clear replaced whole sections.
            Heightmap.primeHeightmaps(chunk, EnumSet.of(
                    Heightmap.Types.MOTION_BLOCKING,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Heightmap.Types.OCEAN_FLOOR,
                    Heightmap.Types.WORLD_SURFACE));

            placeStructures(level, generator, chunk);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[Restore] failed for chunk {}", pos, e);
        }

        chunk.setUnsaved(true);
        relight(level, chunk);
        return true;
    }

    /**
     * Re-places every structure this chunk still references.
     *
     * <p>The clear only swapped sections, so the chunk's {@code structureReferences} survived it
     * intact - which is what makes this a replay rather than a re-roll. The same starts are resolved
     * and the same pieces are stamped back into the same columns, so a restored region is not merely
     * similar to a never-visited one, it is the same.</p>
     *
     * <p>The box is the chunk's own column: {@code placeInChunk} clips each piece to it, so a
     * structure straddling a border is completed by whichever chunks own the rest of it - all of
     * which are in the same clear region.</p>
     */
    private static void placeStructures(ServerLevel level, ChunkGenerator generator, LevelChunk chunk) {
        StructureManager structures = level.structureManager();
        if (!structures.shouldGenerateStructures()) {
            return;
        }
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ChunkPos pos = chunk.getPos();
        SectionPos section = SectionPos.of(pos, level.getMinSection());
        BoundingBox column = new BoundingBox(
                pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
                pos.getMaxBlockX(), level.getMaxBuildHeight() - 1, pos.getMaxBlockZ());

        int placed = 0;
        for (Structure structure : registry) {
            for (StructureStart start : structures.startsForStructure(section, structure)) {
                start.placeInChunk(level, structures, generator, level.getRandom(), column, pos);
                placed++;
            }
        }
        if (placed > 0) {
            CobbleSafari.LOGGER.debug("[Restore] chunk {}: replaced {} structure start(s)", pos, placed);
        }
    }

    /**
     * Recomputes the chunk's light after it has been emptied and refilled.
     *
     * <p>The clear swaps whole {@code LevelChunkSection}s and the restore writes blocks straight back
     * into them, neither of which goes through the block-update path the light engine listens on. The
     * stored lighting therefore still describes the <em>old</em> structure: a region rebuilt without
     * this keeps the shadow the dungeon used to cast, and hands that shadow to the next player the
     * chunk is sent to, because light travels with the chunk packet.</p>
     */
    private static void relight(ServerLevel level, LevelChunk chunk) {
        try {
            level.getChunkSource().getLightEngine().lightChunk(chunk, false);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[Restore] relight failed for chunk {}", chunk.getPos(), e);
        }
    }

    /**
     * Rebuilds the terrain of a noise-generated dimension, column by column, then runs its surface
     * rules.
     *
     * <p>Deliberately <b>not</b> a call to {@code fillFromNoise}: that hands back a
     * {@code CompletableFuture} scheduled on the background executor, and joining it would block the
     * server thread inside a per-tick cursor - the exact failure this whole pipeline is built to
     * avoid. {@code getBaseHeight} gives the same answer per column, synchronously, because the
     * density function is evaluated per column with no dependency on a neighbour.</p>
     *
     * <p>The {@code buildSurface} overload taken here is the one that accepts a
     * {@code WorldGenerationContext} rather than a {@code WorldGenRegion}, for the reason given on
     * the class: a region cannot be built safely over already-loaded chunks. {@code Blender.empty()}
     * is correct because nothing here is blending against old-format terrain.</p>
     */
    private static void stampTerrain(ServerLevel level, NoiseBasedChunkGenerator generator,
                                     LevelChunk chunk) {
        RandomState randomState = level.getChunkSource().randomState();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // The column the density function really produces, block by block (action plan 190, P3).
        // Filling from the world bottom up to getBaseHeight assumed terrain solid all the way down,
        // which a floating floor - the jungle's and the crater's - is not: it would have rebuilt a
        // 150-block pillar under every cleared column.
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                NoiseColumn column = generator.getBaseColumn(x, z, level, randomState);
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockState state = column.getBlock(y);
                    if (!state.isAir()) {
                        cursor.set(x, y, z);
                        chunk.setBlockState(cursor, state, false);
                    }
                }
            }
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(
                Heightmap.Types.MOTION_BLOCKING,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Heightmap.Types.OCEAN_FLOOR,
                Heightmap.Types.WORLD_SURFACE));

        // Through a write-enabled ImposterProtoChunk rather than the LevelChunk itself: a surface
        // rule that places a fluid (the jungle's puddles) asks for post-processing, which a
        // LevelChunk answers with one WARN per block - 31 000 lines for one jungle clear - and the
        // imposter answers with nothing. The writes still land in the wrapped chunk.
        generator.buildSurface(new ImposterProtoChunk(chunk, true),
                new WorldGenerationContext(generator, level),
                randomState,
                level.structureManager(),
                level.getBiomeManager(),
                level.registryAccess().registryOrThrow(Registries.BIOME),
                Blender.empty());
    }

    /**
     * Re-stamps a flat generator's layer stack over the chunk's 256 columns.
     *
     * <p>Mirrors what {@code FlatLevelSource.fillFromNoise} does, rather than calling it, for the
     * same reason as {@link #stampTerrain}: that method is asynchronous.</p>
     */
    private static void stampLayers(ServerLevel level, FlatLevelSource flat, LevelChunk chunk) {
        List<BlockState> layers = flat.settings().getLayers();
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();

        for (int i = 0; i < layers.size(); i++) {
            BlockState state = layers.get(i);
            if (state == null || state.isAir()) {
                continue;
            }
            int y = minY + i;
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    cursor.set(baseX + dx, y, baseZ + dz);
                    chunk.setBlockState(cursor, state, false);
                }
            }
        }
    }
}
