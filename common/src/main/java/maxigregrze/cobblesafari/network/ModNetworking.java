package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class ModNetworking {

    private ModNetworking() {
        // Utility class; not meant to be instantiated.
    }

    public static void sendTimerSync(ServerPlayer player, String dimensionId, int remainingTicks, boolean active, boolean bypassed) {
        Services.PLATFORM.sendPayloadToPlayer(player, new TimerSyncPayload(dimensionId, remainingTicks, active, bypassed));
    }

    public static void sendTimerSync(ServerPlayer player, String dimensionId, int remainingTicks, boolean active) {
        sendTimerSync(player, dimensionId, remainingTicks, active, false);
    }

    /** @deprecated use {@link #sendTimerSync(ServerPlayer, String, int, boolean)} with an explicit dimension id. */
    @Deprecated
    public static void sendTimerSync(ServerPlayer player, int remainingTicks, boolean active) {
        sendTimerSync(player, SafariTimerConfig.getSafariDimensionId(), remainingTicks, active, false);
    }

    public static void sendOpenTpAccept(ServerPlayer player, String dimensionName, String dimensionId,
                                        boolean hasEntryFee, boolean isCobbledollarFee,
                                        int entryFeeAmount, String entryFeeItem, String source,
                                        boolean alreadyPaidToday) {
        Services.PLATFORM.sendPayloadToPlayer(player,
                new OpenTpAcceptPayload(dimensionName, dimensionId, hasEntryFee,
                        isCobbledollarFee, entryFeeAmount, entryFeeItem, source, alreadyPaidToday));
    }

    public static void sendCloseTpAccept(ServerPlayer player) {
        Services.PLATFORM.sendPayloadToPlayer(player, new CloseTpAcceptPayload());
    }

    public static void sendCloseEmptyPhoneConfirm(ServerPlayer player) {
        Services.PLATFORM.sendPayloadToPlayer(player, new CloseEmptyPhoneConfirmPayload());
    }

    /** Broadcasts a shackle state change to every player tracking the entity (never to itself). */
    public static void sendShackleSync(LivingEntity entity, boolean shackled) {
        Services.PLATFORM.sendPayloadToTrackingPlayers(entity, new ShackleSyncPayload(entity.getId(), shackled));
    }

    /** Hands the current shackle state of an entity to a single viewer. */
    public static void sendShackleSync(ServerPlayer viewer, LivingEntity entity, boolean shackled) {
        Services.PLATFORM.sendPayloadToPlayer(viewer, new ShackleSyncPayload(entity.getId(), shackled));
    }
}
