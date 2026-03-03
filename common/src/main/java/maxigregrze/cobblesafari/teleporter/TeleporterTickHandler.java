package maxigregrze.cobblesafari.teleporter;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.SafariConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.compat.EntryFeeHelper;
import maxigregrze.cobblesafari.manager.SafariResetManager;
import maxigregrze.cobblesafari.manager.TimerManager;
import maxigregrze.cobblesafari.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleporterTickHandler {

    private static final int GRACE_PERIOD_TICKS = 5;
    private static final int NO_TIME_MESSAGE_COOLDOWN_TICKS = 600;
    private static final int RESET_DENIED_COOLDOWN_TICKS = 100;
    private static final int TELEPORT_COOLDOWN_TICKS = 300;
    private static final Map<UUID, Long> playersOnTeleporter = new ConcurrentHashMap<>();
    private static final Set<UUID> pendingConfirmation = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> declinedOrFailed = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> noTimeMessageCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> resetDeniedCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> teleportCooldown = new ConcurrentHashMap<>();
    private static long currentServerTick = 0;

    private TeleporterTickHandler() {}

    public static void register() {
    }

    public static void onServerTick(MinecraftServer server) {
        currentServerTick++;

        Iterator<Map.Entry<UUID, Long>> iterator = playersOnTeleporter.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID playerId = entry.getKey();
            long lastSeenTick = entry.getValue();

            if (currentServerTick - lastSeenTick > GRACE_PERIOD_TICKS) {
                iterator.remove();
                if (pendingConfirmation.remove(playerId)) {
                    ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        ModNetworking.sendCloseTpAccept(player);
                        player.sendSystemMessage(Component.translatable("cobblesafari.teleporter.cancelled"));
                    }
                }
                declinedOrFailed.remove(playerId);
            }
        }
    }

    public static void updatePlayerOnTeleporter(ServerPlayer player) {
        UUID playerId = player.getUUID();
        playersOnTeleporter.put(playerId, currentServerTick);

        if (pendingConfirmation.contains(playerId) || declinedOrFailed.contains(playerId)) {
            return;
        }

        Long lastTeleport = teleportCooldown.get(playerId);
        if (lastTeleport != null && currentServerTick - lastTeleport < TELEPORT_COOLDOWN_TICKS) {
            return;
        }

        if (SafariResetManager.isResetPending()) {
            Long lastMessageTick = resetDeniedCooldown.get(playerId);
            if (lastMessageTick == null || currentServerTick - lastMessageTick > RESET_DENIED_COOLDOWN_TICKS) {
                player.sendSystemMessage(Component.translatable(
                        "cobblesafari.reset.entry_denied", SafariResetManager.getFormattedRemainingTime()));
                resetDeniedCooldown.put(playerId, currentServerTick);
            }
            return;
        }

        PlayerTimerData timerData = TimerManager.getOrCreateData(player, SafariTimerConfig.getSafariDimensionId());
        TimerManager.checkDailyReset(player, timerData);

        boolean bypassed = TimerManager.shouldBypassTimer(player);

        if (timerData.getRemainingTicks() <= 0 && !bypassed) {
            boolean canPayAgain = SafariConfig.isAllowMultiplePayment() && SafariConfig.isEntryFeeEnabled();
            if (!canPayAgain) {
                Long lastMessageTick = noTimeMessageCooldown.get(playerId);
                if (lastMessageTick == null || currentServerTick - lastMessageTick > NO_TIME_MESSAGE_COOLDOWN_TICKS) {
                    player.sendSystemMessage(Component.translatable("cobblesafari.teleporter.no_time"));
                    noTimeMessageCooldown.put(playerId, currentServerTick);
                }
                return;
            }
            timerData.resetEntryFeePayDay();
        }

        boolean enableFee = SafariConfig.isEntryFeeEnabled();
        boolean useCobbledollar = SafariConfig.isCobbledollarFeeEnabled();
        EntryFeeHelper.FeeType feeType = EntryFeeHelper.getEffectiveFeeType(enableFee, useCobbledollar);
        boolean hasFee = feeType != EntryFeeHelper.FeeType.NONE;
        boolean isCobbledollarFee = feeType == EntryFeeHelper.FeeType.COBBLEDOLLAR;
        int feeAmount = SafariConfig.getEntryFeeAmount();
        String feeItem = SafariConfig.getEntryFeeItem();
        String dimensionName = Component.translatable("dimension.cobblesafari.domedimension").getString();
        String dimensionId = "domedimension";

        boolean alreadyPaid = timerData.hasPaidEntryFeeToday();

        ModNetworking.sendOpenTpAccept(player, dimensionName, dimensionId, hasFee, isCobbledollarFee, feeAmount, feeItem, "safari", alreadyPaid);
        pendingConfirmation.add(playerId);
    }

    public static void handleAcceptResponse(ServerPlayer player, boolean accepted) {
        UUID playerId = player.getUUID();

        if (!pendingConfirmation.remove(playerId)) {
            ModNetworking.sendCloseTpAccept(player);
            return;
        }

        if (!playersOnTeleporter.containsKey(playerId)) {
            ModNetworking.sendCloseTpAccept(player);
            player.sendSystemMessage(Component.translatable("cobblesafari.teleporter.cancelled"));
            declinedOrFailed.add(playerId);
            return;
        }

        if (!accepted) {
            declinedOrFailed.add(playerId);
            return;
        }

        PlayerTimerData timerData = TimerManager.getOrCreateData(player, SafariTimerConfig.getSafariDimensionId());
        TimerManager.checkDailyReset(player, timerData);

        boolean bypassed = TimerManager.shouldBypassTimer(player);
        boolean needsTimerReset = false;

        if (timerData.getRemainingTicks() <= 0 && !bypassed) {
            boolean canPayAgain = SafariConfig.isAllowMultiplePayment() && SafariConfig.isEntryFeeEnabled();
            if (!canPayAgain) {
                player.sendSystemMessage(Component.translatable("cobblesafari.teleporter.no_time"));
                ModNetworking.sendCloseTpAccept(player);
                declinedOrFailed.add(playerId);
                return;
            }
            needsTimerReset = true;
        }

        if (!timerData.hasPaidEntryFeeToday()) {
            boolean charged = EntryFeeHelper.tryChargeFee(player,
                    SafariConfig.isEntryFeeEnabled(),
                    SafariConfig.getEntryFeeItem(),
                    SafariConfig.isCobbledollarFeeEnabled(),
                    SafariConfig.getEntryFeeAmount());
            if (!charged) {
                player.sendSystemMessage(Component.translatable("cobblesafari.teleporter.fee.insufficient"));
                ModNetworking.sendCloseTpAccept(player);
                declinedOrFailed.add(playerId);
                return;
            }
            timerData.markEntryFeePaidToday();
            TimerManager.savePlayerData(player, timerData);
        }

        if (needsTimerReset) {
            timerData.reset();
            TimerManager.savePlayerData(player, timerData);
        }

        CobbleSafari.LOGGER.info("Player {} accepted safari teleport, initiating teleport sequence",
                player.getName().getString());
        teleportToSafari(player);
    }

    public static void cancelTeleport(ServerPlayer player) {
        UUID playerId = player.getUUID();
        pendingConfirmation.remove(playerId);
        declinedOrFailed.add(playerId);
    }

    public static boolean isTeleporting(ServerPlayer player) {
        return pendingConfirmation.contains(player.getUUID());
    }

    public static void setTeleportCooldown(ServerPlayer player) {
        teleportCooldown.put(player.getUUID(), currentServerTick);
    }

    private static final Random RANDOM = new Random();
    private static final int SLOW_FALLING_DURATION_TICKS = 200;

    private static void teleportToSafari(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        ResourceLocation safariDimension = ResourceLocation.parse(SafariTimerConfig.getSafariDimensionId());
        ResourceKey<Level> dimensionKey = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                safariDimension
        );

        ServerLevel targetLevel = server.getLevel(dimensionKey);
        if (targetLevel == null) {
            CobbleSafari.LOGGER.error("Safari dimension not found: {}", safariDimension);
            ModNetworking.sendCloseTpAccept(player);
            player.sendSystemMessage(Component.translatable("cobblesafari.teleporter.error"));
            return;
        }

        int radius = SafariConfig.getTeleporterRadius();
        double x = RANDOM.nextDouble() * radius * 2 - radius + 0.5;
        double z = RANDOM.nextDouble() * radius * 2 - radius + 0.5;

        
        targetLevel.getChunk((int) x >> 4, (int) z >> 4);
        int surfaceY = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        int teleportY = surfaceY + 32;
        Vec3 targetPos = new Vec3(x, teleportY, z);

        BlockPos originPos = player.blockPosition();
        ResourceKey<Level> originDimension = player.level().dimension();

        DimensionTransition transition = new DimensionTransition(
                targetLevel,
                targetPos,
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING
        );

        player.changeDimension(transition);
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_DURATION_TICKS, 0, false, true));

        TimerManager.setPlayerOrigin(player, SafariTimerConfig.getSafariDimensionId(),
                originPos, originDimension);

        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(player.getUUID());
            if (p != null) {
                TimerManager.grantSafariBallsIfFirstUseToday(p);
            }
        });

        UUID playerId = player.getUUID();
        playersOnTeleporter.remove(playerId);
        pendingConfirmation.remove(playerId);
        declinedOrFailed.remove(playerId);
        teleportCooldown.put(playerId, currentServerTick);

        CobbleSafari.LOGGER.info("Player {} teleported to Safari dimension at ({}, {}, {})",
                player.getName().getString(), (int) x, teleportY, (int) z);
    }
}
