package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CloseTpAcceptPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CloseTpAcceptPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "close_tp_accept"));

    public static final StreamCodec<FriendlyByteBuf, CloseTpAcceptPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> new CloseTpAcceptPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
