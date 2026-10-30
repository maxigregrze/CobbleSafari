package maxigregrze.cobblesafari.client.obstacle;

import maxigregrze.cobblesafari.dungeon.hyperspace.obstacle.ShardClusterSettings;
import net.minecraft.client.Minecraft;

/**
 * Shows or hides the shard clusters for the local player (action plan 196, section 8.9): hidden while
 * {@code enableShardsClusters} is off, unless the player is in creative. The block's render shape reads
 * the flag, so a change re-meshes the loaded chunks - rare: login, sync, game mode switch.
 */
public final class ShardClusterClient {

    private ShardClusterClient() {}

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        boolean hidden = !ShardClusterSettings.clientEnabled() && !mc.player.isCreative();
        if (hidden != ShardClusterSettings.hiddenOnClient()) {
            ShardClusterSettings.setHiddenOnClient(hidden);
            mc.levelRenderer.allChanged();
        }
    }
}
