package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C signal for connected clients to reload {@code hud_config.json} after {@code /cobblesafari refresh}. */
public record HudConfigRefreshPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HudConfigRefreshPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "hud_config_refresh"));

    public static final StreamCodec<FriendlyByteBuf, HudConfigRefreshPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> new HudConfigRefreshPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
