package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenRotomPhoneRequestPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenRotomPhoneRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_rotom_phone_request"));

    public static final StreamCodec<FriendlyByteBuf, OpenRotomPhoneRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> new OpenRotomPhoneRequestPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
