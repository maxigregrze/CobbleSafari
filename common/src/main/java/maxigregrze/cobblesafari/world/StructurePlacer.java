package maxigregrze.cobblesafari.world;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.DungeonStructureBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Comparator;
import java.util.Optional;

public class StructurePlacer {

    /**
     * Chunk-loading ticket held while a structure is being assembled. Unlike {@code TicketType.FORCED}
     * it expires on its own: dungeon generation is spread over several ticks (position -> chunk load ->
     * jigsaw -> finalization) and there is no single exit point where the release could be guaranteed -
     * the player may disconnect, move away, or the portal may vanish in between. The lifespan matches
     * the generation watchdog in {@code DungeonTeleportHandler} (MAX_GENERATION_MS = 30 s).
     *
     * <p>{@code TicketType.FORCED} was used here until action plan 175 (D1): it was added at region
     * radius 0 and removed at radius 1, so the removal never matched and every generation leaked its
     * chunks until the next restart. It is also the type {@code /forceload} uses, which made mod-held
     * and admin-held chunks indistinguishable.
     */
    private static final TicketType<ChunkPos> STRUCTURE_GEN = TicketType.create(
            "cobblesafari_structure_gen", Comparator.comparingLong(ChunkPos::toLong), 600);

    /**
     * Chunk radius a jigsaw placement touches, and therefore the radius that must be ticketed
     * <b>and ready</b> before {@link #placeJigsawStructure} runs (action plan 187, C5).
     *
     * <p>It used to be an 8 written inside {@code forceLoadChunks} while both callers ticketed a
     * radius of 4, so the ring from 5 to 8 was never reserved and was generated synchronously by
     * the blocking {@code getChunk} below - 289 full chunk generations in one tick once the
     * dimension has a floor, features and scattered structures. Sharing the constant is what keeps
     * the ticket and the placement talking about the same area.</p>
     */
    public static final int STRUCTURE_PLACEMENT_RADIUS = 8;

    private StructurePlacer() {}

