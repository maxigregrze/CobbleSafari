package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.config.DimensionTimerEntry;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.manager.TimerManager;
import maxigregrze.cobblesafari.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonTeleportHandler {

    private static final Map<UUID, DungeonInstance> ACTIVE_INSTANCES = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerOrigin> PLAYER_ORIGINS = new ConcurrentHashMap<>();
    private static final Map<UUID, GenerationState> GENERATION_STATES = new ConcurrentHashMap<>();
    private static final Set<UUID> PORTALS_GENERATING = ConcurrentHashMap.newKeySet();
    public static final int ZONE_SIZE = 512;

    private DungeonTeleportHandler() {}

    public record DungeonValidationResult(
            DungeonConfig config,
            ServerLevel dungeonLevel,
            String dimensionId,
            boolean isReEntry,
            int timerTicks,
            BlockPos playerOriginPos,
            ResourceKey<Level> playerOriginDimension
    ) {}

    public record DungeonPrepResult(
            ServerLevel dungeonLevel,
            BlockPos playerSpawnPos,
            float playerYaw,
            String dimensionId,
            boolean isReEntry,
            int timerTicks,
            BlockPos playerOriginPos,
            ResourceKey<Level> playerOriginDimension
    ) {}

    public static DungeonValidationResult validateDungeonEntry(ServerPlayer player, DungeonPortalBlockEntity portalEntity) {
        MinecraftServer server = player.getServer();
        if (server == null) return null;

        DungeonConfig config = resolveDungeonConfig(player, portalEntity);
        if (config == null) {
            return null;
        }

        ServerLevel dungeonLevel = server.getLevel(config.getDimensionKey());
        if (dungeonLevel == null) {
            CobbleSafari.LOGGER.error("Dungeon dimension not found: {}", config.getDimensionId());
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.error.no_dimension"));
            return null;
        }

        String dimensionId = config.getDimensionId();
        boolean isReEntry = portalEntity.hasPlayerEntered(player.getUUID());
        PlayerTimerData timerData = TimerManager.getOrCreateData(player, dimensionId);

        int timerTicks;
        if (isReEntry) {
            Integer reEntryTicks = computeReEntryTimerTicks(player, portalEntity, config, timerData);
            if (reEntryTicks == null) {
                return null;
            }
            timerTicks = reEntryTicks;
        } else {
            timerTicks = computeNewEntryTimerTicks(portalEntity, dimensionId);
        }

        BlockPos playerOriginPos = player.blockPosition();
        ResourceKey<Level> playerOriginDimension = player.level().dimension();

        return new DungeonValidationResult(
                config, dungeonLevel, dimensionId,
                isReEntry, timerTicks, playerOriginPos, playerOriginDimension
        );
    }

    private static DungeonConfig resolveDungeonConfig(ServerPlayer player, DungeonPortalBlockEntity portalEntity) {
        DungeonConfig config = DungeonDimensions.getDungeonById(portalEntity.getDungeonDimensionId());
        if (config == null) {
            config = DungeonDimensions.getRandomDungeon();
        }
        if (config == null) {
            CobbleSafari.LOGGER.error("No dungeon configuration found");
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.error.no_config"));
        }
        return config;
    }

    /** Returns the timer ticks for a re-entry, or {@code null} if entry must be denied. */
    private static Integer computeReEntryTimerTicks(ServerPlayer player, DungeonPortalBlockEntity portalEntity,
                                                    DungeonConfig config, PlayerTimerData timerData) {
        if (timerData.getRemainingTicks() <= 0) {
            Optional<DungeonDimensionEntry> dimEntry = PortalSpawnConfig.getDimensionConfig(config.getId());
            boolean canPayAgain = dimEntry.map(DungeonDimensionEntry::isAllowMultiplePayment).orElse(false)
                    && dimEntry.map(DungeonDimensionEntry::isEntryFeeEnabled).orElse(false);
            if (!canPayAgain) {
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.error.no_time_remaining"));
                return null;
            }
            timerData.resetEntryFeePayDay();
        }

        long portalRemaining = portalEntity.getRemainingLifetimeTicks();
        if (portalRemaining >= 0 && timerData.getRemainingTicks() > portalRemaining) {
            CobbleSafari.LOGGER.info("Player {} re-entering dungeon: timer capped from {} to {} ticks (portal expiring)",
                    player.getName().getString(), timerData.getRemainingTicks(), (int) portalRemaining);
            return (int) portalRemaining;
        }
        return timerData.getRemainingTicks();
    }

    private static int computeNewEntryTimerTicks(DungeonPortalBlockEntity portalEntity, String dimensionId) {
        int configuredTicks = SafariTimerConfig.getDimensionConfig(dimensionId)
                .map(DimensionTimerEntry::getTimerDurationTicks)
                .orElse(SafariTimerConfig.getTimerDurationTicks());

        long portalRemaining = portalEntity.getRemainingLifetimeTicks();
        if (portalRemaining >= 0 && portalRemaining < configuredTicks) {
            return (int) portalRemaining;
        }
        return configuredTicks;
    }

    /** @deprecated superseded by the staged dungeon-preparation flow; retained for compatibility. */
    @Deprecated
    public static DungeonPrepResult generateAndPrepareDungeon(
            ServerPlayer player, DungeonPortalBlockEntity portalEntity, DungeonValidationResult validation) {
        
        if (!calculatePosition(player, portalEntity, validation)) return null;
        if (!loadChunks(player, portalEntity, validation)) return null;
        if (!generateStructure(player, portalEntity, validation)) return null;
        return finalizeGeneration(player, portalEntity, validation);
    }

    public static void executeDungeonTeleport(ServerPlayer player, DungeonPortalBlockEntity portalEntity, DungeonPrepResult prep) {
        PLAYER_ORIGINS.put(player.getUUID(), new PlayerOrigin(prep.playerOriginPos(), prep.playerOriginDimension()));

        DungeonInstance instance = ACTIVE_INSTANCES.get(portalEntity.getPortalId());
        if (instance != null) {
            instance.addPlayer(player.getUUID());
        }

        if (!prep.isReEntry()) {
            portalEntity.addEnteredPlayer(player.getUUID());
            int portalsUsed = maxigregrze.cobblesafari.init.ModStats.awardAndGet(
                    player, maxigregrze.cobblesafari.init.ModStats.HOOPA_PORTALS_EXPLORED);
            maxigregrze.cobblesafari.advancement.ModCriteria.PORTAL_USED.trigger(player, portalsUsed);
            PlayerTimerData timerData = TimerManager.getOrCreateData(player, prep.dimensionId());
            timerData.setRemainingTicks(prep.timerTicks());
            timerData.setLastResetTimestamp(System.currentTimeMillis());
        } else {
            PlayerTimerData timerData = TimerManager.getOrCreateData(player, prep.dimensionId());
            if (prep.timerTicks() < timerData.getRemainingTicks()) {
                timerData.setRemainingTicks(prep.timerTicks());
                TimerManager.savePlayerData(player, timerData);
            }
        }

        TimerManager.setPlayerOrigin(player, prep.dimensionId(), prep.playerOriginPos(), prep.playerOriginDimension());

        if (player.isRemoved()) {
            CobbleSafari.LOGGER.warn("Player {} is already removed ({}), aborting dungeon teleport",
                    player.getName().getString(), player.getRemovalReason());
            PLAYER_ORIGINS.remove(player.getUUID());
            return;
        }

        DimensionTransition transition = new DimensionTransition(
                prep.dungeonLevel(),
                new Vec3(prep.playerSpawnPos().getX() + 0.5, prep.playerSpawnPos().getY(), prep.playerSpawnPos().getZ() + 0.5),
                Vec3.ZERO,
                prep.playerYaw(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING
        );

        try {
            player.changeDimension(transition);
        } catch (NullPointerException e) {
            CobbleSafari.LOGGER.error("Failed to teleport player {} to dungeon due to chunk tracking inconsistency: {}",
                    player.getName().getString(), e.getMessage());
            PLAYER_ORIGINS.remove(player.getUUID());
            if (instance != null) {
                instance.removePlayer(player.getUUID());
            }
            return;
        }

        TimerManager.startTimer(player, prep.dimensionId());
        player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.teleport.entrance"));

        CobbleSafari.LOGGER.info("Player {} teleported to dungeon at {}",
                player.getName().getString(), prep.playerSpawnPos());
    }

    public static void teleportToExit(ServerPlayer player, DungeonPortalBlockEntity portalEntity) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        PlayerOrigin origin = PLAYER_ORIGINS.get(player.getUUID());
        BlockPos targetPos;
        ServerLevel targetLevel;
        boolean usedFailsafe = false;

        if (origin != null) {
            targetPos = origin.position();
            targetLevel = server.getLevel(origin.dimension());
            PLAYER_ORIGINS.remove(player.getUUID());
        } else if (portalEntity.getOriginPos() != null && portalEntity.getOriginDimension() != null) {
            targetPos = portalEntity.getOriginPos();
            targetLevel = server.getLevel(portalEntity.getOriginDimension());
        } else {
            PlayerTimerData timerData = findTimerDataWithOrigin(player, server);
            if (timerData != null && timerData.getOriginPos() != null && timerData.getOriginDimension() != null) {
                targetPos = timerData.getOriginPos();
                targetLevel = server.getLevel(timerData.getOriginDimension());
                CobbleSafari.LOGGER.warn("Using PlayerTimerData fallback for player {} exit teleport",
                        player.getName().getString());
            } else {
                targetLevel = server.overworld();
                targetPos = targetLevel.getSharedSpawnPos();
                usedFailsafe = true;
                CobbleSafari.LOGGER.warn("No origin data found for player {}, teleporting to spawn (failsafe)",
                        player.getName().getString());
            }
        }

        if (targetLevel == null) {
            targetLevel = server.overworld();
            targetPos = targetLevel.getSharedSpawnPos();
            usedFailsafe = true;
            CobbleSafari.LOGGER.warn("Target level was null for player {}, teleporting to spawn (failsafe)",
                    player.getName().getString());
        }

        DimensionTransition transition = new DimensionTransition(
                targetLevel,
                new Vec3(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING
        );

        player.changeDimension(transition);

        if (usedFailsafe) {
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.teleport.exit.failsafe"));
        } else {
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.teleport.exit"));
        }

        CobbleSafari.LOGGER.info("Player {} teleported back from dungeon to {}{}",
                player.getName().getString(), targetPos, usedFailsafe ? " (failsafe)" : "");
    }

    private static PlayerTimerData findTimerDataWithOrigin(ServerPlayer player, @SuppressWarnings("unused") MinecraftServer server) {
        for (DungeonConfig dungeon : DungeonDimensions.getAllDungeons()) {
            String dimId = dungeon.getDimensionId();
            PlayerTimerData data = TimerManager.getOrCreateData(player, dimId);
            if (data.getOriginPos() != null && data.getOriginDimension() != null) {
                return data;
            }
        }
        return null;
    }

    private static BlockPos calculateUniquePosition(MinecraftServer server, DungeonConfig config) {
        String dimensionId = config.getDimensionId();
        DungeonPositionSavedData positionData = DungeonPositionSavedData.get(server);
        int zoneSize = config.getZoneSize();

        int slot = positionData.allocateSlot(dimensionId);
        int zOffset = positionData.getAndIncrementSlotZOffset(dimensionId, slot);
        int x = (slot % 100) * zoneSize;
        int z = (slot / 100) * zoneSize + zOffset;
        int y = (slot % 2 == 0) ? 96 : 32;
        BlockPos pos = new BlockPos(x, y, z);

        CobbleSafari.LOGGER.info("Allocated dungeon slot {} at {} (Z offset {}) for dimension {}", slot, pos, zOffset, dimensionId);
        return pos;
    }

    private static void placeExitPortal(ServerLevel level, BlockPos pos, BlockPos originPos, ResourceKey<Level> originDimension, Direction facing) {
        BlockState portalState = ModBlocks.DUNGEON_PORTAL.defaultBlockState()
                .setValue(DungeonPortalBlock.PORTAL_TYPE, DungeonPortalBlock.PortalType.EXIT)
                .setValue(DungeonPortalBlock.FACING, facing);

        level.setBlock(pos, portalState, 3);

        if (level.getBlockEntity(pos) instanceof DungeonPortalBlockEntity portalEntity) {
            portalEntity.setOriginPos(originPos);
            portalEntity.setOriginDimension(originDimension);
        }
    }

    private static BlockPos findPortalPosition(ServerLevel level, BlockPos anchorPos) {
        int scanRadius = 5;
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dy = -2; dy <= 5; dy++) {
                for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                    BlockPos candidate = anchorPos.offset(dx, dy, dz);
                    if (level.getBlockState(candidate).is(ModBlocks.HOOPA_RING_PORTAL)) {
                        CobbleSafari.LOGGER.info("Found hoopa_ring_portal placeholder at {} (offset {} {} {} from anchor)",
                                candidate, dx, dy, dz);
                        return candidate;
                    }
                }
            }
        }

        CobbleSafari.LOGGER.warn("Could not find hoopa_ring_portal placeholder near anchor {}, using default offset", anchorPos);
        return anchorPos.offset(0, 2, -1);
    }

    private static Direction getPortalFacing(BlockPos anchorPos, BlockPos portalPos) {
        int dx = portalPos.getX() - anchorPos.getX();
        int dz = portalPos.getZ() - anchorPos.getZ();

        if (dz < 0) return Direction.NORTH;
        if (dz > 0) return Direction.SOUTH;
        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        return Direction.NORTH;
    }

    private static float getPlayerYawFromFacing(Direction portalFacing) {
        return switch (portalFacing) {
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            case EAST -> -90.0f;
            case WEST -> 90.0f;
            default -> 180.0f;
        };
    }

    public static void clearPlayerData(UUID playerId) {
        PLAYER_ORIGINS.remove(playerId);
        for (DungeonInstance instance : ACTIVE_INSTANCES.values()) {
            instance.removePlayer(playerId);
        }
    }

    public static void removeInstance(UUID portalId) {
        ACTIVE_INSTANCES.remove(portalId);
    }

    public static void clearGenerationStates() {
        GENERATION_STATES.clear();
        PORTALS_GENERATING.clear();
    }

    public static Map<UUID, DungeonInstance> getActiveInstances() {
        return ACTIVE_INSTANCES;
    }

    private record PlayerOrigin(BlockPos position, ResourceKey<Level> dimension) {}

    private static class GenerationState {
        BlockPos structurePos;
        BlockPos exitPortalPos;
        boolean isFirstPortalUse;
        DungeonConfig config;
        ServerLevel dungeonLevel;
        /** Real footprint of the placed structure (V3); null falls back to the zoneSize radius. */
        BoundingBox structureBounds;

        GenerationState(DungeonConfig config, ServerLevel dungeonLevel, boolean isFirstPortalUse) {
            this.config = config;
            this.dungeonLevel = dungeonLevel;
            this.isFirstPortalUse = isFirstPortalUse;
        }
    }

    public static boolean calculatePosition(ServerPlayer player, DungeonPortalBlockEntity portalEntity, DungeonValidationResult validation) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        UUID portalId = portalEntity.getPortalId();
        DungeonConfig config = validation.config();
        ServerLevel dungeonLevel = validation.dungeonLevel();
        boolean isFirstPortalUse = portalEntity.getDungeonStructurePos() == null;

        if (isFirstPortalUse && !PORTALS_GENERATING.add(portalId)) {
            CobbleSafari.LOGGER.info("Portal {} already has generation in progress, player {} will wait",
                    portalId, player.getName().getString());
            return false;
        }

        GenerationState state = new GenerationState(config, dungeonLevel, isFirstPortalUse);

        if (!isFirstPortalUse) {
            state.structurePos = portalEntity.getDungeonStructurePos();
            state.exitPortalPos = portalEntity.getDungeonExitPortalPos();
            CobbleSafari.LOGGER.info("Reusing existing dungeon for portal {}: structure at {}, exit portal at {}",
                    portalId, state.structurePos, state.exitPortalPos);
        } else {
            state.structurePos = calculateUniquePosition(server, config);
            CobbleSafari.LOGGER.info("Calculated new dungeon position at {}", state.structurePos);
        }

        GENERATION_STATES.put(portalId, state);
        return true;
    }

    /**
     * Returns true if a dungeon generation is currently in progress for the given portal.
     */
    public static boolean isGenerationInProgress(UUID portalId) {
        return PORTALS_GENERATING.contains(portalId);
    }

    /**
     * Releases the per-portal generation lock and aborts any in-flight generation state for that
     * portal. Idempotent and safe to call when the lock is not held. Must be invoked whenever the
     * player who acquired the lock leaves the generation sequence without reaching
     * {@link #completeFinalization} (moved, disconnected, portal removed, etc.); otherwise the lock
     * leaks and the portal becomes permanently unusable until a server restart.
     */
    public static void releaseGenerationLock(UUID portalId) {
        if (portalId == null) {
            return;
        }
        PORTALS_GENERATING.remove(portalId);
        GENERATION_STATES.remove(portalId);
    }

    public static boolean loadChunks(ServerPlayer player, DungeonPortalBlockEntity portalEntity, DungeonValidationResult validation) {
        GenerationState state = GENERATION_STATES.get(portalEntity.getPortalId());
        if (state == null) {
            CobbleSafari.LOGGER.error("Generation state not found for portal {}", portalEntity.getPortalId());
            return false;
        }
        if (!state.isFirstPortalUse) return true;

        ChunkPos chunkPos = new ChunkPos(state.structurePos);
        state.dungeonLevel.getChunkSource().addRegionTicket(
                TicketType.PORTAL, chunkPos, 4, state.structurePos);

        CobbleSafari.LOGGER.info("Loaded chunks for dungeon at {}", state.structurePos);
        return true;
    }

    public static boolean generateStructure(ServerPlayer player, DungeonPortalBlockEntity portalEntity, DungeonValidationResult validation) {
        GenerationState state = GENERATION_STATES.get(portalEntity.getPortalId());
        if (state == null) {
            CobbleSafari.LOGGER.error("Generation state not found for portal {}", portalEntity.getPortalId());
            return false;
        }
        if (!state.isFirstPortalUse) return true;

        boolean placed;
        if (state.config.isJigsaw()) {
            placed = StructurePlacer.placeJigsawStructure(state.dungeonLevel, state.structurePos,
                    state.config.getStructureId(), state.config.getJigsawDepth());
            if (placed) {
                // V3: real assembled extent, captured by JigsawPlacementMixin during /place jigsaw.
                state.structureBounds = DungeonStructureBounds.consume(state.structurePos);
            }
        } else {
            placed = StructurePlacer.placeStructure(state.dungeonLevel, state.structurePos,
                    state.config.getStructureId());
            if (placed) {
                // V3: template placed with no rotation/mirror -> exact extent [pos, pos+size).
                BlockPos size = StructurePlacer.getStructureSize(state.dungeonLevel, state.config.getStructureId());
                if (size != null && !size.equals(BlockPos.ZERO)) {
                    state.structureBounds = new BoundingBox(
                            state.structurePos.getX(), state.structurePos.getY(), state.structurePos.getZ(),
                            state.structurePos.getX() + size.getX() - 1,
                            state.structurePos.getY() + size.getY() - 1,
                            state.structurePos.getZ() + size.getZ() - 1);
                }
            }
        }

        if (!placed) {
            CobbleSafari.LOGGER.error("Failed to place dungeon structure");
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.error.structure_failed"));
            GENERATION_STATES.remove(portalEntity.getPortalId());
            PORTALS_GENERATING.remove(portalEntity.getPortalId());
            return false;
        }

        CobbleSafari.LOGGER.info("Generated structure for dungeon at {}", state.structurePos);
        return true;
    }

    public static boolean startFinalization(ServerPlayer player, DungeonPortalBlockEntity portalEntity, @SuppressWarnings("unused") DungeonValidationResult validation) {
        GenerationState state = GENERATION_STATES.get(portalEntity.getPortalId());
        if (state == null) return false;

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        if (state.isFirstPortalUse) {
            if (state.config.isJigsaw()) {
                state.exitPortalPos = findPortalPosition(state.dungeonLevel, state.structurePos);
                Direction portalFacing = getPortalFacing(state.structurePos, state.exitPortalPos);
                placeExitPortal(state.dungeonLevel, state.exitPortalPos, portalEntity.getBlockPos(), 
                        player.level().dimension(), portalFacing);
            } else {
                state.exitPortalPos = state.structurePos.offset(state.config.getPlayerSpawnOffsetX(), 
                        state.config.getPlayerSpawnOffsetY(), state.config.getPlayerSpawnOffsetZ());
                placeExitPortal(state.dungeonLevel, state.exitPortalPos, portalEntity.getBlockPos(), 
                        player.level().dimension(), Direction.NORTH);
            }

            CobbleSafari.LOGGER.debug("Exit portal placed at {} for portal {}", state.exitPortalPos, portalEntity.getPortalId());
        }

        return true;
    }

    public static DungeonPrepResult completeFinalization(ServerPlayer player, DungeonPortalBlockEntity portalEntity, DungeonValidationResult validation) {
        GenerationState state = GENERATION_STATES.get(portalEntity.getPortalId());
        if (state == null) return null;

        MinecraftServer server = player.getServer();
        if (server == null) return null;

        if (state.exitPortalPos == null) {
            CobbleSafari.LOGGER.warn("Exit portal position is null for portal {} (structure at {}), recalculating...",
                    portalEntity.getPortalId(), state.structurePos);

            if (state.config.isJigsaw()) {
                state.exitPortalPos = findPortalPosition(state.dungeonLevel, state.structurePos);
                Direction portalFacing = getPortalFacing(state.structurePos, state.exitPortalPos);
                placeExitPortal(state.dungeonLevel, state.exitPortalPos, portalEntity.getBlockPos(),
                        player.level().dimension(), portalFacing);
            } else {
                state.exitPortalPos = state.structurePos.offset(
                        state.config.getPlayerSpawnOffsetX(),
                        state.config.getPlayerSpawnOffsetY(),
                        state.config.getPlayerSpawnOffsetZ());
                placeExitPortal(state.dungeonLevel, state.exitPortalPos, portalEntity.getBlockPos(),
                        player.level().dimension(), Direction.NORTH);
            }

            portalEntity.setDungeonExitPortalPos(state.exitPortalPos);
            portalEntity.setChanged();

            CobbleSafari.LOGGER.info("Recalculated exit portal position for portal {}: {}", 
                    portalEntity.getPortalId(), state.exitPortalPos);
        }

        if (state.isFirstPortalUse) {
            portalEntity.setDungeonStructurePos(state.structurePos);
            portalEntity.setDungeonExitPortalPos(state.exitPortalPos);

            int chunkMinX;
            int chunkMinZ;
            int chunkMaxX;
            int chunkMaxZ;
            BoundingBox bounds = state.structureBounds;
            if (bounds != null) {
                // V3: clear exactly the structure's footprint (+1 chunk margin). This both avoids
                // generating ~1024 empty chunks for a small dungeon AND covers a large jigsaw that can
                // sprawl beyond the old ±255 window (maxGenDistance is raised to 512 by JigsawPlacementMixin).
                chunkMinX = (bounds.minX() >> 4) - 1;
                chunkMinZ = (bounds.minZ() >> 4) - 1;
                chunkMaxX = (bounds.maxX() >> 4) + 1;
                chunkMaxZ = (bounds.maxZ() >> 4) + 1;
            } else {
                // Fallback (extent unavailable: template size missing, jigsaw capture skipped, legacy record).
                int regionRadius = state.config.getZoneSize() / 2 - 1;
                chunkMinX = (state.structurePos.getX() - regionRadius) >> 4;
                chunkMinZ = (state.structurePos.getZ() - regionRadius) >> 4;
                chunkMaxX = (state.structurePos.getX() + regionRadius) >> 4;
                chunkMaxZ = (state.structurePos.getZ() + regionRadius) >> 4;
            }
            portalEntity.setDungeonChunkBounds(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ);

            PortalSpawnManager.updatePortalChunkBounds(
                    portalEntity.getPortalId(), state.config.getId(), state.structurePos,
                    chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ);

            portalEntity.setChanged();

            DungeonInstance instance = new DungeonInstance(
                    state.config.getId(), state.dungeonLevel.dimension(), state.structurePos, state.exitPortalPos);
            ACTIVE_INSTANCES.put(portalEntity.getPortalId(), instance);

            CobbleSafari.LOGGER.info("Finalized dungeon for portal {}: structure at {}, exit portal at {}",
                    portalEntity.getPortalId(), state.structurePos, state.exitPortalPos);

            DungeonInstanceCleanup.registerActiveInstance(
                    server,
                    portalEntity.getPortalId(),
                    state.config,
                    state.structurePos,
                    portalEntity.getDungeonChunkMinX(),
                    portalEntity.getDungeonChunkMinZ(),
                    portalEntity.getDungeonChunkMaxX(),
                    portalEntity.getDungeonChunkMaxZ()
            );
        }

        BlockPos playerSpawnPos;
        float playerYaw;
        if (state.config.isJigsaw()) {
            Direction portalDir = getPortalFacing(state.structurePos, state.exitPortalPos);
            playerSpawnPos = state.exitPortalPos.relative(portalDir.getOpposite());
            playerYaw = getPlayerYawFromFacing(portalDir);
        } else {
            playerSpawnPos = state.exitPortalPos.south();
            playerYaw = 180.0f;
        }

        GENERATION_STATES.remove(portalEntity.getPortalId());
        PORTALS_GENERATING.remove(portalEntity.getPortalId());

        return new DungeonPrepResult(
                state.dungeonLevel, playerSpawnPos, playerYaw, validation.dimensionId(),
                validation.isReEntry(), validation.timerTicks(),
                validation.playerOriginPos(), validation.playerOriginDimension()
        );
    }

    public static DungeonPrepResult finalizeGeneration(ServerPlayer player, DungeonPortalBlockEntity portalEntity, DungeonValidationResult validation) {
        if (!startFinalization(player, portalEntity, validation)) {
            return null;
        }
        return completeFinalization(player, portalEntity, validation);
    }

    public static DungeonPrepResult buildPrepResultFromPortal(ServerPlayer player, DungeonPortalBlockEntity portalEntity, DungeonValidationResult validation) {
        MinecraftServer server = player.getServer();
        if (server == null) return null;

        BlockPos structurePos = portalEntity.getDungeonStructurePos();
        BlockPos exitPortalPos = portalEntity.getDungeonExitPortalPos();
        if (structurePos == null || exitPortalPos == null) return null;

        ServerLevel dungeonLevel = server.getLevel(validation.config().getDimensionKey());
        if (dungeonLevel == null) return null;

        BlockPos playerSpawnPos;
        float playerYaw;
        if (validation.config().isJigsaw()) {
            Direction portalDir = getPortalFacing(structurePos, exitPortalPos);
            playerSpawnPos = exitPortalPos.relative(portalDir.getOpposite());
            playerYaw = getPlayerYawFromFacing(portalDir);
        } else {
            playerSpawnPos = exitPortalPos.south();
            playerYaw = 180.0f;
        }

        return new DungeonPrepResult(
                dungeonLevel, playerSpawnPos, playerYaw, validation.dimensionId(),
                validation.isReEntry(), validation.timerTicks(),
                validation.playerOriginPos(), validation.playerOriginDimension()
        );
    }
}
