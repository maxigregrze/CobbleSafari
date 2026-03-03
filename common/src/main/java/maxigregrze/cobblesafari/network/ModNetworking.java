package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.server.level.ServerPlayer;

public class ModNetworking {

    public static void sendTimerSync(ServerPlayer player, String dimensionId, int remainingTicks, boolean active, boolean bypassed) {
        Services.PLATFORM.sendPayloadToPlayer(player, new TimerSyncPayload(dimensionId, remainingTicks, active, bypassed));
    }

    public static void sendTimerSync(ServerPlayer player, String dimensionId, int remainingTicks, boolean active) {
        sendTimerSync(player, dimensionId, remainingTicks, active, false);
    }

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
}
