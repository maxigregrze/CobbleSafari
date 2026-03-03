package maxigregrze.cobblesafari.manager;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.DimensionTimerEntry;
import maxigregrze.cobblesafari.config.SafariConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.data.TimerSavedData;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.DungeonTeleportHandler;
import maxigregrze.cobblesafari.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TimerManager {
    private static final Map<UUID, Map<String, PlayerTimerData>> activeTimers = new ConcurrentHashMap<>();
    private static MinecraftServer serverInstance;

    public static void setServer(MinecraftServer server) {
        serverInstance = server;
    }

    public static void clearServer() {
        serverInstance = null;
        activeTimers.clear();
    }

    public static boolean shouldBypassTimer(ServerPlayer player) {
        String safariDimId = SafariTimerConfig.getSafariDimensionId();
        PlayerTimerData data = getOrCreateData(player, safariDimId);
        return data.isTimerBypassed();
    }

    public static void toggleBypass(ServerPlayer player, Boolean forceState) {
        String safariDimId = SafariTimerConfig.getSafariDimensionId();
        PlayerTimerData data = getOrCreateData(player, safariDimId);

        boolean newState;
        if (forceState != null) {
            newState = forceState;
        } else {
            newState = !data.isTimerBypassed();
        }

        data.setTimerBypassed(newState);
        savePlayerData(player, data);
        syncToClient(player, data, newState);

        CobbleSafari.LOGGER.info("Timer bypass for player {} set to {}",
                player.getName().getString(), newState);
    }

    public static boolean isInSafariDimension(ServerPlayer player) {
        ResourceLocation dimensionId = ResourceLocation.parse(SafariTimerConfig.getSafariDimensionId());
        ResourceLocation playerDimension = player.level().dimension().location();
        return playerDimension.equals(dimensionId);
    }

    public static Optional<String> getConfiguredDimensionId(ServerPlayer player) {
        String playerDimension = player.level().dimension().location().toString();
        if (SafariTimerConfig.hasDimensionTimer(playerDimension)) {
            return Optional.of(playerDimension);
        }
        return Optional.empty();
    }

    public static void startTimer(ServerPlayer player, String dimensionId) {
        UUID playerId = player.getUUID();
        PlayerTimerData data = getOrCreateData(player, dimensionId);

        checkDailyReset(player, data);

        data.setActive(true);
        activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(dimensionId, data);
        syncToClient(player, data);

        CobbleSafari.LOGGER.debug("Timer started for player {} in dimension {} with {} ticks remaining",
                player.getName().getString(), dimensionId, data.getRemainingTicks());
    }

    @Deprecated
    public static void startTimer(ServerPlayer player) {
        startTimer(player, SafariTimerConfig.getSafariDimensionId());
    }

    public static void pauseTimer(ServerPlayer player, String dimensionId) {
        UUID playerId = player.getUUID();
        Map<String, PlayerTimerData> playerTimers = activeTimers.get(playerId);

        if (playerTimers != null) {
            PlayerTimerData data = playerTimers.get(dimensionId);
            if (data != null) {
                data.setActive(false);
                savePlayerData(player, data);
                syncToClient(player, data);

                CobbleSafari.LOGGER.debug("Timer paused for player {} in dimension {} with {} ticks remaining",
                        player.getName().getString(), dimensionId, data.getRemainingTicks());
            }
        }
    }

    @Deprecated
    public static void pauseTimer(ServerPlayer player) {
        pauseTimer(player, SafariTimerConfig.getSafariDimensionId());
    }

    public static void pauseAllTimers(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Map<String, PlayerTimerData> playerTimers = activeTimers.get(playerId);

        if (playerTimers != null) {
            for (PlayerTimerData data : playerTimers.values()) {
                if (data.isActive()) {
                    data.setActive(false);
                    savePlayerData(player, data);
                    syncToClient(player, data);
                }
            }
        }
    }

    public static void tickAllTimers() {
        if (serverInstance == null) return;

        for (Map.Entry<UUID, Map<String, PlayerTimerData>> playerEntry : activeTimers.entrySet()) {
            UUID playerId = playerEntry.getKey();
            Map<String, PlayerTimerData> dimensionTimers = playerEntry.getValue();

            for (Map.Entry<String, PlayerTimerData> timerEntry : dimensionTimers.entrySet()) {
                String timerDimensionId = timerEntry.getKey();
                PlayerTimerData data = timerEntry.getValue();

                if (data.isActive()) {
                    ServerPlayer player = getPlayerByUUID(playerId);

                    if (player != null) {
                        String playerDimension = player.level().dimension().location().toString();

                        if (!playerDimension.equals(timerDimensionId)) {
                            data.setActive(false);
                            savePlayerData(player, data);
                            syncToClient(player, data);
                            CobbleSafari.LOGGER.debug("Timer auto-paused for player {} - no longer in dimension {}",
                                    player.getName().getString(), timerDimensionId);
                            continue;
                        }

                        if (DungeonDimensions.isDungeonDimension(timerDimensionId)) {
                            boolean dungeonStillExists = checkDungeonStillExists(player, timerDimensionId);
                            if (!dungeonStillExists) {
                                CobbleSafari.LOGGER.warn("Player {} is in dungeon {} but dungeon no longer exists, evacuating",
                                        player.getName().getString(), timerDimensionId);
                                data.setRemainingTicks(0);
                                teleportOnTimerExpired(player, timerDimensionId, data);
                                data.setActive(false);
                                savePlayerData(player, data);
                                continue;
                            }
                        }

                        if (DungeonDimensions.isDungeonDimension(timerDimensionId) && player.getY() < 0) {
                            data.setRemainingTicks(0);
                            teleportOnTimerExpired(player, timerDimensionId, data);
                            data.setActive(false);
                            savePlayerData(player, data);
                            CobbleSafari.LOGGER.info("Player {} detected in void in dungeon dimension {}, teleporting out",
                                    player.getName().getString(), timerDimensionId);
                            continue;
                        }

                        if (shouldBypassTimer(player)) {
                            if (serverInstance.getTickCount() % 20 == 0) {
                                syncToClient(player, data, true);
                            }
                            continue;
                        }
                    }

                    data.tick();

                    if (data.isExpired()) {
                        if (player != null) {
                            teleportOnTimerExpired(player, timerDimensionId, data);
                            data.setActive(false);
                            savePlayerData(player, data);
                        }
                    }

                    if (data.getRemainingTicks() % 20 == 0) {
                        if (player != null) {
                            syncToClient(player, data);
                        }
                    }
                }
            }
        }
    }

    public static void checkDailyReset(ServerPlayer player, PlayerTimerData data) {
        Optional<DimensionTimerEntry> configOpt = SafariTimerConfig.getDimensionConfig(data.getDimensionId());
        boolean allowReset = configOpt.map(DimensionTimerEntry::isAllowReset).orElse(true);
        if (!allowReset) return;

        long lastResetMs = data.getLastResetTimestamp();

        LocalDate lastResetDate = Instant.ofEpochMilli(lastResetMs)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalTime now = LocalTime.now(ZoneId.systemDefault());
        int resetHour = data.getResetHour();

        boolean shouldReset = false;

        if (today.isAfter(lastResetDate)) {
            if (now.getHour() >= resetHour) {
                shouldReset = true;
            } else if (today.isAfter(lastResetDate.plusDays(1))) {
                shouldReset = true;
            }
        }

        if (shouldReset) {
            String dimensionId = data.getDimensionId();
            boolean isSafari = dimensionId.equals(SafariTimerConfig.getSafariDimensionId());
            boolean playerInSafari = isSafari && isInSafariDimension(player);

            if (playerInSafari && shouldKickOnDailyReset()) {
                teleportOnTimerExpired(player, dimensionId, data);
                player.sendSystemMessage(Component.translatable("cobblesafari.reset.daily_kick"));
            }

            data.reset();
            data.resetEntryFeePayDay();
            data.setLastSafariBallGrantDay(0);
            player.sendSystemMessage(Component.translatable("cobblesafari.timer.reset"));
            CobbleSafari.LOGGER.info("Timer reset for player {} in dimension {} (daily reset)",
                    player.getName().getString(), data.getDimensionId());
        }
    }

    private static boolean shouldKickOnDailyReset() {
        if (SafariConfig.isKickOnReset()) return true;
        return SafariConfig.isEntryFeeEnabled();
    }

    public static void teleportToOverworldSpawn(ServerPlayer player) {
        if (serverInstance == null) return;

        ServerLevel overworld = serverInstance.getLevel(Level.OVERWORLD);

        if (overworld != null) {
            BlockPos spawnPos = overworld.getSharedSpawnPos();

            player.teleportTo(
                    overworld,
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot()
            );
            player.resetFallDistance();
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

            player.sendSystemMessage(Component.translatable("cobblesafari.timer.expired"));
            CobbleSafari.LOGGER.info("Player {} teleported to Overworld spawn (timer expired)",
                    player.getName().getString());
        }
    }

    public static void teleportOnTimerExpired(ServerPlayer player, String dimensionId, PlayerTimerData data) {
        if (serverInstance == null) return;

        Optional<DimensionTimerEntry> config = SafariTimerConfig.getDimensionConfig(dimensionId);
        boolean returnToSpawn = config.map(DimensionTimerEntry::isReturnToSpawn).orElse(false);

        BlockPos targetPos;
        ServerLevel targetLevel;

        if (returnToSpawn) {
            targetLevel = serverInstance.getLevel(Level.OVERWORLD);
            if (targetLevel != null) {
                targetPos = targetLevel.getSharedSpawnPos();
            } else {
                CobbleSafari.LOGGER.error("Overworld not found for timer expiration");
                return;
            }
        } else {
            if (data.getOriginPos() != null && data.getOriginDimension() != null) {
                targetPos = data.getOriginPos();
                targetLevel = serverInstance.getLevel(data.getOriginDimension());
                if (targetLevel == null) {
                    CobbleSafari.LOGGER.warn("Origin dimension {} not found, falling back to overworld spawn",
                            data.getOriginDimension().location());
                    targetLevel = serverInstance.getLevel(Level.OVERWORLD);
                    if (targetLevel != null) {
                        targetPos = targetLevel.getSharedSpawnPos();
                    } else {
                        return;
                    }
                }
            } else {
                targetLevel = serverInstance.getLevel(Level.OVERWORLD);
                if (targetLevel != null) {
                    targetPos = targetLevel.getSharedSpawnPos();
                } else {
                    CobbleSafari.LOGGER.error("Overworld not found and no origin position stored");
                    return;
                }
            }
        }

        player.teleportTo(
                targetLevel,
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
        player.resetFallDistance();
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        if (DungeonDimensions.isDungeonDimension(dimensionId)) {
            DungeonTeleportHandler.clearPlayerData(player.getUUID());
        }

        player.sendSystemMessage(Component.translatable("cobblesafari.timer.expired"));
        CobbleSafari.LOGGER.info("Player {} teleported to {} at {} (timer expired, returnToSpawn: {})",
                player.getName().getString(), targetLevel.dimension().location(), targetPos, returnToSpawn);
    }

    public static void setPlayerOrigin(ServerPlayer player, String dimensionId, BlockPos originPos, ResourceKey<Level> originDimension) {
        PlayerTimerData data = getOrCreateData(player, dimensionId);
        data.setOriginPos(originPos);
        data.setOriginDimension(originDimension);
        savePlayerData(player, data);
        CobbleSafari.LOGGER.debug("Set origin position for player {} in dimension {}: {} in {}",
                player.getName().getString(), dimensionId, originPos, originDimension.location());
    }

    private static final String SAFARI_BALL_ITEM_ID = "cobblemon:safari_ball";

    public static void grantSafariBallsIfFirstUseToday(ServerPlayer player) {
        if (serverInstance == null) return;
        if (!isInSafariDimension(player)) return;

        String safariDimensionId = SafariTimerConfig.getSafariDimensionId();
        PlayerTimerData data = getOrCreateData(player, safariDimensionId);
        long todayEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay();

        if (data.getLastSafariBallGrantDay() == todayEpochDay) {
            return;
        }

        int safariBallsCount = SafariConfig.getDailySafariBallsCount();
        int baitCount = SafariConfig.getDailyBaitCount();
        int mudBallCount = SafariConfig.getDailyMudBallCount();

        if (safariBallsCount > 0) {
            Item safariBall = BuiltInRegistries.ITEM.get(ResourceLocation.parse(SAFARI_BALL_ITEM_ID));
            if (safariBall != null && !safariBall.getDefaultInstance().isEmpty()) {
                ItemStack toGive = new ItemStack(safariBall, safariBallsCount);
                if (!player.getInventory().add(toGive)) {
                    player.drop(toGive, false);
                }
            } else {
                CobbleSafari.LOGGER.warn("Safari ball item not found: {}", SAFARI_BALL_ITEM_ID);
            }
        }

        if (baitCount > 0) {
            ItemStack bait = new ItemStack(maxigregrze.cobblesafari.init.ModItems.BAIT, baitCount);
            if (!player.getInventory().add(bait)) {
                player.drop(bait, false);
            }
        }

        if (mudBallCount > 0) {
            ItemStack mudBalls = new ItemStack(maxigregrze.cobblesafari.init.ModItems.MUD_BALL, mudBallCount);
            if (!player.getInventory().add(mudBalls)) {
                player.drop(mudBalls, false);
            }
        }

        data.setLastSafariBallGrantDay(todayEpochDay);
        savePlayerData(player, data);
        player.sendSystemMessage(Component.translatable("cobblesafari.teleporter.safari_items_received", 
                safariBallsCount, baitCount, mudBallCount));
        CobbleSafari.LOGGER.info("Granted {} Safari Balls, {} Bait, {} Mud Balls to {} (first use today)", 
                safariBallsCount, baitCount, mudBallCount, player.getName().getString());
    }

    public static void syncToClient(ServerPlayer player, PlayerTimerData data, boolean bypassed) {
        ModNetworking.sendTimerSync(player, data.getDimensionId(), data.getRemainingTicks(), data.isActive(), bypassed);
    }

    public static void syncToClient(ServerPlayer player, PlayerTimerData data) {
        syncToClient(player, data, false);
    }

    public static PlayerTimerData getOrCreateData(ServerPlayer player, String dimensionId) {
        UUID playerId = player.getUUID();

        Map<String, PlayerTimerData> playerTimers = activeTimers.get(playerId);
        if (playerTimers != null) {
            PlayerTimerData data = playerTimers.get(dimensionId);
            if (data != null) {
                return data;
            }
        }

        if (serverInstance != null) {
            TimerSavedData savedData = TimerSavedData.get(serverInstance);
            PlayerTimerData data = savedData.getOrCreatePlayerData(playerId, dimensionId);
            activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(dimensionId, data);
            return data;
        }

        PlayerTimerData data = new PlayerTimerData(playerId, dimensionId);
        activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(dimensionId, data);
        return data;
    }

    @Deprecated
    public static PlayerTimerData getOrCreateData(ServerPlayer player) {
        return getOrCreateData(player, SafariTimerConfig.getSafariDimensionId());
    }

    public static void loadPlayerData(ServerPlayer player) {
        if (serverInstance == null) return;

        UUID playerId = player.getUUID();
        TimerSavedData savedData = TimerSavedData.get(serverInstance);

        for (String dimensionId : SafariTimerConfig.getConfiguredDimensionIds()) {
            PlayerTimerData data = savedData.getOrCreatePlayerData(playerId, dimensionId);
            checkDailyReset(player, data);
            activeTimers.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(dimensionId, data);
        }

        String safariDimId = SafariTimerConfig.getSafariDimensionId();
        PlayerTimerData safariData = savedData.getOrCreatePlayerData(playerId, safariDimId);
        if (safariData.isNeedsEvacuation()) {
            SafariResetManager.addPendingEvacuation(playerId);
            safariData.setNeedsEvacuation(false);
            savedData.setPlayerData(playerId, safariDimId, safariData);
        }

        Optional<String> currentDimension = getConfiguredDimensionId(player);
        if (currentDimension.isPresent()) {
            String dimensionId = currentDimension.get();
            PlayerTimerData data = getOrCreateData(player, dimensionId);
            data.setActive(true);
            syncToClient(player, data);
        }

        CobbleSafari.LOGGER.debug("Loaded timer data for player {}", player.getName().getString());
    }

    public static void savePlayerData(ServerPlayer player, PlayerTimerData data) {
        if (serverInstance == null) return;

        TimerSavedData savedData = TimerSavedData.get(serverInstance);
        savedData.setPlayerData(player.getUUID(), data.getDimensionId(), data);
    }

    public static void saveAllData() {
        if (serverInstance == null) return;

        TimerSavedData savedData = TimerSavedData.get(serverInstance);
        int totalTimers = 0;

        for (Map.Entry<UUID, Map<String, PlayerTimerData>> playerEntry : activeTimers.entrySet()) {
            UUID playerId = playerEntry.getKey();
            for (Map.Entry<String, PlayerTimerData> timerEntry : playerEntry.getValue().entrySet()) {
                savedData.setPlayerData(playerId, timerEntry.getKey(), timerEntry.getValue());
                totalTimers++;
            }
        }

        CobbleSafari.LOGGER.info("Saved all timer data ({} players, {} timers)", activeTimers.size(), totalTimers);
    }

    public static void resetDimensionTimers(String dimensionId) {
        for (Map<String, PlayerTimerData> playerTimers : activeTimers.values()) {
            playerTimers.remove(dimensionId);
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Map<String, PlayerTimerData> playerTimers = activeTimers.remove(playerId);

        if (playerTimers != null) {
            for (PlayerTimerData data : playerTimers.values()) {
                data.setActive(false);
                savePlayerData(player, data);
            }
        }
    }

    private static boolean checkDungeonStillExists(ServerPlayer player, String dimensionId) {
        List<maxigregrze.cobblesafari.dungeon.PortalSpawnManager.ActivePortal> activePortals =
                maxigregrze.cobblesafari.dungeon.PortalSpawnManager.getActivePortals();
        
        for (maxigregrze.cobblesafari.dungeon.PortalSpawnManager.ActivePortal portal : activePortals) {
            if (portal.dungeonDimensionId() != null) {
                maxigregrze.cobblesafari.dungeon.DungeonConfig config = 
                        maxigregrze.cobblesafari.dungeon.DungeonDimensions.getDungeonById(portal.dungeonDimensionId());
                if (config != null && config.getDimensionId().equals(dimensionId)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private static ServerPlayer getPlayerByUUID(UUID uuid) {
        if (serverInstance == null) return null;
        return serverInstance.getPlayerList().getPlayer(uuid);
    }
}
