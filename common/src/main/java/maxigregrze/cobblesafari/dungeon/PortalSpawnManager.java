package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.api.DungeonRegistrationAPI;
import maxigregrze.cobblesafari.api.PortalExpirationHandler;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import maxigregrze.cobblesafari.data.PortalSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PortalSpawnManager {

    private static final Map<UUID, ActivePortal> ACTIVE_PORTALS = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static long lastSpawnTick = 0;
    private static MinecraftServer serverInstance;
    private static volatile boolean suppressDungeonCleanup = false;

    private PortalSpawnManager() {}

    public static void setServer(MinecraftServer server) {
        serverInstance = server;
        loadPortalsFromWorld(server);
    }

    public static void clearServer() {
        savePortals();
        serverInstance = null;
        ACTIVE_PORTALS.clear();
        lastSpawnTick = 0;
    }

    private static void loadPortalsFromWorld(MinecraftServer server) {
        ACTIVE_PORTALS.clear();

        PortalSavedData savedData = PortalSavedData.get(server);
        Map<UUID, PortalSavedData.PortalData> savedPortals = savedData.getPortals();

        for (PortalSavedData.PortalData portalData : savedPortals.values()) {
            ServerLevel level = server.getLevel(portalData.dimension());
            if (level != null) {
                BlockEntity blockEntity = level.getBlockEntity(portalData.pos());
                if (blockEntity instanceof DungeonPortalBlockEntity portalEntity) {
                    if (portalEntity.getPortalId().equals(portalData.portalId())) {
                        ACTIVE_PORTALS.put(portalData.portalId(), new ActivePortal(
                                portalData.portalId(),
                                portalData.pos(),
                                portalData.dimension(),
                                portalData.spawnTick(),
                                portalData.dungeonId(),
                                portalData.dungeonDimensionId(),
                                portalData.dungeonStructurePos(),
                                portalData.chunkMinX(),
                                portalData.chunkMinZ(),
                                portalData.chunkMaxX(),
                                portalData.chunkMaxZ(),
                                portalData.hasDungeonChunkBounds()
                        ));
                        CobbleSafari.LOGGER.debug("Loaded portal {} from saved data at {}", portalData.portalId(), portalData.pos());
                    } else {
                        CobbleSafari.LOGGER.warn("Portal ID mismatch at {}: saved {}, found {}",
                                portalData.pos(), portalData.portalId(), portalEntity.getPortalId());
                    }
                } else {
                    CobbleSafari.LOGGER.warn("Portal block entity not found at {} in dimension {}, removing from saved data",
                            portalData.pos(), portalData.dimension().location());
                    savedData.removePortal(portalData.portalId());
                }
            }
        }

        scanWorldForPortals(server);

        CobbleSafari.LOGGER.info("Loaded {} active portals from world", ACTIVE_PORTALS.size());
    }

    private static void scanWorldForPortals(MinecraftServer server) {
        scanWorldForPortals(server, false);
    }

    public static int forceScanWorldForPortals(MinecraftServer server) {
        return scanWorldForPortals(server, true);
    }

    private static int scanWorldForPortals(MinecraftServer server, boolean forceLoad) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) return 0;

        int foundPortals = 0;

        for (int chunkX = -32; chunkX <= 32; chunkX++) {
            for (int chunkZ = -32; chunkZ <= 32; chunkZ++) {
                if (!forceLoad && !overworld.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                net.minecraft.world.level.chunk.ChunkAccess chunk = overworld.getChunk(chunkX, chunkZ);
                if (chunk instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk) {
                    for (BlockEntity blockEntity : levelChunk.getBlockEntities().values()) {
                        if (blockEntity instanceof DungeonPortalBlockEntity portalEntity) {
                            UUID portalId = portalEntity.getPortalId();
                            if (!ACTIVE_PORTALS.containsKey(portalId)) {
                                BlockPos pos = portalEntity.getBlockPos();
                                String dungeonId = portalEntity.getDungeonDimensionId();
                                if (dungeonId == null) {
                                    dungeonId = "dungeon_jump";
                                }

                                long spawnTick;
                                if (portalEntity.getSpawnTick() >= 0) {
                                    spawnTick = portalEntity.getSpawnTick();
                                } else {
                                    spawnTick = overworld.getGameTime() - (PortalSpawnConfig.getPortalLifetimeTicks() / 2);
                                }

                                ACTIVE_PORTALS.put(portalId, new ActivePortal(
                                        portalId,
                                        pos,
                                        overworld.dimension(),
                                        spawnTick,
                                        dungeonId
                                ));
                                foundPortals++;
                                CobbleSafari.LOGGER.debug("Found existing portal {} at {} not in saved data, adding to active list",
                                        portalId, pos);
                            }
                        }
                    }
                }
            }
        }

        if (foundPortals > 0) {
            CobbleSafari.LOGGER.info("Found {} additional portals in world not in saved data (forceLoad: {})", foundPortals, forceLoad);
            savePortals();
        }

        return foundPortals;
    }

    private static void savePortals() {
        if (serverInstance == null) return;

        PortalSavedData savedData = PortalSavedData.get(serverInstance);
        for (ActivePortal portal : ACTIVE_PORTALS.values()) {
            savedData.addPortal(portal.id(), portal.pos(), portal.dimension(), portal.spawnTick(), portal.dungeonId(),
                    portal.dungeonDimensionId(), portal.dungeonStructurePos(),
                    portal.dungeonChunkMinX(), portal.dungeonChunkMinZ(),
                    portal.dungeonChunkMaxX(), portal.dungeonChunkMaxZ(),
                    portal.hasDungeonChunkBounds());
        }
        savedData.setDirty();
        CobbleSafari.LOGGER.debug("Saved {} portals to persistent storage", ACTIVE_PORTALS.size());
    }

    public static List<String> getEnabledDungeonIdsForCycle() {
        List<String> ids = new ArrayList<>();
        for (DungeonConfig dungeon : DungeonDimensions.getAllDungeons()) {
            boolean enabled = PortalSpawnConfig.getDimensionConfig(dungeon.getId())
                    .map(DungeonDimensionEntry::isEnabled)
                    .orElse(true);
            if (enabled) {
                ids.add(dungeon.getId());
            }
        }
        ids.sort(Comparator.naturalOrder());
        return ids;
    }

    public static void tick(MinecraftServer server) {
        if (!PortalSpawnConfig.isEnabled()) return;

        long currentTick = server.overworld().getGameTime();

        if (currentTick - lastSpawnTick >= PortalSpawnConfig.getSpawnIntervalTicks()) {
            trySpawnRandomPortal(server);
            lastSpawnTick = currentTick;
        }

        cleanupExpiredPortals(server, currentTick);
    }

    private static void trySpawnRandomPortal(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        List<ServerPlayer> overworldPlayers = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension() == Level.OVERWORLD) {
                overworldPlayers.add(player);
            }
        }

        if (overworldPlayers.isEmpty()) {
            CobbleSafari.LOGGER.debug("No players in overworld for portal spawn");
            return;
        }

        int maxAttempts = 3;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            ServerPlayer targetPlayer = overworldPlayers.get(RANDOM.nextInt(overworldPlayers.size()));
            BlockPos portalPos = findValidSpawnPosition(overworld, targetPlayer.blockPosition());

            if (portalPos != null) {
                spawnPortal(overworld, portalPos, targetPlayer, null);
                return;
            }
        }

        CobbleSafari.LOGGER.warn("Failed to spawn dungeon portal after {} attempts", maxAttempts);
    }

    public static boolean spawnPortalNearPlayer(ServerPlayer player) {
        return spawnPortalNearPlayer(player, false);
    }

    public static boolean spawnPortalNearPlayer(ServerPlayer player, boolean force) {
        return spawnPortalNearPlayer(player, force, null);
    }

    public static boolean spawnPortalNearPlayer(ServerPlayer player, boolean force, String dungeonId) {
        if (!(player.level() instanceof ServerLevel level)) {
            CobbleSafari.LOGGER.warn("Cannot spawn portal: player {} is not in a ServerLevel", player.getName().getString());
            return false;
        }

        if (level.dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.translatable("cobblesafari.command.dungeon.spawn.overworld_only"));
            CobbleSafari.LOGGER.warn("Cannot spawn portal: player {} is not in the Overworld", player.getName().getString());
            return false;
        }

        DungeonConfig fixedDungeon = null;
        if (dungeonId != null && !dungeonId.isBlank()) {
            fixedDungeon = DungeonDimensions.getDungeonById(dungeonId.trim());
            if (fixedDungeon == null) {
                CobbleSafari.LOGGER.warn("Cannot spawn portal: unknown dungeon id {}", dungeonId);
                return false;
            }
        }

        BlockPos portalPos;
        if (force) {
            portalPos = player.blockPosition().above();
            CobbleSafari.LOGGER.info("Force spawning portal at player position: {}", portalPos);
        } else {
            portalPos = findValidSpawnPosition(level, player.blockPosition());
            if (portalPos == null) {
                CobbleSafari.LOGGER.warn("Failed to find valid spawn position near player {} at {}",
                        player.getName().getString(), player.blockPosition());
                return false;
            }
        }

        spawnPortal(level, portalPos, player, fixedDungeon);
        return true;
    }

    private static BlockPos findValidSpawnPosition(ServerLevel level, BlockPos center) {
        int minRadius = PortalSpawnConfig.getSpawnRadiusMin();
        int maxRadius = PortalSpawnConfig.getSpawnRadiusMax();

        CobbleSafari.LOGGER.debug("Searching for valid portal position near {} (radius: {}-{}) in dimension {}",
                center, minRadius, maxRadius, level.dimension().location());

        for (int attempt = 0; attempt < 20; attempt++) {
            int radius = minRadius + RANDOM.nextInt(maxRadius - minRadius);
            double angle = RANDOM.nextDouble() * 2 * Math.PI;

            int x = center.getX() + (int)(radius * Math.cos(angle));
            int z = center.getZ() + (int)(radius * Math.sin(angle));

            int y;
            try {
                y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            } catch (Exception e) {
                CobbleSafari.LOGGER.warn("Failed to get height at ({}, {}) in dimension {}: {}",
                        x, z, level.dimension().location(), e.getMessage());
                y = level.getMinBuildHeight() + 64;
            }

            if (y < level.getMinBuildHeight() || y > level.getMaxBuildHeight() - 3) {
                CobbleSafari.LOGGER.debug("Height {} out of bounds for dimension {} (min: {}, max: {})",
                        y, level.dimension().location(), level.getMinBuildHeight(), level.getMaxBuildHeight());
                continue;
            }

            BlockPos groundPos = new BlockPos(x, y, z);
            BlockPos portalPos = groundPos.above();

            String reason = isValidPortalPositionReason(level, portalPos);
            if (reason == null) {
                CobbleSafari.LOGGER.debug("Found valid portal position at {} (attempt {})", portalPos, attempt + 1);
                return portalPos;
            } else {
                CobbleSafari.LOGGER.debug("Position {} invalid (attempt {}): {}", portalPos, attempt + 1, reason);
            }
        }

        CobbleSafari.LOGGER.warn("Failed to find valid portal position after 20 attempts near {} in dimension {}",
                center, level.dimension().location());
        return null;
    }

    private static String isValidPortalPositionReason(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        var belowState = level.getBlockState(below);

        boolean isSolid = belowState.blocksMotion() || belowState.isFaceSturdy(level, below, net.minecraft.core.Direction.UP);
        boolean isAir = belowState.isAir();

        if (!isSolid && !isAir) {
            return String.format("Block below (%s) is not solid or air: %s", below, belowState.getBlock().getName().getString());
        }

        for (int dy = 0; dy < 3; dy++) {
            BlockPos checkPos = pos.above(dy);
            var checkState = level.getBlockState(checkPos);
            if (!checkState.isAir()) {
                return String.format("Block at %s (offset +%d) is not air: %s", checkPos, dy, checkState.getBlock().getName().getString());
            }
        }

        for (ActivePortal portal : ACTIVE_PORTALS.values()) {
            if (portal.pos().closerThan(pos, 50)) {
                return String.format("Too close to existing portal at %s (distance: %.1f)", portal.pos(), portal.pos().distSqr(pos));
            }
        }

        return null;
    }

    private static void spawnPortal(ServerLevel level, BlockPos pos, ServerPlayer nearestPlayer, DungeonConfig dungeonOrNull) {
        DungeonConfig dungeon = dungeonOrNull;
        if (dungeon == null) {
            dungeon = DungeonDimensions.getRandomDungeon();
        }
        if (dungeon == null) {
            CobbleSafari.LOGGER.error("No dungeon configuration available");
            return;
        }

        CobbleSafari.LOGGER.info("Attempting to spawn portal at {} in dimension {}", pos, level.dimension().location());

        Direction facing = Direction.fromYRot(RANDOM.nextFloat() * 360);
        BlockState portalState = ModBlocks.DUNGEON_PORTAL.defaultBlockState()
                .setValue(DungeonPortalBlock.PORTAL_TYPE, DungeonPortalBlock.PortalType.ENTRANCE)
                .setValue(DungeonPortalBlock.FACING, facing);

        var oldState = level.getBlockState(pos);
        CobbleSafari.LOGGER.debug("Current block at {}: {}", pos, oldState.getBlock().getName().getString());

        boolean setResult = level.setBlock(pos, portalState, 3);
        if (!setResult) {
            CobbleSafari.LOGGER.error("Failed to set block at {} - setBlock returned false", pos);
            return;
        }

        var newState = level.getBlockState(pos);
        if (!newState.is(ModBlocks.DUNGEON_PORTAL)) {
            CobbleSafari.LOGGER.error("Block at {} is not DUNGEON_PORTAL after placement! Got: {}", pos, newState.getBlock().getName().getString());
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DungeonPortalBlockEntity portalEntity) {
            portalEntity.setDungeonDimensionId(dungeon.getId());
            portalEntity.setSpawnTick(level.getGameTime());
            CobbleSafari.LOGGER.debug("Portal block entity configured successfully");
        } else {
            CobbleSafari.LOGGER.warn("Block entity at {} is not DungeonPortalBlockEntity! Got: {}", pos, blockEntity != null ? blockEntity.getClass().getName() : "null");
        }

        UUID portalId;
        if (blockEntity instanceof DungeonPortalBlockEntity portalEntity) {
            portalId = portalEntity.getPortalId();
        } else {
            portalId = UUID.randomUUID();
        }

        ACTIVE_PORTALS.put(portalId, new ActivePortal(
                portalId,
                pos,
                level.dimension(),
                level.getGameTime(),
                dungeon.getId(),
                dungeon.getId(),
                null,
                0, 0, 0, 0, false
        ));

        savePortals();

        if (blockEntity instanceof DungeonPortalBlockEntity portalEntity) {
            var callback = DungeonRegistrationAPI.getPortalSpawnCallback(dungeon.getId());
            if (callback != null) {
                try {
                    callback.onPortalSpawned(level, portalEntity);
                } catch (Exception e) {
                    CobbleSafari.LOGGER.error("Portal spawn callback failed for dungeon {}", dungeon.getId(), e);
                }
            }
        }

        notifyNearbyPlayers(level, pos);

        CobbleSafari.LOGGER.info("Successfully spawned dungeon portal at {} near player {}",
                pos, nearestPlayer.getName().getString());
    }

    private static void notifyNearbyPlayers(ServerLevel level, BlockPos portalPos) {
        int radius = PortalSpawnConfig.getNotificationRadius();

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(portalPos, radius)) {
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.portal.appeared",
                        portalPos.getX(), portalPos.getY(), portalPos.getZ()));
            }
        }
    }

    private static void cleanupExpiredPortals(MinecraftServer server, long currentTick) {
        int lifetime = PortalSpawnConfig.getPortalLifetimeTicks();
        List<UUID> expiredIds = new ArrayList<>();

        for (ActivePortal portal : ACTIVE_PORTALS.values()) {
            if (currentTick - portal.spawnTick() >= lifetime) {
                expiredIds.add(portal.id());
            }
        }

        for (UUID portalId : expiredIds) {
            ActivePortal portal = ACTIVE_PORTALS.get(portalId);
            if (portal == null) continue;

            ServerLevel level = server.getLevel(portal.dimension());
            if (level != null) {
                BlockEntity blockEntity = level.getBlockEntity(portal.pos());
                if (blockEntity instanceof DungeonPortalBlockEntity portalEntity && portalEntity.isAutoRenewPortal()) {
                    renewCreativePortal(server, level, portal.pos(), portalEntity);
                } else {
                    level.removeBlock(portal.pos(), false);
                    CobbleSafari.LOGGER.debug("Removed expired portal at {}", portal.pos());
                }
            } else {
                ACTIVE_PORTALS.remove(portalId);
                clearDungeonForPortal(server, portal);
                PortalSavedData.get(server).removePortal(portalId);
            }
        }
    }

    private static DungeonConfig resolveDungeonForPortalEntity(DungeonPortalBlockEntity portalEntity) {
        if (!portalEntity.isRandomDestinationMode()) {
            String fixedDungeonId = portalEntity.getFixedDungeonId();
            if (fixedDungeonId != null) {
                DungeonConfig fixedConfig = DungeonDimensions.getDungeonById(fixedDungeonId);
                if (fixedConfig != null) {
                    boolean enabled = PortalSpawnConfig.getDimensionConfig(fixedConfig.getId())
                            .map(DungeonDimensionEntry::isEnabled)
                            .orElse(true);
                    if (enabled) {
                        return fixedConfig;
                    }
                }
            }
        }
        return DungeonDimensions.getRandomDungeon();
    }

    public static boolean registerCreativePortal(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof DungeonPortalBlockEntity portalEntity)) {
            return false;
        }

        ACTIVE_PORTALS.values().removeIf(portal -> portal.pos().equals(pos) && portal.dimension().equals(level.dimension()));
        PortalSavedData savedData = PortalSavedData.get(level.getServer());
        List<UUID> idsToRemove = new ArrayList<>();
        for (PortalSavedData.PortalData portalData : savedData.getPortals().values()) {
            if (portalData.pos().equals(pos) && portalData.dimension().equals(level.dimension())) {
                idsToRemove.add(portalData.portalId());
            }
        }
        for (UUID id : idsToRemove) {
            savedData.removePortal(id);
        }

        DungeonConfig dungeon = resolveDungeonForPortalEntity(portalEntity);
        if (dungeon != null) {
            portalEntity.setDungeonDimensionId(dungeon.getId());
        } else {
            portalEntity.setDungeonDimensionId(null);
        }
        portalEntity.setSpawnTick(level.getGameTime());
        portalEntity.setChanged();

        UUID portalId = portalEntity.getPortalId();
        String resolvedDungeonDimensionId = dungeon != null ? dungeon.getId() : null;
        ACTIVE_PORTALS.put(portalId, new ActivePortal(
                portalId,
                pos,
                level.dimension(),
                level.getGameTime(),
                dungeon != null ? dungeon.getId() : "random",
                resolvedDungeonDimensionId,
                null,
                0, 0, 0, 0, false
        ));
        savePortals();

        if (dungeon != null) {
            var callback = DungeonRegistrationAPI.getPortalSpawnCallback(dungeon.getId());
            if (callback != null) {
                try {
                    callback.onPortalSpawned(level, portalEntity);
                } catch (Exception e) {
                    CobbleSafari.LOGGER.error("Portal spawn callback failed for dungeon {}", dungeon.getId(), e);
                }
            }
        }
        return true;
    }

    private static void renewCreativePortal(MinecraftServer server, ServerLevel level, BlockPos pos, DungeonPortalBlockEntity portalEntity) {
        UUID oldPortalId = portalEntity.getPortalId();
        ActivePortal oldPortal = ACTIVE_PORTALS.get(oldPortalId);

        onPortalBlockRemoved(portalEntity);

        UUID newPortalId = UUID.randomUUID();
        portalEntity.setPortalId(newPortalId);
        portalEntity.resetDungeonRuntimeData();

        DungeonConfig dungeon = resolveDungeonForPortalEntity(portalEntity);
        if (dungeon != null) {
            portalEntity.setDungeonDimensionId(dungeon.getId());
        } else {
            portalEntity.setDungeonDimensionId(null);
        }
        portalEntity.setSpawnTick(level.getGameTime());
        portalEntity.setChanged();

        String renewedDungeonDimensionId = dungeon != null ? dungeon.getId() : null;
        ACTIVE_PORTALS.put(newPortalId, new ActivePortal(
                newPortalId,
                pos,
                level.dimension(),
                level.getGameTime(),
                dungeon != null ? dungeon.getId() : (oldPortal != null ? oldPortal.dungeonId() : "random"),
                renewedDungeonDimensionId,
                null,
                0, 0, 0, 0, false
        ));
        savePortals();

        if (dungeon != null) {
            var callback = DungeonRegistrationAPI.getPortalSpawnCallback(dungeon.getId());
            if (callback != null) {
                try {
                    callback.onPortalSpawned(level, portalEntity);
                } catch (Exception e) {
                    CobbleSafari.LOGGER.error("Portal spawn callback failed for dungeon {}", dungeon.getId(), e);
                }
            }
        }
        CobbleSafari.LOGGER.info("Renewed creative portal at {} with new portal id {}", pos, newPortalId);
    }

    public static void removePortal(UUID portalId) {
        ActivePortal portal = ACTIVE_PORTALS.remove(portalId);
        if (portal != null && serverInstance != null) {
            PortalSavedData.get(serverInstance).removePortal(portalId);
        }
    }

    public static void onPortalBlockRemoved(DungeonPortalBlockEntity portalEntity) {
        UUID portalId = portalEntity.getPortalId();
        ActivePortal portal = ACTIVE_PORTALS.remove(portalId);
        DungeonTeleportHandler.removeInstance(portalId);

        if (serverInstance != null && !suppressDungeonCleanup) {
            String dungeonDimId = portalEntity.getDungeonDimensionId();

            if (dungeonDimId != null) {
                DungeonConfig config = DungeonDimensions.getDungeonById(dungeonDimId);
                if (config != null && config.isExternallyManaged()) {
                    PortalExpirationHandler handler = DungeonRegistrationAPI.getPortalExpirationHandler(dungeonDimId);
                    if (handler != null) {
                        handler.onPortalExpired(serverInstance, portalEntity, config);
                    }
                    if (portal != null) {
                        PortalSavedData.get(serverInstance).removePortal(portalId);
                    }
                    CobbleSafari.LOGGER.info("Portal {} removed (externally managed, block destroyed)", portalId);
                    return;
                }
            }

            if (portalEntity.hasDungeonChunkBounds() && dungeonDimId != null) {
                DungeonConfig config = DungeonDimensions.getDungeonById(dungeonDimId);
                if (config != null) {
                    ServerLevel dungeonLevel = serverInstance.getLevel(config.getDimensionKey());
                    if (dungeonLevel != null) {
                        evacuatePlayersFromDungeon(dungeonLevel, portalEntity, config);
                        
                        int structureY = portalEntity.getDungeonStructurePos() != null
                                ? portalEntity.getDungeonStructurePos().getY() : 64;
                        DungeonRegionClearer.clearRegion(dungeonLevel,
                                portalEntity.getDungeonChunkMinX(), portalEntity.getDungeonChunkMinZ(),
                                portalEntity.getDungeonChunkMaxX(), portalEntity.getDungeonChunkMaxZ(),
                                structureY, config.getClearSectionsBelow(), config.getClearSectionsAbove());
                    }
                }
                if (portalEntity.getDungeonStructurePos() != null) {
                    String dimensionId = config != null ? config.getDimensionId() : dungeonDimId;
                    int zoneSize = config != null ? config.getZoneSize() : DungeonConfig.DEFAULT_ZONE_SIZE;
                    DungeonPositionSavedData.get(serverInstance).removeUsedPosition(dimensionId, portalEntity.getDungeonStructurePos(), zoneSize);
                }
            }

            if (portal != null) {
                PortalSavedData.get(serverInstance).removePortal(portalId);
            }
            CobbleSafari.LOGGER.info("Portal {} removed from active list (block destroyed)", portalId);
        }
    }

    public static void removeAllDungeonPortals(MinecraftServer server) {
        suppressDungeonCleanup = true;
        try {
            List<ActivePortal> portalsToRemove = new ArrayList<>(ACTIVE_PORTALS.values());
            ACTIVE_PORTALS.clear();
            for (ActivePortal portal : portalsToRemove) {
                ServerLevel level = server.getLevel(portal.dimension());
                if (level != null) {
                    level.removeBlock(portal.pos(), false);
                }
            }
            PortalSavedData.get(server).clearAll();
            CobbleSafari.LOGGER.info("Removed all {} dungeon portals", portalsToRemove.size());
        } finally {
            suppressDungeonCleanup = false;
        }
    }

    public static void updatePortalChunkBounds(UUID portalId, String dungeonDimensionId,
                                                  BlockPos dungeonStructurePos,
                                                  int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ) {
        ActivePortal old = ACTIVE_PORTALS.get(portalId);
        if (old != null) {
            ACTIVE_PORTALS.put(portalId, new ActivePortal(
                    old.id(), old.pos(), old.dimension(), old.spawnTick(), old.dungeonId(),
                    dungeonDimensionId, dungeonStructurePos,
                    chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ, true
            ));
            savePortals();
        }
    }

    private static void clearDungeonForPortal(MinecraftServer server, ActivePortal portal) {
        if (portal.dungeonDimensionId() == null) return;

        DungeonConfig config = DungeonDimensions.getDungeonById(portal.dungeonDimensionId());
        if (config == null) return;

        if (config.isExternallyManaged()) {
            return;
        }

        if (!portal.hasDungeonChunkBounds()) return;

        ServerLevel dungeonLevel = server.getLevel(config.getDimensionKey());
        if (dungeonLevel != null) {
            evacuatePlayersFromDungeonByChunkBounds(dungeonLevel, config,
                    portal.dungeonChunkMinX(), portal.dungeonChunkMinZ(),
                    portal.dungeonChunkMaxX(), portal.dungeonChunkMaxZ());
            
            int structureY = portal.dungeonStructurePos() != null ? portal.dungeonStructurePos().getY() : 64;
            DungeonRegionClearer.clearRegion(dungeonLevel,
                    portal.dungeonChunkMinX(), portal.dungeonChunkMinZ(),
                    portal.dungeonChunkMaxX(), portal.dungeonChunkMaxZ(),
                    structureY, config.getClearSectionsBelow(), config.getClearSectionsAbove());
        }

        if (portal.dungeonStructurePos() != null) {
            DungeonPositionSavedData.get(server).removeUsedPosition(
                    config.getDimensionId(), portal.dungeonStructurePos(), config.getZoneSize());
        }
    }

    private static void evacuatePlayersFromDungeon(ServerLevel dungeonLevel, DungeonPortalBlockEntity portalEntity, DungeonConfig config) {
        if (!portalEntity.hasDungeonChunkBounds()) return;
        
        evacuatePlayersFromDungeonByChunkBounds(dungeonLevel, config,
                portalEntity.getDungeonChunkMinX(), portalEntity.getDungeonChunkMinZ(),
                portalEntity.getDungeonChunkMaxX(), portalEntity.getDungeonChunkMaxZ());
    }

    private static void evacuatePlayersFromDungeonByChunkBounds(ServerLevel dungeonLevel, DungeonConfig config,
                                                                  int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ) {
        int blockMinX = chunkMinX << 4;
        int blockMinZ = chunkMinZ << 4;
        int blockMaxX = (chunkMaxX << 4) + 15;
        int blockMaxZ = (chunkMaxZ << 4) + 15;
        
        net.minecraft.world.phys.AABB regionBounds = new net.minecraft.world.phys.AABB(
                blockMinX, dungeonLevel.getMinBuildHeight(), blockMinZ,
                blockMaxX, dungeonLevel.getMaxBuildHeight(), blockMaxZ
        );

        List<ServerPlayer> playersInRegion = new ArrayList<>();
        for (ServerPlayer player : dungeonLevel.players()) {
            if (regionBounds.contains(player.position())) {
                playersInRegion.add(player);
            }
        }

        if (!playersInRegion.isEmpty()) {
            CobbleSafari.LOGGER.warn("Evacuating {} player(s) from dungeon {} before cleanup: {}",
                    playersInRegion.size(), config.getId(),
                    playersInRegion.stream().map(p -> p.getName().getString()).toList());

            for (ServerPlayer player : playersInRegion) {
                maxigregrze.cobblesafari.manager.TimerManager.teleportOnTimerExpired(player, config.getDimensionId(),
                        maxigregrze.cobblesafari.manager.TimerManager.getOrCreateData(player, config.getDimensionId()));
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.evacuated"));
                CobbleSafari.LOGGER.info("Player {} evacuated from dungeon {} (portal expired)",
                        player.getName().getString(), config.getId());
            }
        }
    }

    public static ActivePortal getActivePortalById(UUID portalId) {
        return ACTIVE_PORTALS.get(portalId);
    }

    public static List<ActivePortal> getActivePortals() {
        return new ArrayList<>(ACTIVE_PORTALS.values());
    }

    public record ActivePortal(
            UUID id,
            BlockPos pos,
            net.minecraft.resources.ResourceKey<Level> dimension,
            long spawnTick,
            String dungeonId,
            String dungeonDimensionId,
            BlockPos dungeonStructurePos,
            int dungeonChunkMinX,
            int dungeonChunkMinZ,
            int dungeonChunkMaxX,
            int dungeonChunkMaxZ,
            boolean hasDungeonChunkBounds
    ) {
        public ActivePortal(UUID id, BlockPos pos, net.minecraft.resources.ResourceKey<Level> dimension,
                            long spawnTick, String dungeonId) {
            this(id, pos, dimension, spawnTick, dungeonId, null, null, 0, 0, 0, 0, false);
        }
    }
}
