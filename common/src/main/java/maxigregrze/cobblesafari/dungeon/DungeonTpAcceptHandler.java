package maxigregrze.cobblesafari.dungeon;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.api.DungeonRegistrationAPI;
import maxigregrze.cobblesafari.api.PortalInterceptor;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.data.TimerScope;
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

    // A prompt closed without a reply (screen lost, packet dropped, modified client) must not lock the
    // player out of every portal: past this age it is replaced by the next portal click and refused on accept.
    private static final long PENDING_TIMEOUT_MS = 60_000L;
    // Accepting is only valid while still standing near the portal that opened the prompt.
    private static final double MAX_ACCEPT_DISTANCE_SQ = 16.0 * 16.0;

    private DungeonTpAcceptHandler() {}

    private record PendingDungeonEntry(
            BlockPos portalPos,
            ResourceKey<Level> portalDimension,
            String dungeonDimensionId,
            long createdAtMs
    ) {
        boolean isExpired() {
            return System.currentTimeMillis() - createdAtMs > PENDING_TIMEOUT_MS;
        }
    }

    public static void openTpAcceptForDungeon(ServerPlayer player, DungeonPortalBlockEntity portalEntity) {
        UUID playerId = player.getUUID();

        PendingDungeonEntry existing = pendingConfirmations.get(playerId);
        if (existing != null) {
            if (!existing.isExpired()) {
                return;
            }
            pendingConfirmations.remove(playerId);
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
                    new PendingDungeonEntry(portalEntity.getBlockPos(), player.level().dimension(), dungeonId,
                            System.currentTimeMillis()));
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

        // "Already paid" now means "is resuming a visit that still has time on it" (plan 181
        // § 6.1). The payload is unchanged; only what fills the flag differs. An exhausted session
        // is NOT already paid - re-entering it is a re-purchase and must show the fee.
        boolean alreadyPaid = entryIsFree(player, portalEntity, validation);

        ModNetworking.sendOpenTpAccept(player, dimensionName, dimensionId,
                hasFee, isCobbledollarFee, feeAmount, feeItem, "dungeon", alreadyPaid);

        pendingConfirmations.put(playerId, new PendingDungeonEntry(
                portalEntity.getBlockPos(),
                player.level().dimension(),
                dungeonId,
                System.currentTimeMillis()
        ));

        CobbleSafari.LOGGER.debug("Opened TpAccept for player {} (dungeon: {})",
                player.getName().getString(), dungeonId);
    }

    public static void handleAcceptResponse(ServerPlayer player, boolean accepted) {
        UUID playerId = player.getUUID();

        if (maxigregrze.cobblesafari.effect.RedShackledEffects.isShackled(player)) {
            pendingConfirmations.remove(playerId);
            return;
        }

        PendingDungeonEntry pending = pendingConfirmations.remove(playerId);
        if (pending == null) {
            return;
        }

        if (!accepted) {
            CobbleSafari.LOGGER.debug("Player {} declined dungeon teleport", player.getName().getString());
            return;
        }

        if (pending.isExpired()
                || player.level().dimension() != pending.portalDimension()
                || player.blockPosition().distSqr(pending.portalPos()) > MAX_ACCEPT_DISTANCE_SQ) {
            ModNetworking.sendCloseTpAccept(player);
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

        // The session is the receipt (plan 181 § 6.1): charge unless the player is resuming a visit
        // that still has time on it. On a first entry no instance exists yet, so there can be no
        // session; on a re-purchase the session exists but is spent. Both are charged.
        ChargedFee charged = null;
        if (!entryIsFree(player, portalEntity, validation)) {
            boolean ok = EntryFeeHelper.tryChargeFee(player, enableFee, feeItem, useCobbledollar, feeAmount);
            if (!ok) {
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.entry.fee.insufficient"));
                ModNetworking.sendCloseTpAccept(player);
                return;
            }
            EntryFeeHelper.FeeType feeType = EntryFeeHelper.getEffectiveFeeType(enableFee, useCobbledollar);
            if (feeType != EntryFeeHelper.FeeType.NONE) {
                charged = new ChargedFee(feeType, feeItem, feeAmount);
            }
            // Paying is what makes a new run, so it is also what gives the wormhole back. The
            // once-per-run entry lives on the instance record and used to reset only when the
            // instance itself did - which meant a player who let their time run out and bought a
            // second entry into the SAME instance was still refused (action plan 183, B7).
            //
            // Hooked to the charge and not to the entry: walking out through the exit portal with
            // credit left and walking back in is free, takes this branch, and must not hand out a
            // second wormhole.
            resetWormholeEntryForNewRun(player, portalEntity, validation);
        }

        executeTeleportOrCountdown(player, portalEntity, validation, charged);
    }

    /**
     * The player's session for the instance this portal admits to, or {@code null}. Resolved
     * <em>by portal</em>, since at this point the player is still standing in the overworld.
     */
    private static PlayerTimerData sessionForPortal(ServerPlayer player, DungeonPortalBlockEntity portalEntity,
                                                    DungeonTeleportHandler.DungeonValidationResult validation) {
        UUID instanceId = validation.instanceId() != null
                ? validation.instanceId()
                : DungeonTeleportHandler.resolveInstanceForPortal(
                        player.getServer(), portalEntity, validation.config().getDimensionId());
        if (instanceId == null) {
            return null;
        }
        return TimerManager.session(player,
                TimerScope.of(validation.config().getDimensionId(), instanceId));
    }

    /**
     * Whether this entry is free, i.e. the player is resuming a visit they still have time on.
     * <p>
     * Holding a session is <b>not</b> sufficient: a session at zero credit is a spent visit, and
     * re-entering it is the re-purchase of § 6.2 - which must be charged like a first entry. Reading
     * this as "holds a session" collapses that row of the table and lets an exhausted player back in
     * for free, with a screen that wrongly says they already paid.
     * <p>
     * When the credit is zero and {@code allowMultiplePayment} is off, we never get here:
     * {@code validateDungeonEntry} has already refused with {@code no_time_remaining}.
     */
    private static boolean entryIsFree(ServerPlayer player, DungeonPortalBlockEntity portalEntity,
                                       DungeonTeleportHandler.DungeonValidationResult validation) {
        PlayerTimerData session = sessionForPortal(player, portalEntity, validation);
        return session != null && session.getRemainingTicks() > 0;
    }

    /**
     * Clears this player's once-per-run wormhole entry on the instance they are paying to enter.
     * No-op on a first entry, where no instance record exists yet - and none can have been spent.
     */
    private static void resetWormholeEntryForNewRun(ServerPlayer player, DungeonPortalBlockEntity portalEntity,
                                                    DungeonTeleportHandler.DungeonValidationResult validation) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID instanceId = validation.instanceId() != null
                ? validation.instanceId()
                : DungeonTeleportHandler.resolveInstanceForPortal(
                        server, portalEntity, validation.config().getDimensionId());
        if (instanceId == null) {
            return;
        }
        maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager.clearEntriesForNewRun(
                server, instanceId, player.getUUID());
    }

    /** A fee actually taken from the player, carried so every abandon path can hand it back. */
    public record ChargedFee(EntryFeeHelper.FeeType feeType, String feeItem, int feeAmount) {}

    private static void executeTeleportOrCountdown(ServerPlayer player, DungeonPortalBlockEntity portalEntity,
                                                    DungeonTeleportHandler.DungeonValidationResult validation,
                                                    ChargedFee charged) {
        if (validation.isReEntry() || portalEntity.getDungeonStructurePos() != null) {
            DungeonTeleportHandler.DungeonPrepResult prepResult =
                    DungeonTeleportHandler.buildPrepResultFromPortal(player, portalEntity, validation);
            if (prepResult != null && prepResult.instanceId() != null) {
                // Even here the teleport can abort (player removed, chunk-tracking NPE), and the fee
                // was taken before we got here - so the outcome decides whether it is owed back.
                if (!DungeonTeleportHandler.executeDungeonTeleport(player, portalEntity, prepResult)) {
                    refundCharged(player, charged);
                }
            } else {
                ModNetworking.sendCloseTpAccept(player);
                player.sendSystemMessage(Component.translatable("cobblesafari.dungeon.error.structure_failed"));
                // The fee was taken before this branch could fail, so it is owed back (D1).
                refundCharged(player, charged);
            }
        } else {
            boolean started = DungeonTeleportCountdown.startTeleportSequence(player, portalEntity, charged);
            if (!started) {
                ModNetworking.sendCloseTpAccept(player);
                refundCharged(player, charged);
            }
        }
    }

    /** The single refund entry point; every abandon path funnels here (plan 181 § 11.2). */
    public static void refundCharged(ServerPlayer player, ChargedFee charged) {
        if (charged == null) {
            return;
        }
        refundFee(player, charged.feeType(), charged.feeItem(), charged.feeAmount());
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
