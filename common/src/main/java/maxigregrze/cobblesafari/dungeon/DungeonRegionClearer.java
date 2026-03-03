package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class DungeonRegionClearer {

    private static final int SECTION_HEIGHT = 16;
    private static final int SECTION_PADDING_BELOW = 1;
    private static final int SECTION_PADDING_ABOVE = 1;

    private DungeonRegionClearer() {}

    public static void clearRegion(ServerLevel dungeonLevel, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ) {
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

        CobbleSafari.LOGGER.info("Cleared dungeon region: chunks [{},{}] to [{},{}] in {}",
                chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ, dungeonLevel.dimension().location());
    }

    public static void clearRegion(ServerLevel dungeonLevel, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ, int structureY) {
        int minClearY = structureY - (SECTION_PADDING_BELOW * SECTION_HEIGHT);
        int maxClearY = structureY + (SECTION_PADDING_ABOVE * SECTION_HEIGHT);

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

        CobbleSafari.LOGGER.info("Cleared dungeon region (Y-bounded [{},{}]): chunks [{},{}] to [{},{}] in {}",
                minClearY, maxClearY, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ, dungeonLevel.dimension().location());
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
                LevelChunk chunk = level.getChunk(cx, cz);
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
                                new PalettedContainer<>(
                                        biomeRegistry.asHolderIdMap(),
                                        biomeRegistry.getHolderOrThrow(Biomes.THE_VOID),
                                        PalettedContainer.Strategy.SECTION_BIOMES
                                )
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
        int minBuildHeight = level.getMinBuildHeight();

        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
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
                                new PalettedContainer<>(
                                        biomeRegistry.asHolderIdMap(),
                                        biomeRegistry.getHolderOrThrow(Biomes.THE_VOID),
                                        PalettedContainer.Strategy.SECTION_BIOMES
                                )
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
        }
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
}
