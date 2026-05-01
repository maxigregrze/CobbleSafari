package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RotomPhoneRotoGlideRequestPayload(double horizontalMoveX, double horizontalMoveZ) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RotomPhoneRotoGlideRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "rotom_phone_roto_glide_request"));

    public static final StreamCodec<FriendlyByteBuf, RotomPhoneRotoGlideRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeDouble(payload.horizontalMoveX());
                buf.writeDouble(payload.horizontalMoveZ());
            },
            buf -> new RotomPhoneRotoGlideRequestPayload(buf.readDouble(), buf.readDouble())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
