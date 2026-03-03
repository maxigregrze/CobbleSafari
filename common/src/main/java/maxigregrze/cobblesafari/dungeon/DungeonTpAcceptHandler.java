package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.api.DungeonRegistrationAPI;
import maxigregrze.cobblesafari.api.PortalInterceptor;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.compat.EntryFeeHelper;
import maxigregrze.cobblesafari.manager.TimerManager;
import maxigregrze.cobblesafari.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonTpAcceptHandler {

    private static final Map<UUID, PendingDungeonEntry> pendingConfirmations = new ConcurrentHashMap<>();

    private DungeonTpAcceptHandler() {}

    private record PendingDungeonEntry(
            BlockPos portalPos,
            ResourceKey<Level> portalDimension,
            String dungeonDimensionId
    ) {}

    public static void openTpAcceptForDungeon(ServerPlayer player, DungeonPortalBlockEntity portalEntity) {
        UUID playerId = player.getUUID();

        if (pendingConfirmations.containsKey(playerId)) {
            return;
        }

        if (DungeonTeleportCountdown.isTeleporting(playerId)) {
            return;
        }

        DungeonTeleportHandler.DungeonValidationResult validation =
                DungeonTeleportHandler.validateDungeonEntry(player, portalEntity);
        if (validation == null) {
            return;
        }

        String dungeonId = validation.config().getId();

        PortalInterceptor interceptor = DungeonRegistrationAPI.getPortalInterceptor(dungeonId);
        if (interceptor != null && interceptor.handlePortalInteraction(player, portalEntity, validation.config())) {
            return;
        }

        if (validation.config().shouldSkipTeleportScreen()) {
            processDungeonTeleport(player, portalEntity,
                    new PendingDungeonEntry(portalEntity.getBlockPos(), player.level().dimension(), dungeonId));
            return;
        }

        String dimensionId = dungeonId;

        String dimensionTranslationKey = "dimension.cobblesafari." + dungeonId;
        String dimensionName = Component.translatable(dimensionTranslationKey).getString();

        Optional<DungeonDimensionEntry> dimConfig = PortalSpawnConfig.getDimensionConfig(dungeonId);

        boolean enableFee = dimConfig.map(DungeonDimensionEntry::isEntryFeeEnabled).orElse(false);
        boolean useCobbledollar = dimConfig.map(DungeonDimensionEntry::isCobbledollarEntryFee).orElse(false);
        EntryFeeHelper.FeeType feeType = EntryFeeHelper.getEffectiveFeeType(enableFee, useCobbledollar);
        boolean hasFee = feeType != EntryFeeHelper.FeeType.NONE;
        boolean isCobbledollarFee = feeType == EntryFeeHelper.FeeType.COBBLEDOLLAR;
        int feeAmount = dimConfig.map(DungeonDimensionEntry::getEntryFeeAmount).orElse(5000);
        String feeItem = dimConfig.map(DungeonDimensionEntry::getEntryFee).orElse("minecraft:diamond");

        String fullDimensionId = validation.config().getDimensionId();
        PlayerTimerData timerData = TimerManager.getOrCreateData(player, fullDimensionId);
        boolean alreadyPaid = timerData.hasPaidEntryFeeToday();

        ModNetworking.sendOpenTpAccept(player, dimensionName, dimensionId,
                hasFee, isCobbledollarFee, feeAmount, feeItem, "dungeon", alreadyPaid);

        pendingConfirmations.put(playerId, new PendingDungeonEntry(
                portalEntity.getBlockPos(),
                player.level().dimension(),
                dungeonId
        ));

        CobbleSafari.LOGGER.debug("Opened TpAccept for player {} (dungeon: {})",
                player.getName().getString(), dungeonId);
    }

    public static void handleAcceptResponse(ServerPlayer player, boolean accepted) {
        UUID playerId = player.getUUID();

        PendingDungeonEntry pending = pendingConfirmations.remove(playerId);
        if (pending == null) {
            return;
        }

        if (!accepted) {
            CobbleSafari.LOGGER.debug("Player {} declined dungeon teleport", player.getName().getString());
            return;
        }

        DungeonPortalBlockEntity portalEntity = resolvePortal(player, pending);
        if (portalEntity == null) {
            return;
        }

        CobbleSafari.LOGGER.info("Player {} accepted dungeon teleport to {}, initiating teleport sequence",
                player.getName().getString(), pending.dungeonDimensionId());
        processDungeonTeleport(player, portalEntity, pending);
    }

    private static DungeonPortalBlockEntity resolvePortal(ServerPlayer player, PendingDungeonEntry pending) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            ModNetworking.sendCloseTpAccept(player);
            return null;
        }

        ServerLevel portalLevel = server.getLevel(pending.portalDimension());
        if (portalLevel == null) {
            ModNetworking.sendCloseTpAccept(player);
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.error.no_dimension"));
            return null;
        }

        BlockEntity be = portalLevel.getBlockEntity(pending.portalPos());
        if (!(be instanceof DungeonPortalBlockEntity portalEntity)) {
            ModNetworking.sendCloseTpAccept(player);
            player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.error.portal_gone"));
            return null;
        }

        return portalEntity;
    }

    private static void processDungeonTeleport(ServerPlayer player, DungeonPortalBlockEntity portalEntity,
                                                PendingDungeonEntry pending) {
        Optional<DungeonDimensionEntry> dimConfig = PortalSpawnConfig.getDimensionConfig(pending.dungeonDimensionId());
        boolean enableFee = dimConfig.map(DungeonDimensionEntry::isEntryFeeEnabled).orElse(false);
        String feeItem = dimConfig.map(DungeonDimensionEntry::getEntryFee).orElse("minecraft:diamond");
        boolean useCobbledollar = dimConfig.map(DungeonDimensionEntry::isCobbledollarEntryFee).orElse(false);
        int feeAmount = dimConfig.map(DungeonDimensionEntry::getEntryFeeAmount).orElse(5000);

        DungeonTeleportHandler.DungeonValidationResult validation =
                DungeonTeleportHandler.validateDungeonEntry(player, portalEntity);
        if (validation == null) {
            ModNetworking.sendCloseTpAccept(player);
            return;
        }

        String fullDimensionId = validation.config().getDimensionId();
        PlayerTimerData timerData = TimerManager.getOrCreateData(player, fullDimensionId);

        if (!timerData.hasPaidEntryFeeToday()) {
            boolean charged = EntryFeeHelper.tryChargeFee(player, enableFee, feeItem, useCobbledollar, feeAmount);
            if (!charged) {
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.entry.fee.insufficient"));
                ModNetworking.sendCloseTpAccept(player);
                return;
            }
            timerData.markEntryFeePaidToday();
            TimerManager.savePlayerData(player, timerData);
        }

        executeTeleportOrCountdown(player, portalEntity, validation,
                new FeeContext(enableFee, useCobbledollar, feeItem, feeAmount), timerData);
    }

    private record FeeContext(boolean enableFee, boolean useCobbledollar, String feeItem, int feeAmount) {}

    private static void executeTeleportOrCountdown(ServerPlayer player, DungeonPortalBlockEntity portalEntity,
                                                    DungeonTeleportHandler.DungeonValidationResult validation,
                                                    FeeContext fee, PlayerTimerData timerData) {
        if (validation.isReEntry() || portalEntity.getDungeonStructurePos() != null) {
            DungeonTeleportHandler.DungeonPrepResult prepResult =
                    DungeonTeleportHandler.buildPrepResultFromPortal(player, portalEntity, validation);
            if (prepResult != null) {
                DungeonTeleportHandler.executeDungeonTeleport(player, portalEntity, prepResult);
            } else {
                ModNetworking.sendCloseTpAccept(player);
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.error.structure_failed"));
            }
        } else {
            boolean started = DungeonTeleportCountdown.startTeleportSequence(player, portalEntity);
            if (!started) {
                ModNetworking.sendCloseTpAccept(player);
                if (fee.enableFee() && !timerData.hasPaidEntryFeeToday()) {
                    EntryFeeHelper.FeeType feeType = EntryFeeHelper.getEffectiveFeeType(fee.enableFee(), fee.useCobbledollar());
                    refundFee(player, feeType, fee.feeItem(), fee.feeAmount());
                }
            }
        }
    }

    private static void refundFee(ServerPlayer player, EntryFeeHelper.FeeType feeType, String feeItem, int feeAmount) {
        switch (feeType) {
            case COBBLEDOLLAR:
                maxigregrze.cobblesafari.compat.CobbleDollarHelper.give(player, feeAmount);
                CobbleSafari.LOGGER.info("Refunded {} CobbleDollars to player {} (dungeon teleport failed)",
                        feeAmount, player.getName().getString());
                break;
            case ITEM:
                net.minecraft.world.item.Item item = EntryFeeHelper.resolveItem(feeItem);
                if (item != net.minecraft.world.item.Items.AIR) {
                    net.minecraft.world.item.ItemStack refundStack = new net.minecraft.world.item.ItemStack(item, 1);
                    if (!player.getInventory().add(refundStack)) {
                        player.drop(refundStack, false);
                    }
                    CobbleSafari.LOGGER.info("Refunded 1x {} to player {} (dungeon teleport failed)",
                            feeItem, player.getName().getString());
                }
                break;
            default:
                break;
        }
    }

    public static boolean hasPendingConfirmation(UUID playerId) {
        return pendingConfirmations.containsKey(playerId);
    }

    public static void cancelPending(UUID playerId) {
        pendingConfirmations.remove(playerId);
    }
}
