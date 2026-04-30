package maxigregrze.cobblesafari.event;

import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.manager.TimerManager;
import net.minecraft.server.level.ServerPlayer;

public final class DimensionTimerDeathHandler {

    private DimensionTimerDeathHandler() {}

    public static boolean allowVanillaDeath(ServerPlayer player) {
        if (player.level().isClientSide()) {
            return true;
        }
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        String dimId = player.level().dimension().location().toString();
        if (!SafariTimerConfig.hasDimensionTimer(dimId)) {
            return true;
        }
        TimerManager.onDeathWhileTimed(player, dimId);
        return false;
    }
}
