package maxigregrze.cobblesafari.dungeon.hyperspace.obstacle;

import maxigregrze.cobblesafari.config.MiscConfig;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

/**
 * {@code enableShardsClusters} on both sides (action plan 196, § 8.9). The server reads its config; a
 * client reads the value the server synced at login, never its own local config file.
 */
public final class ShardClusterSettings {

    private static volatile boolean clientEnabled;
    /** Client only: shard clusters are currently drawn invisible for the local player. */
    private static volatile boolean hiddenOnClient;

    private ShardClusterSettings() {}

    public static boolean enabled(BlockGetter level) {
        if (level instanceof Level l && l.isClientSide()) {
            return clientEnabled;
        }
        return MiscConfig.isShardsClustersEnabled();
    }

    public static boolean clientEnabled() {
        return clientEnabled;
    }

    public static void setClientEnabled(boolean enabled) {
        clientEnabled = enabled;
    }

    public static boolean hiddenOnClient() {
        return hiddenOnClient;
    }

    public static void setHiddenOnClient(boolean hidden) {
        hiddenOnClient = hidden;
    }
}
