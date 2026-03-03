package maxigregrze.cobblesafari.world;

import maxigregrze.cobblesafari.CobbleSafari;
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

import java.util.Optional;

public class StructurePlacer {

    private StructurePlacer() {}

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

        forceLoadChunks(world, pos, 8);

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
            server.getCommands().performPrefixedCommand(source, command);
            CobbleSafari.LOGGER.info("Jigsaw: Command executed");
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("Jigsaw: Command failed: {}", e.getMessage());
            return placeStructure(world, pos, "cobblesafari:underground/start/underground_entrance");
        }

        return true;
    }

    private static void forceLoadChunks(ServerLevel world, BlockPos center, int radius) {
        ChunkPos centerChunk = new ChunkPos(center);
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                world.getChunkSource().addRegionTicket(TicketType.FORCED, cp, 0, cp);
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
