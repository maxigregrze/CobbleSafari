package maxigregrze.cobblesafari.event;

import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.TimerScope;
import maxigregrze.cobblesafari.manager.TimerManager;
import net.minecraft.server.level.ServerPlayer;

public final class DimensionTimerDeathHandler {

    private DimensionTimerDeathHandler() {}

    /**
     * Whether a death of {@code player} right now would be cancelled by the timer rescue. Side
     * effect free: the totem / Rainbow Feather check uses it so the rescue wins on both loaders
     * (Fabric asks before the totem check, NeoForge after it).
     */
    public static boolean wouldRescue(ServerPlayer player) {
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) {
            return false;
        }
        return SafariTimerConfig.hasDimensionTimer(player.level().dimension().location().toString());
    }

    public static boolean allowVanillaDeath(ServerPlayer player) {
        if (!wouldRescue(player)) {
            return true;
        }
        String dimId = player.level().dimension().location().toString();
        // Resolve the scope before draining it: on an instanced dimension the rescue must drain the
        // session of the instance the player died in, and leave that session alive at zero credit so
        // an operator can credit it and the player can walk back in (plan 181 § 9.1).
        TimerScope scope = TimerManager.currentScope(player);
        if (scope == null) {
            scope = TimerScope.of(dimId);
        }
        TimerManager.onDeathWhileTimed(player, scope);
        return false;
    }
}
