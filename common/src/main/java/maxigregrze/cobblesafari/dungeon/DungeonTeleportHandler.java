package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.config.DimensionTimerEntry;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.DungeonInstanceRecord;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.data.TimerScope;
import maxigregrze.cobblesafari.manager.TimerManager;
import maxigregrze.cobblesafari.network.ModNetworking;
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
    private static final Map<UUID, GenerationState> GENERATION_STATES = new ConcurrentHashMap<>();
    private static final Set<UUID> PORTALS_GENERATING = ConcurrentHashMap.newKeySet();
    public static final int ZONE_SIZE = 512;

    private DungeonTeleportHandler() {}

    /**
     * @param instanceId the instance this portal admits to, or {@code null} on a first entry where
     *                   the structure does not exist yet. Resolved by portal, never by position:
     *                   at admission time the player is still in the overworld (plan 181 § 3.5).
     */
    public record DungeonValidationResult(
            DungeonConfig config,
            ServerLevel dungeonLevel,
            String dimensionId,
            boolean isReEntry,
            int timerTicks,
            BlockPos playerOriginPos,
            ResourceKey<Level> playerOriginDimension,
            UUID instanceId
    ) {}

    /** @param instanceId the instance the player is about to enter; the session's scope id. */
    public record DungeonPrepResult(
            ServerLevel dungeonLevel,
            BlockPos playerSpawnPos,
            float playerYaw,
            String dimensionId,
            boolean isReEntry,
            int timerTicks,
            BlockPos playerOriginPos,
            ResourceKey<Level> playerOriginDimension,
            UUID instanceId
    ) {}

    /**
     * The "by portal" scope resolver (plan 181 § 3.5): which live instance does this portal admit to?
     * Returns {@code null} when the structure has not been generated yet (a first entry), or when the
     * instance is already scheduled for deletion - a door onto a region that is being erased.
     */
    public static UUID resolveInstanceForPortal(MinecraftServer server, DungeonPortalBlockEntity portalEntity,
                                                String dimensionId) {
        if (server == null || portalEntity == null) {
            return null;
        }
        DungeonInstanceRecord record = DungeonPositionSavedData.get(server).findInstance(
                portalEntity.getPortalId(), portalEntity.getDungeonStructurePos(), dimensionId);
        if (record == null || record.pendingDeletion()) {
            return null;
        }
        return record.instanceId();
    }

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
        UUID instanceId = resolveInstanceForPortal(server, portalEntity, dimensionId);

        // The session for the instance this portal admits to, if the player already holds one.
        // Absent on a first entry, when no instance exists yet.
        PlayerTimerData session = instanceId == null
                ? null
                : TimerManager.session(player, TimerScope.of(dimensionId, instanceId));

        int timerTicks;
        if (isReEntry && session != null) {
            Integer reEntryTicks = computeReEntryTimerTicks(player, portalEntity, config, session);
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
                isReEntry, timerTicks, playerOriginPos, playerOriginDimension, instanceId
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

    /**
     * Returns the timer ticks for a re-entry, or {@code null} if entry must be denied.
     * <p>
     * Pure calculation since plan 181 (V4): it used to call {@code resetEntryFeePayDay()} - a
     * mutation, from a method invoked up to three times per interaction including when the player
     * <em>declines</em>. Deciding to charge now belongs to the caller, the only one that knows
     * whether the player accepted.
     */
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
            // A re-purchase is a first entry (plan 181 § 6.2): same grant, same clamp, same stamp.
            // The branch used to return getRemainingTicks() here - that is, 0 - so the player was
            // charged and then evicted on the next tick.
            return computeNewEntryTimerTicks(portalEntity, config.getDimensionId());
        }

        // No clamp to the portal's remaining life since plan 182 (A4). That clamp was fix 2 of plan
        // 43, and it existed because the instance died with the portal - which it no longer does.
        return timerData.getRemainingTicks();
    }

    private static int computeNewEntryTimerTicks(DungeonPortalBlockEntity portalEntity, String dimensionId) {
        int configuredTicks = SafariTimerConfig.getDimensionConfig(dimensionId)
                .map(DimensionTimerEntry::getTimerDurationTicks)
                .orElse(SafariTimerConfig.getTimerDurationTicks());

        // The whole point of Feature A: a player entering at minute 29 of a 30-minute window gets
        // the full configured stay, and plays it out after the entrance closes (plan 182, A4).
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

    /** @return {@code true} only if the player is actually through; {@code false} means the
     *          entry fee is still owed back (plan 181, V8). */
    public static boolean executeDungeonTeleport(ServerPlayer player, DungeonPortalBlockEntity portalEntity, DungeonPrepResult prep) {
        // Close the (uncloseable) TpAccept loading screen before doing anything else: once we are in
        // here the teleport is committed, so no branch below should leave it open. Do not rely on the
        // vanilla ReceivingLevelScreen replacing it during changeDimension -- client mods that skip the
        // world loading screen cancel Minecraft.setScreen entirely and would soft-lock the player.
        ModNetworking.sendCloseTpAccept(player);

        // The scope must be known before the door opens; nothing is written until it is crossed.
        if (prep.instanceId() == null) {
            CobbleSafari.LOGGER.error("Aborting dungeon teleport for {}: no instance resolved for portal {}",
                    player.getName().getString(), portalEntity.getPortalId());
            return false;
        }
        // D2 (plan 182 section 5.4): the instance id was resolved in completeFinalization, up to 100
        // ticks ago, and PortalSpawnManager.tick runs earlier in this very tick than the countdown.
        // An entrance that closed in between may already have released it, so re-check before
        // admitting anyone. Abandoning here refunds the fee through plan 181 V8.
        MinecraftServer teleportServer = player.getServer();
        if (teleportServer == null
                || !DungeonPositionSavedData.get(teleportServer).isInstanceLive(prep.instanceId())) {
            CobbleSafari.LOGGER.warn("Aborting dungeon teleport for {}: instance {} is no longer live",
                    player.getName().getString(), prep.instanceId());
            ModNetworking.sendCloseTpAccept(player);
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.error.structure_failed"));
            return false;
        }

        TimerScope scope = TimerScope.of(prep.dimensionId(), prep.instanceId());

        DungeonInstance instance = ACTIVE_INSTANCES.get(portalEntity.getPortalId());
        if (instance != null) {
            instance.addPlayer(player.getUUID());
        }

        if (player.isRemoved()) {
            CobbleSafari.LOGGER.warn("Player {} is already removed ({}), aborting dungeon teleport",
                    player.getName().getString(), player.getRemovalReason());
            if (instance != null) {
                instance.removePlayer(player.getUUID());
            }
            return false;
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
            if (instance != null) {
                instance.removePlayer(player.getUUID());
            }
            return false;
        }

        // The receipt is issued at the door, not at the till (plan 181 § 4.2). Everything above can
        // still abort; from here the player is demonstrably inside, so "a session exists" is
        // equivalent to "this player was admitted" - the equivalence the presence guard rests on.
        if (!prep.isReEntry()) {
            portalEntity.addEnteredPlayer(player.getUUID());
            int portalsUsed = maxigregrze.cobblesafari.init.ModStats.awardAndGet(
                    player, maxigregrze.cobblesafari.init.ModStats.HOOPA_PORTALS_EXPLORED);
            maxigregrze.cobblesafari.advancement.ModCriteria.PORTAL_USED.trigger(player, portalsUsed);
            TimerManager.openSession(player, scope, prep.timerTicks(),
                    prep.playerOriginPos(), prep.playerOriginDimension());
        } else {
            PlayerTimerData existing = TimerManager.session(player, scope);
            if (existing == null) {
                // Re-entry into an instance whose session is gone (purged mid-flight): treat it as
                // the first entry it effectively is, rather than admitting an unmetered player.
                TimerManager.openSession(player, scope, prep.timerTicks(),
                        prep.playerOriginPos(), prep.playerOriginDimension());
            } else {
                // Assign, do not clamp. computeReEntryTimerTicks has already produced the correct
                // figure for both cases: min(remaining, portal life) for a normal re-entry, and a
                // full fresh grant for a re-purchase (§ 6.2). A "only lower it" guard would leave a
                // re-purchase at zero - which is exactly the half of S2 this plan closes.
                existing.setRemainingTicks(prep.timerTicks());
                existing.setLastResetTimestamp(System.currentTimeMillis());
                existing.setOriginPos(prep.playerOriginPos());
                existing.setOriginDimension(prep.playerOriginDimension());
                TimerManager.savePlayerData(player, existing);
            }
        }

        TimerManager.startSession(player, scope);
        player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.teleport.entrance"));

        CobbleSafari.LOGGER.info("Player {} teleported to dungeon at {} (session {})",
                player.getName().getString(), prep.playerSpawnPos(), scope.key());
        return true;
    }

    public static void teleportToExit(ServerPlayer player, DungeonPortalBlockEntity portalEntity) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        // One source of origin, in one order (plan 181 § 8): the session that recorded it, then the
        // exit portal's own record, then spawn. There is no in-memory map to disagree with the
        // persisted data any more, and no "first dungeon with any origin" fallback to pick the wrong
        // one (finding S3).
        PlayerTimerData session = TimerManager.currentSession(player);
        BlockPos targetPos;
        ServerLevel targetLevel;
        boolean usedFailsafe = false;

        if (session != null && session.getOriginPos() != null && session.getOriginDimension() != null) {
            targetPos = session.getOriginPos();
            targetLevel = server.getLevel(session.getOriginDimension());
        } else if (portalEntity.getOriginPos() != null && portalEntity.getOriginDimension() != null) {
            targetPos = portalEntity.getOriginPos();
            targetLevel = server.getLevel(portalEntity.getOriginDimension());
            CobbleSafari.LOGGER.warn("Using exit portal origin fallback for player {} exit teleport",
                    player.getName().getString());
        } else {
            targetLevel = server.overworld();
            targetPos = targetLevel.getSharedSpawnPos();
            usedFailsafe = true;
            CobbleSafari.LOGGER.warn("No origin data found for player {}, teleporting to spawn (failsafe)",
                    player.getName().getString());
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

        // Leaving is final once the entrance is gone: there is no door to come back through, so the
        // credit is spent rather than left dangling in the HUD (plan 182, A5). Read before the
        // teleport, while the session is still resolvable from where the player stands.
        boolean entranceClosed = isInsideClosedInstance(server, player);

        if (player.isRemoved()) {
            // changeDimension would return without doing anything (plan 190, J5).
            TimerManager.disconnectDesynced(player, "dungeon exit portal");
            return;
        }
        player.changeDimension(transition);

        // Nothing is settled before the player is demonstrably out (plan 190, J3). The credit used
        // to be zeroed first, so a teleport that silently failed left them inside with no time -
        // and the expiry that followed could not move them either.
        if (player.level() != targetLevel) {
            CobbleSafari.LOGGER.error("Exit teleport failed for {}: still in {} (removed={}, reason={}); credit kept",
                    player.getName().getString(), player.level().dimension().location(),
                    player.isRemoved(), player.getRemovalReason());
            if (player.isRemoved()) {
                TimerManager.disconnectDesynced(player, "dungeon exit portal");
            } else {
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.teleport.exit.failed"));
            }
            return;
        }

        if (entranceClosed) {
            if (session != null) {
                // The credit is spent only now that the exit has worked. changeDimension already
                // paused and synced the session through ServerPlayerMixin; this second, explicit
                // active=false packet is the one the HUD hides on, and the last chance to send one
                // - the instance is about to be released and the session purged with it.
                session.setRemainingTicks(0);
                session.setActive(false);
                TimerManager.savePlayerData(player, session);
                TimerManager.syncToClient(player, session, false);
            }
            player.displayClientMessage(
                    Component.translatable("cobblesafari.dungeon.exit.final"), true);
        }

        if (usedFailsafe) {
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.teleport.exit.failsafe"));
        } else {
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.teleport.exit"));
        }

        CobbleSafari.LOGGER.info("Player {} teleported back from dungeon to {}{}",
                player.getName().getString(), targetPos, usedFailsafe ? " (failsafe)" : "");
    }

    /** Whether the player stands in an instance whose entrance has already closed (plan 182, A5). */
    private static boolean isInsideClosedInstance(MinecraftServer server, ServerPlayer player) {
        TimerScope scope = TimerManager.currentScope(player);
        if (scope == null || !scope.isSession()) {
            return false;
        }
        try {
            DungeonInstanceRecord record = DungeonPositionSavedData.get(server)
                    .getInstance(UUID.fromString(scope.scopeId()));
            return record != null && record.entranceClosed();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Slot allocated for a new dungeon, together with the anchor position derived from it. */
    private record SlotAllocation(int slot, BlockPos pos) {}

    private static SlotAllocation calculateUniquePosition(MinecraftServer server, DungeonConfig config) {
        String dimensionId = config.getDimensionId();
        DungeonPositionSavedData positionData = DungeonPositionSavedData.get(server);
        int zoneSize = config.getZoneSize();

        int slot = positionData.allocateSlot(dimensionId);
        int zOffset = positionData.getAndIncrementSlotZOffset(dimensionId, slot, zoneSize);
        int x = (slot % 100) * zoneSize;
        int z = (slot / 100) * zoneSize + zOffset;
        // A dungeon whose dimension has a floor at a fixed altitude must always anchor at the same Y,
        // otherwise the cleanup window can no longer be aligned with that floor (action plan 175, § 2).
        int y = config.hasFixedStructureY()
                ? config.getFixedStructureY()
                : ((slot % 2 == 0) ? 96 : 32);
        BlockPos pos = new BlockPos(x, y, z);

        CobbleSafari.LOGGER.info("Allocated dungeon slot {} at {} (Z offset {}) for dimension {}", slot, pos, zOffset, dimensionId);
        return new SlotAllocation(slot, pos);
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

    /**
     * Detaches a player from the in-memory instances. It used to also drop their origin, which is
     * how finding S11 arose: the call is dimension-agnostic, so unwinding one dungeon wiped the
     * origin of another. The origin now lives on the session and nothing here can touch it.
     */
    public static void clearPlayerData(UUID playerId) {
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

    private static class GenerationState {
        BlockPos structurePos;
        BlockPos exitPortalPos;
        /**
         * Slot handed out by {@code allocateSlot} on first use, or
         * {@link DungeonInstanceCleanup#UNKNOWN_SLOT} on re-entry; carrying it avoids re-deriving it.
         */
        int slot = DungeonInstanceCleanup.UNKNOWN_SLOT;
        boolean isFirstPortalUse;
        DungeonConfig config;
        ServerLevel dungeonLevel;
        /** Real footprint of the placed structure (V3); null falls back to the zoneSize radius. */
        BoundingBox structureBounds;
        /** Wall-clock start of this generation; used by the stale-lock watchdog (C11). */
        final long startedAtMs = System.currentTimeMillis();

        GenerationState(DungeonConfig config, ServerLevel dungeonLevel, boolean isFirstPortalUse) {
            this.config = config;
            this.dungeonLevel = dungeonLevel;
            this.isFirstPortalUse = isFirstPortalUse;
        }
    }

    /** Max time a generation may hold its portal lock before the watchdog force-releases it (C11). */
    private static final long MAX_GENERATION_MS = 30_000L;

    /**
     * Watchdog for the {@link #PORTALS_GENERATING} lock: the multi-tick generation releases the lock on its
     * explicit success/failure branches, but an unexpected exception between acquiring and releasing would
     * otherwise strand the lock until a server restart, making the portal permanently unusable. This sweep
     * force-releases any generation that has been in progress too long. See action plan 145 (C11).
     */
    public static void sweepStaleGenerations() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, GenerationState> e : GENERATION_STATES.entrySet()) {
            if (now - e.getValue().startedAtMs > MAX_GENERATION_MS) {
                CobbleSafari.LOGGER.warn("Dungeon generation for portal {} exceeded {} ms - force-releasing lock",
                        e.getKey(), MAX_GENERATION_MS);
                releaseGenerationLock(e.getKey());
            }
        }
        // A lock without a matching generation state (state cleared but lock left behind) is also released.
        for (UUID portalId : PORTALS_GENERATING) {
            if (!GENERATION_STATES.containsKey(portalId)) {
                PORTALS_GENERATING.remove(portalId);
            }
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
            SlotAllocation allocation = calculateUniquePosition(server, config);
            state.slot = allocation.slot();
            state.structurePos = allocation.pos();
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
        // Radius matched to what the placement actually touches (action plan 187, C5). It used to
        // reserve 4 while StructurePlacer blocked over 8, so the ring from 5 to 8 was generated
        // synchronously on the server thread - invisible while every dungeon dimension was an empty
        // void, and a stall of several hundred chunk generations as soon as one has worldgen.
        state.dungeonLevel.getChunkSource().addRegionTicket(
                TicketType.PORTAL, chunkPos, StructurePlacer.STRUCTURE_PLACEMENT_RADIUS, state.structurePos);
        StructurePlacer.ticketPlacementChunks(
                state.dungeonLevel, state.structurePos, StructurePlacer.STRUCTURE_PLACEMENT_RADIUS);

        CobbleSafari.LOGGER.info("Loaded chunks for dungeon at {}", state.structurePos);
        return true;
    }

    /**
     * Whether the chunks reserved by {@link #loadChunks} have finished loading (action plan 187, C5).
     *
     * <p>The generation sequence must wait on this before {@link #generateStructure}, so that the
     * blocking {@code getChunk} calls inside the placement are all cache hits. Returns true for a
     * repeat use of an already-generated dungeon, which places nothing.</p>
     */
    public static boolean areChunksReady(DungeonPortalBlockEntity portalEntity) {
        GenerationState state = GENERATION_STATES.get(portalEntity.getPortalId());
        if (state == null || !state.isFirstPortalUse) {
            return true;
        }
        return StructurePlacer.areChunksReady(
                state.dungeonLevel, state.structurePos, StructurePlacer.STRUCTURE_PLACEMENT_RADIUS);
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

    /**
     * Action plan 196, § 3.5: the instance record exists now, and its chunks are still loaded. Also
     * starts the sky map (action plan 197, § 5.1) while the player still stands at the Hoopa portal,
     * in {@code portalLevel}, where the chunks around it are loaded too.
     */
    private static void resolveHyperspaceZone(MinecraftServer server, GenerationState state,
                                              DungeonPortalBlockEntity portalEntity, ServerLevel portalLevel) {
        try {
            DungeonInstanceRecord record = DungeonPositionSavedData
                    .get(server).findInstance(portalEntity.getPortalId(), state.structurePos, state.config.getDimensionId());
            if (record == null) {
                CobbleSafari.LOGGER.warn("[Hyperspace] no instance record for portal {} - zone not resolved",
                        portalEntity.getPortalId());
                return;
            }
            maxigregrze.cobblesafari.dungeon.hyperspace.HyperspaceZoneResolver.resolve(
                    state.dungeonLevel, record.instanceId(),
                    portalEntity.getDungeonChunkMinX(), portalEntity.getDungeonChunkMinZ(),
                    portalEntity.getDungeonChunkMaxX(), portalEntity.getDungeonChunkMaxZ());
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[Hyperspace] zone resolution failed for portal {}", portalEntity.getPortalId(), e);
        }
        try {
            DungeonInstanceRecord record = DungeonPositionSavedData
                    .get(server).findInstance(portalEntity.getPortalId(), state.structurePos, state.config.getDimensionId());
            if (record != null) {
                maxigregrze.cobblesafari.dungeon.hyperspace.HyperspaceSkyMapService.startForInstance(
                        server, record.instanceId(), portalLevel, portalEntity.getBlockPos());
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[Hyperspace] sky map could not start for portal {}", portalEntity.getPortalId(), e);
        }
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
                    portalEntity.getDungeonChunkMaxZ(),
                    state.slot
            );

            if (state.config.getDimensionKey().equals(DungeonDimensions.DUNGEON_PARIS)) {
                resolveHyperspaceZone(server, state, portalEntity, player.serverLevel());
            }
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

        // The instance exists now (registerActiveInstance ran above, or this portal already had
        // one): this is where a first entry finally learns its scope.
        UUID instanceId = resolveInstanceForPortal(server, portalEntity, validation.dimensionId());

        return new DungeonPrepResult(
                state.dungeonLevel, playerSpawnPos, playerYaw, validation.dimensionId(),
                validation.isReEntry(), validation.timerTicks(),
                validation.playerOriginPos(), validation.playerOriginDimension(), instanceId
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

        UUID instanceId = validation.instanceId() != null
                ? validation.instanceId()
                : resolveInstanceForPortal(server, portalEntity, validation.dimensionId());

        return new DungeonPrepResult(
                dungeonLevel, playerSpawnPos, playerYaw, validation.dimensionId(),
                validation.isReEntry(), validation.timerTicks(),
                validation.playerOriginPos(), validation.playerOriginDimension(), instanceId
        );
    }
}
