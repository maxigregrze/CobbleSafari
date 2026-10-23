package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.item.ultraloot.ReconArmorLogic;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * S2C: the server's {@code enableUltraReconElytra} (action plan 192, section 10.5). The client
 * decides alone whether to start gliding and words the armor tooltip from it.
 */
public record UltraLootConfigSyncPayload(boolean reconElytra) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UltraLootConfigSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "ultra_loot_config_sync"));

    public static final StreamCodec<FriendlyByteBuf, UltraLootConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.reconElytra()),
            buf -> new UltraLootConfigSyncPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static UltraLootConfigSyncPayload current() {
        return new UltraLootConfigSyncPayload(MiscConfig.isUltraReconElytraEnabled());
    }

    public static void syncToPlayer(ServerPlayer player) {
        Services.PLATFORM.sendPayloadToPlayer(player, current());
    }

    public static void syncToAll(MinecraftServer server) {
        UltraLootConfigSyncPayload payload = current();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Services.PLATFORM.sendPayloadToPlayer(player, payload);
        }
    }

    /** Client side. */
    public void apply() {
        ReconArmorLogic.setClientElytraEnabled(reconElytra);
    }
}
