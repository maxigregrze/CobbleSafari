package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.dungeon.hyperspace.obstacle.ShardClusterSettings;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * S2C: the server's {@code enableShardsClusters} (action plan 196, section 8.9). The client needs it
 * to hide the clusters and drop their shapes, or its movement would collide with what it cannot see.
 */
public record ObstacleConfigSyncPayload(boolean shardClusters) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ObstacleConfigSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "obstacle_config_sync"));

    public static final StreamCodec<FriendlyByteBuf, ObstacleConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.shardClusters()),
            buf -> new ObstacleConfigSyncPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void syncToPlayer(ServerPlayer player) {
        Services.PLATFORM.sendPayloadToPlayer(player, new ObstacleConfigSyncPayload(MiscConfig.isShardsClustersEnabled()));
    }

    /** Client side. */
    public void apply() {
        ShardClusterSettings.setClientEnabled(shardClusters);
    }
}