    /**
     * Whether every chunk a placement will touch is already loaded, <b>without generating any</b>.
     *
     * <p>{@code getChunkNow} returns null rather than building a chunk, which is the property - not
     * a guard - that makes this safe to call every tick from a generation state machine. Callers
     * ticket the same radius first, then poll this until it answers true; by then the blocking
     * {@code getChunk} calls inside {@link #placeJigsawStructure} are all cache hits.</p>
     *
     * @see #STRUCTURE_PLACEMENT_RADIUS
     */
    public static boolean areChunksReady(ServerLevel world, BlockPos center, int radius) {
        ChunkPos centerChunk = new ChunkPos(center);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (world.getChunkSource().getChunkNow(centerChunk.x + dx, centerChunk.z + dz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Reserves the chunks a placement will touch. Self-expiring, never {@code TicketType.FORCED}
     * (plan 175, D1). Pair it with {@link #areChunksReady} on a later tick rather than blocking.
     */
    public static void ticketPlacementChunks(ServerLevel world, BlockPos center, int radius) {
        ChunkPos centerChunk = new ChunkPos(center);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                world.getChunkSource().addRegionTicket(STRUCTURE_GEN, cp, 0, cp);
            }
        }
    }

    public static boolean placeStructure(ServerLevel world, BlockPos pos, String structureId) {
        ResourceLocation structureResource = ResourceLocation.parse(structureId);
        StructureTemplateManager structTemplateManager = world.getServer().getStructureManager();
        Optional<StructureTemplate> structureTemplate = structTemplateManager.get(structureResource);

        if (structureTemplate.isPresent()) {
            StructurePlaceSettings structPlacementData = new StructurePlaceSettings()
                    .setIgnoreEntities(false)
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .setKnownShape(true);

            if (structureTemplate.get().placeInWorld(world, pos, pos, structPlacementData, world.random, 2)) {
                CobbleSafari.LOGGER.debug("Successfully placed structure: {} at {},{},{}", 
                        structureId, pos.getX(), pos.getY(), pos.getZ());
                return true;
            } else {
                CobbleSafari.LOGGER.warn("Error placing structure: {}", structureResource);
                return false;
            }
        } else {
            CobbleSafari.LOGGER.warn("Failed to load structure: {}", structureResource);
            return false;
        }
    }

    public static boolean placeJigsawStructure(ServerLevel world, BlockPos pos, String poolId, int depth) {
        CobbleSafari.LOGGER.info("Jigsaw: Starting generation at {} with pool {} and depth {} in dimension {}", 
                pos, poolId, depth, world.dimension().location());

        forceLoadChunks(world, pos, STRUCTURE_PLACEMENT_RADIUS);

        String command = String.format("execute in %s run place jigsaw %s %s %d %d %d %d",
                world.dimension().location(), poolId, "cobblesafari:start", depth, 
                pos.getX(), pos.getY(), pos.getZ());

        CobbleSafari.LOGGER.info("Jigsaw: Executing command: /{}", command);
        
        var server = world.getServer();
        var source = server.createCommandSourceStack()
                .withLevel(world)
                .withPosition(net.minecraft.world.phys.Vec3.atCenterOf(pos))
                .withPermission(4);

        try {
            DungeonStructureBounds.beginCapture();
            server.getCommands().performPrefixedCommand(source, command);
            CobbleSafari.LOGGER.info("Jigsaw: Command executed");
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("Jigsaw: Command failed: {}", e.getMessage());
            return placeStructure(world, pos, "cobblesafari:underground/start/underground_entrance");
        } finally {
            DungeonStructureBounds.endCapture();
        }

        return true;
    }

    /**
     * Same as {@link #placeJigsawStructure} but does not fall back to placing the underground entrance template on failure.
     */
    public static boolean placeJigsawStructureStrict(ServerLevel world, BlockPos pos, String poolId, int depth) {
        CobbleSafari.LOGGER.info("Jigsaw (strict): generation at {} pool {} depth {} dim {}",
                pos, poolId, depth, world.dimension().location());

        forceLoadChunks(world, pos, 3);

        String command = String.format("execute in %s run place jigsaw %s %s %d %d %d %d",
                world.dimension().location(), poolId, "cobblesafari:start", depth,
                pos.getX(), pos.getY(), pos.getZ());

        var server = world.getServer();
        var source = server.createCommandSourceStack()
                .withLevel(world)
                .withPosition(net.minecraft.world.phys.Vec3.atCenterOf(pos))
                .withPermission(4);

        try {
            DungeonStructureBounds.beginCapture();
            server.getCommands().performPrefixedCommand(source, command);
            return true;
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("Jigsaw (strict): command failed: {}", e.getMessage());
            return false;
        } finally {
            DungeonStructureBounds.endCapture();
        }
    }

    private static void forceLoadChunks(ServerLevel world, BlockPos center, int radius) {
        ChunkPos centerChunk = new ChunkPos(center);
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                world.getChunkSource().addRegionTicket(STRUCTURE_GEN, cp, 0, cp);
                world.getChunk(cp.x, cp.z);
                count++;
            }
        }
        CobbleSafari.LOGGER.info("Jigsaw: Force-loaded {} chunks around {}", count, center);
    }

    public static BlockPos getStructureSize(ServerLevel world, String structureId) {
        ResourceLocation structureResource = ResourceLocation.parse(structureId);
        StructureTemplateManager structTemplateManager = world.getServer().getStructureManager();
        Optional<StructureTemplate> structureTemplate = structTemplateManager.get(structureResource);

        if (structureTemplate.isPresent()) {
            var size = structureTemplate.get().getSize();
            return new BlockPos(size.getX(), size.getY(), size.getZ());
        }
        return BlockPos.ZERO;
    }
}
