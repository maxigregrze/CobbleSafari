package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.api.DungeonRegistrationAPI;
import maxigregrze.cobblesafari.api.PortalExpirationHandler;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.init.ModBlockTags;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import maxigregrze.cobblesafari.data.PortalSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class PortalSpawnManager {

    private static final Map<UUID, ActivePortal> ACTIVE_PORTALS = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static final String LOG_CALLBACK_FAILED = "Portal spawn callback failed for dungeon {}";
    /** How far below a candidate portal the ground is searched (plan 188, section 9.2). */
    private static final int GROUND_SEARCH_DEPTH = 3;
    private static final Predicate<BlockState> IS_REPELLENT = state -> state.is(ModBlockTags.PORTAL_REPELLENTS);
    private static long lastSpawnTick = 0;
    private static MinecraftServer serverInstance;
    private static volatile boolean suppressDungeonCleanup = false;

    /**
     * Which portal id is currently expiring <em>naturally</em> (action plan 182, A1/S5).
     * <p>
     * Both a natural expiry and a pickaxe reach {@link #onPortalBlockRemoved} through
     * {@code Block.onRemove}, so the caller has to say which it is. Deliberately <b>not</b> shaped
     * like {@link #suppressDungeonCleanup}: that is a global {@code boolean} set around a
     * <em>loop</em>, and a global boolean set around a loop mis-classifies any other portal removal
     * triggered from inside it - an API callback, a cascade. Keying the marker to one portal id, and
     * restoring the previous value rather than clearing it, makes it precise and re-entrant. Anything
     * that reaches {@code onRemove} without matching the marker keeps today's immediate wipe, which
     * is the safe direction to fail in.
     */
    private static final ThreadLocal<UUID> NATURAL_EXPIRY_PORTAL = new ThreadLocal<>();

    /** Runs {@code action} marked as the natural expiry of {@code portalId}. */
    private static void withNaturalExpiry(UUID portalId, Runnable action) {
        UUID previous = NATURAL_EXPIRY_PORTAL.get();
        NATURAL_EXPIRY_PORTAL.set(portalId);
        try {
            action.run();
        } finally {
            NATURAL_EXPIRY_PORTAL.set(previous);
        }
    }

    /** Whether the removal currently in progress is the natural expiry of this very portal. */
    private static boolean isNaturalExpiryOf(UUID portalId) {
        return portalId != null && portalId.equals(NATURAL_EXPIRY_PORTAL.get());
    }

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
                                portalData.hasDungeonChunkBounds(),
                                portalData.lifetimeTicks()
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
        scanWorldForPortalsInternal(server);
    }

    /**
     * Re-runs the portal scan on demand ({@code /cs dungeon list portal force}).
     * <p>
     * It used to force every chunk in a 65x65 grid to load - <em>generating</em> the ones that did
     * not exist yet - for every allowed dimension. That is 4 225 synchronous chunk generations per
     * dimension on the server thread, and it reliably tripped the watchdog: a single tick of 60
     * seconds, then a forced shutdown. It now inspects only chunks that are already loaded.
     * <p>
     * Nothing is lost by that. A chunk that was never generated cannot contain a portal, so
     * generating it to look was always pure waste; and a portal sitting in a generated-but-unloaded
     * chunk is inert until that chunk loads, at which point
     * {@code DungeonPortalBlockEntity.serverTick} self-expires it through the existing safety net.
     * What remains - re-registering a live but untracked portal in an area the operator has flown
     * to - is exactly what the command is for.
     */
    public static int forceScanWorldForPortals(MinecraftServer server) {
        return scanWorldForPortalsInternal(server);
    }

    private static int scanWorldForPortalsInternal(MinecraftServer server) {
        int foundPortals = 0;
        for (String dimId : PortalSpawnConfig.getAllowedDimensions()) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimId));
            ServerLevel level = server.getLevel(key);
            if (level != null) {
                foundPortals += scanLevelForPortals(level);
            }
        }

        if (foundPortals > 0) {
            CobbleSafari.LOGGER.info("Found {} additional portal(s) in loaded chunks not in saved data", foundPortals);
            savePortals();
        }

        return foundPortals;
    }

    private static int scanLevelForPortals(ServerLevel level) {
        int foundPortals = 0;

        for (int chunkX = -32; chunkX <= 32; chunkX++) {
            for (int chunkZ = -32; chunkZ <= 32; chunkZ++) {
                // Ask for an already-loaded chunk and accept null: with load = false the chunk
                // source never generates, so "we do not generate" is a property of the call rather
                // than of a guard someone can later bypass - which is precisely how the watchdog
                // crash got in (the old `forceLoad` flag skipped the guard and called
                // level.getChunk, which generates).
                net.minecraft.world.level.chunk.ChunkAccess chunk = level.getChunkSource()
                        .getChunk(chunkX, chunkZ, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
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
                                    spawnTick = level.getGameTime() - (portalEntity.getEffectiveLifetimeTicks() / 2);
                                }

                                ACTIVE_PORTALS.put(portalId, new ActivePortal(
                                        portalId,
                                        pos,
                                        level.dimension(),
                                        spawnTick,
                                        dungeonId,
                                        null, null, 0, 0, 0, 0, false,
                                        portalEntity.getLifetimeTicksOverride()
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
                    portal.hasDungeonChunkBounds(), portal.lifetimeTicks());
        }
        savedData.setDirty();
        CobbleSafari.LOGGER.debug("Saved {} portals to persistent storage", ACTIVE_PORTALS.size());
    }

    public static List<String> getEnabledDungeonIdsForCycle() {
        List<String> ids = new ArrayList<>();
        for (DungeonConfig dungeon : DungeonDimensions.getAllDungeons()) {
            // A sub-dungeon is never listed in the portal cycle (action plan 183, section 3.2).
            if (dungeon.isSubDungeon()) {
                continue;
            }
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
        long currentTick = server.overworld().getGameTime();

        // "enabled" gates natural spawning only: existing portals (Portal Spawner ones included)
        // keep expiring when it is off (plan 188, S0).
        if (PortalSpawnConfig.isEnabled()
                && currentTick - lastSpawnTick >= PortalSpawnConfig.getSpawnIntervalTicks()) {
            trySpawnRandomPortal(server);
            lastSpawnTick = currentTick;
        }

        cleanupExpiredPortals(server, currentTick);
    }

    private static void trySpawnRandomPortal(MinecraftServer server) {
        float chance = PortalSpawnConfig.getSpawnChance();
        if (chance <= 0f) {
            CobbleSafari.LOGGER.debug("Scheduled dungeon portal spawn skipped (spawnChance is 0)");
            return;
        }
        float roll = RANDOM.nextFloat();
        if (roll > chance) {
            CobbleSafari.LOGGER.debug("Scheduled dungeon portal spawn skipped by spawnChance (roll {} > {})", roll, chance);
            return;
        }

        List<ServerPlayer> eligiblePlayers = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() instanceof ServerLevel playerLevel
                    && PortalSpawnConfig.isSpawnDimensionAllowed(playerLevel.dimension())) {
                eligiblePlayers.add(player);
            }
        }

        if (eligiblePlayers.isEmpty()) {
            CobbleSafari.LOGGER.debug("No players in allowed spawn dimensions for portal spawn");
            return;
        }

        int maxAttempts = 3;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            ServerPlayer targetPlayer = eligiblePlayers.get(RANDOM.nextInt(eligiblePlayers.size()));
            ServerLevel spawnLevel = (ServerLevel) targetPlayer.level();
            BlockPos portalPos = findValidSpawnPosition(spawnLevel, targetPlayer.blockPosition());

            if (portalPos != null) {
                spawnPortal(spawnLevel, portalPos, targetPlayer, null);
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

        if (!PortalSpawnConfig.isSpawnDimensionAllowed(level.dimension())) {
            player.sendSystemMessage(Component.translatable(
                    "cobblesafari.command.dungeon.spawn.dimension_not_allowed",
                    level.dimension().location()));
            CobbleSafari.LOGGER.warn("Cannot spawn portal: dimension {} is not allowed for Hoopa portal spawn",
                    level.dimension().location());
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

    /**
     * Why {@code pos} cannot host a natural portal, or {@code null} if it can. Cheapest checks first.
     * The portal floats one block above the surface, so the ground is searched below it rather than
     * assumed at {@code pos.below()} (which is air in the normal case - plan 188, section 9).
     */
    private static String isValidPortalPositionReason(ServerLevel level, BlockPos pos) {
        BlockPos ground = findGround(level, pos);
        if (ground == null) {
            return String.format("No ground within %d blocks below %s", GROUND_SEARCH_DEPTH, pos);
        }
        BlockState groundState = level.getBlockState(ground);
        if (!groundState.is(ModBlockTags.PORTAL_SPAWNABLE_ON)) {
            return String.format("Ground (%s) is not in #cobblesafari:portal_spawnable_on: %s",
                    ground, groundState.getBlock().getName().getString());
        }

        String groundId = BuiltInRegistries.BLOCK.getKey(groundState.getBlock()).toString();
        for (String bannedId : PortalSpawnConfig.getBannedBlocks()) {
            if (groundId.equals(bannedId)) {
                return String.format("Ground (%s) is banned: %s", ground, bannedId);
            }
        }

        if (PortalSpawnConfig.isBiomeBanned(level.getBiome(ground))) {
            return String.format("Biome at %s is banned", ground);
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

        int repellentRadius = PortalSpawnConfig.getRepellentRadius();
        if (hasRepellentNearby(level, pos, repellentRadius)) {
            return String.format("Portal repellent within %d blocks of %s", repellentRadius, pos);
        }

        return null;
    }

    /** First non-air block below the portal, searched {@link #GROUND_SEARCH_DEPTH} deep; fluids count. */
    private static BlockPos findGround(ServerLevel level, BlockPos portalPos) {
        BlockPos cursor = portalPos.below();
        for (int i = 0; i < GROUND_SEARCH_DEPTH; i++) {
            if (!level.getBlockState(cursor).isAir()) {
                return cursor;
            }
            cursor = cursor.below();
        }
        return null;
    }

    /**
     * Whether a {@code #cobblesafari:portal_repellents} block lies in the cube of {@code radius}
     * around {@code center} (plan 188, P5). Only chunk sections whose palette may hold a repellent
     * are walked block by block, and only already-loaded chunks are read: this never loads a chunk.
     */
    private static boolean hasRepellentNearby(ServerLevel level, BlockPos center, int radius) {
        if (radius <= 0) {
            return false;
        }
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + radius);
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    if (section.hasOnlyAir() || !section.maybeHas(IS_REPELLENT)) {
                        continue;
                    }
                    int x0 = Math.max(minX, cx << 4);
                    int x1 = Math.min(maxX, (cx << 4) + 15);
                    int y0 = Math.max(minY, sy << 4);
                    int y1 = Math.min(maxY, (sy << 4) + 15);
                    int z0 = Math.max(minZ, cz << 4);
                    int z1 = Math.min(maxZ, (cz << 4) + 15);
                    if (sectionHasRepellent(section, x0, x1, y0, y1, z0, z1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean sectionHasRepellent(LevelChunkSection section,
                                               int x0, int x1, int y0, int y1, int z0, int z1) {
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    if (IS_REPELLENT.test(section.getBlockState(x & 15, y & 15, z & 15))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void spawnPortal(ServerLevel level, BlockPos pos, ServerPlayer nearestPlayer, DungeonConfig dungeonOrNull) {
        Direction facing = Direction.fromYRot(RANDOM.nextFloat() * 360);
        UUID portalId = spawnManagedPortal(level, pos, facing, dungeonOrNull, level.getGameTime(), -1, true);
        if (portalId != null) {
            CobbleSafari.LOGGER.info("Successfully spawned dungeon portal at {} near player {}",
                    pos, nearestPlayer.getName().getString());
        }
    }

    /**
     * Places an ENTRANCE portal at {@code pos} and tracks it (plan 188, section 11.3).
     *
     * @param dungeonOrNull the destination, or {@code null} for the weighted random pool
     * @param spawnTick game tick the portal is considered born at; may lie in the past (back-dated
     *     Portal Spawner catch-up), the remaining lifetime then being shorter
     * @param lifetimeTicks this portal's own lifetime, or a negative value for the global one
     * @param notify whether nearby players get the (config-gated) "portal appeared" message
     * @return the new portal id, or {@code null} if placement failed
     */
    public static @Nullable UUID spawnManagedPortal(ServerLevel level, BlockPos pos, Direction facing,
                                                    @Nullable DungeonConfig dungeonOrNull, long spawnTick,
                                                    int lifetimeTicks, boolean notify) {
        DungeonConfig dungeon = dungeonOrNull;
        if (dungeon == null) {
            dungeon = DungeonDimensions.getRandomDungeon();
        }
        if (dungeon == null) {
            CobbleSafari.LOGGER.error("No dungeon configuration available");
            return null;
        }

        CobbleSafari.LOGGER.info("Attempting to spawn portal at {} in dimension {}", pos, level.dimension().location());

        BlockState portalState = ModBlocks.DUNGEON_PORTAL.defaultBlockState()
                .setValue(DungeonPortalBlock.PORTAL_TYPE, DungeonPortalBlock.PortalType.ENTRANCE)
                .setValue(DungeonPortalBlock.FACING, facing);

        var oldState = level.getBlockState(pos);
        CobbleSafari.LOGGER.debug("Current block at {}: {}", pos, oldState.getBlock().getName().getString());

        boolean setResult = level.setBlock(pos, portalState, 3);
        if (!setResult) {
            CobbleSafari.LOGGER.error("Failed to set block at {} - setBlock returned false", pos);
            return null;
        }

        var newState = level.getBlockState(pos);
        if (!newState.is(ModBlocks.DUNGEON_PORTAL)) {
            CobbleSafari.LOGGER.error("Block at {} is not DUNGEON_PORTAL after placement! Got: {}", pos, newState.getBlock().getName().getString());
            return null;
        }

        int ownLifetime = Math.max(-1, lifetimeTicks);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DungeonPortalBlockEntity portalEntity) {
            portalEntity.setDungeonDimensionId(dungeon.getId());
            portalEntity.setSpawnTick(spawnTick);
            if (ownLifetime >= 0) {
                portalEntity.setLifetimeTicksOverride(ownLifetime);
            }
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
                spawnTick,
                dungeon.getId(),
                dungeon.getId(),
                null,
                0, 0, 0, 0, false,
                ownLifetime
        ));

        savePortals();

        if (blockEntity instanceof DungeonPortalBlockEntity portalEntity) {
            var callback = DungeonRegistrationAPI.getPortalSpawnCallback(dungeon.getId());
            if (callback != null) {
                try {
                    callback.onPortalSpawned(level, portalEntity);
                } catch (Exception e) {
                    CobbleSafari.LOGGER.error(LOG_CALLBACK_FAILED, dungeon.getId(), e);
                }
            }
        }

        if (notify) {
            notifyNearbyPlayers(level, pos);
        }
        return portalId;
    }

    /**
     * Removes the portal at {@code pos} as a <em>natural</em> expiry: its dungeon instance is
     * detached, nobody is evicted (plan 182, A1). Used by the Portal Spawner (plan 188, S1).
     */
    public static void closePortalNaturally(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DungeonPortalBlockEntity portalEntity) {
            withNaturalExpiry(portalEntity.getPortalId(), () -> level.removeBlock(pos, false));
        }
    }

    private static void notifyNearbyPlayers(ServerLevel level, BlockPos portalPos) {
        if (!PortalSpawnConfig.isNotificationEnabled()) {
            return;
        }

        int radius = PortalSpawnConfig.getNotificationRadius();

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(portalPos, radius)) {
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.portal.appeared_at",
                        DungeonChatComponents.coordinates(player, level.dimension(), portalPos)));
            }
        }
    }

    private static void cleanupExpiredPortals(MinecraftServer server, long currentTick) {
        List<UUID> expiredIds = null;

        for (ActivePortal portal : ACTIVE_PORTALS.values()) {
            if (currentTick - portal.spawnTick() >= portal.effectiveLifetimeTicks()) {
                if (expiredIds == null) {
                    expiredIds = new ArrayList<>();
                }
                expiredIds.add(portal.id());
            }
        }

        if (expiredIds == null) {
            return;
        }

        for (UUID portalId : expiredIds) {
            ActivePortal portal = ACTIVE_PORTALS.get(portalId);
            if (portal == null) continue;

            ServerLevel level = server.getLevel(portal.dimension());
            if (level == null) {
                expireWithoutBlockRemoval(server, portal);
                continue;
            }

            if (!level.hasChunkAt(portal.pos()) && !isExternallyManagedDungeon(portal)) {
                // Chunk not loaded: clean up the dungeon side now without force-loading the
                // chunk. The portal block self-expires via its own ticker the next time the
                // chunk is loaded (see DungeonPortalBlockEntity.serverTick).
                expireWithoutBlockRemoval(server, portal);
                CobbleSafari.LOGGER.debug("Expired portal {} at {} in unloaded chunk, block removal deferred to next chunk load",
                        portalId, portal.pos());
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(portal.pos());
            if (blockEntity instanceof DungeonPortalBlockEntity portalEntity && portalEntity.isAutoRenewPortal()) {
                renewCreativePortal(server, level, portal.pos(), portalEntity);
            } else {
                // Natural expiry: the block goes, the region stays until it empties (plan 182, A1).
                withNaturalExpiry(portalId, () -> level.removeBlock(portal.pos(), false));
                CobbleSafari.LOGGER.debug("Removed expired portal at {} (instance detached)", portal.pos());
            }
        }
    }

    /**
     * Expires a tracked portal without touching its (unloaded or unavailable) block:
     * evacuates and schedules the dungeon-instance cleanup, then drops the tracking entry
     * and saved data. The physical block is removed later by its own ticker.
     */
    private static void expireWithoutBlockRemoval(MinecraftServer server, ActivePortal portal) {
        ACTIVE_PORTALS.remove(portal.id());
        DungeonTeleportHandler.removeInstance(portal.id());
        detachDungeonForPortal(server, portal);
        PortalSavedData.get(server).removePortal(portal.id());
    }

    private static boolean isExternallyManagedDungeon(ActivePortal portal) {
        String dungeonDimId = portal.dungeonDimensionId() != null ? portal.dungeonDimensionId() : portal.dungeonId();
        if (dungeonDimId == null) return false;
        DungeonConfig config = DungeonDimensions.getDungeonById(dungeonDimId);
        return config != null && config.isExternallyManaged();
    }

    /**
     * Expires a portal block in place, called from its own block entity ticker. Safety net for
     * portals whose chunk was unloaded when their lifetime elapsed (or whose tracking entry was
     * lost): the block removes (or renews) itself on the first tick after its chunk is loaded.
     */
    public static void expirePortalInPlace(ServerLevel level, BlockPos pos, DungeonPortalBlockEntity portalEntity) {
        if (portalEntity.isAutoRenewPortal()) {
            renewCreativePortal(level.getServer(), level, pos, portalEntity);
        } else {
            // Self-expiry is a natural expiry: detach, do not wipe (plan 182, A1).
            withNaturalExpiry(portalEntity.getPortalId(), () -> level.removeBlock(pos, false));
            CobbleSafari.LOGGER.debug("Removed expired portal at {} (self-expiry on chunk tick)", pos);
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
                0, 0, 0, 0, false,
                -1
        ));
        savePortals();

        if (dungeon != null) {
            var callback = DungeonRegistrationAPI.getPortalSpawnCallback(dungeon.getId());
            if (callback != null) {
                try {
                    callback.onPortalSpawned(level, portalEntity);
                } catch (Exception e) {
                    CobbleSafari.LOGGER.error(LOG_CALLBACK_FAILED, dungeon.getId(), e);
                }
            }
        }
        return true;
    }

    private static void renewCreativePortal(MinecraftServer server, ServerLevel level, BlockPos pos, DungeonPortalBlockEntity portalEntity) {
        UUID oldPortalId = portalEntity.getPortalId();
        ActivePortal oldPortal = ACTIVE_PORTALS.get(oldPortalId);

        // A renewal cycle is a natural expiry of the old portal: without the marker each cycle would
        // wipe the previous instance and evict whoever is still inside it (plan 58, plan 182 A1).
        withNaturalExpiry(oldPortalId, () -> onPortalBlockRemoved(portalEntity));

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
        String fallbackDungeonId = oldPortal != null ? oldPortal.dungeonId() : "random";
        String activeDungeonId = dungeon != null ? dungeon.getId() : fallbackDungeonId;
        ACTIVE_PORTALS.put(newPortalId, new ActivePortal(
                newPortalId,
                pos,
                level.dimension(),
                level.getGameTime(),
                activeDungeonId,
                renewedDungeonDimensionId,
                null,
                0, 0, 0, 0, false,
                -1
        ));
        savePortals();

        if (dungeon != null) {
            var callback = DungeonRegistrationAPI.getPortalSpawnCallback(dungeon.getId());
            if (callback != null) {
                try {
                    callback.onPortalSpawned(level, portalEntity);
                } catch (Exception e) {
                    CobbleSafari.LOGGER.error(LOG_CALLBACK_FAILED, dungeon.getId(), e);
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
                if (config == null) {
                    config = DungeonPositionSavedData.resolveDungeonConfig(dungeonDimId);
                }
                if (portal != null || hasPendingInstanceRecord(portalEntity, config)) {
                    if (isNaturalExpiryOf(portalId)) {
                        // The entrance closed on its own: keep the region for whoever is inside it.
                        DungeonInstanceCleanup.detachInstanceForPortal(serverInstance, portalEntity);
                    } else {
                        // Deliberate destruction - pickaxe, explosion, /setblock. Still the panic
                        // button: wipe now, evacuating anyone inside first.
                        if (config != null) {
                            ServerLevel dungeonLevel = serverInstance.getLevel(config.getDimensionKey());
                            if (dungeonLevel != null) {
                                evacuatePlayersFromDungeon(dungeonLevel, portalEntity, config);
                            }
                        }
                        DungeonInstanceCleanup.scheduleDeletionForPortal(serverInstance, portalEntity);
                    }
                } else {
                    // Untracked portal block with no live instance record: its dungeon cleanup
                    // already ran when it expired (deferred removal), or its bounds are stale.
                    // Re-clearing here could wipe a region since reallocated to another dungeon.
                    CobbleSafari.LOGGER.debug("Skipped dungeon cleanup for untracked portal {} (no pending instance record)", portalId);
                }
            }

            if (portal != null) {
                PortalSavedData.get(serverInstance).removePortal(portalId);
            }
            CobbleSafari.LOGGER.info("Portal {} removed from active list (block destroyed)", portalId);
        }
    }

    private static boolean hasPendingInstanceRecord(DungeonPortalBlockEntity portalEntity, DungeonConfig config) {
        if (serverInstance == null || config == null) return false;
        BlockPos structurePos = portalEntity.getDungeonStructurePos();
        if (structurePos == null) return false;
        return DungeonPositionSavedData.get(serverInstance)
                .findInstance(portalEntity.getPortalId(), structurePos, config.getDimensionId()) != null;
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
                    chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ, true,
                    old.lifetimeTicks()
            ));
            savePortals();
        }
    }

    /**
     * Natural expiry of a portal whose block is unavailable (unloaded chunk): the instance is
     * detached rather than cleared, and released by the sweep once it empties (plan 182, A1).
     */
    private static void detachDungeonForPortal(MinecraftServer server, ActivePortal portal) {
        if (portal.dungeonDimensionId() == null) return;

        DungeonConfig config = DungeonDimensions.getDungeonById(portal.dungeonDimensionId());
        if (config == null || config.isExternallyManaged()) {
            return;
        }
        if (!portal.hasDungeonChunkBounds()) return;

        DungeonInstanceCleanup.detachInstanceForActivePortal(server, portal);
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
                // Resolve the scope from where the player actually stands: this region belongs to
                // one instance, and evacuating them under another instance's session would send
                // them to the wrong origin.
                maxigregrze.cobblesafari.data.TimerScope scope =
                        maxigregrze.cobblesafari.manager.TimerManager.currentScope(player);
                if (scope == null) {
                    scope = maxigregrze.cobblesafari.data.TimerScope.of(config.getDimensionId());
                }
                maxigregrze.cobblesafari.manager.TimerManager.expireSessionAndTeleport(player, scope, false);
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
            boolean hasDungeonChunkBounds,
            int lifetimeTicks
    ) {
        public ActivePortal(UUID id, BlockPos pos, net.minecraft.resources.ResourceKey<Level> dimension,
                            long spawnTick, String dungeonId) {
            this(id, pos, dimension, spawnTick, dungeonId, null, null, 0, 0, 0, 0, false, -1);
        }

        /** This portal's own lifetime ({@code lifetimeTicks} >= 0), else the global one (plan 188, S0). */
        public int effectiveLifetimeTicks() {
            return lifetimeTicks >= 0 ? lifetimeTicks : PortalSpawnConfig.getPortalLifetimeTicks();
        }
    }
}
