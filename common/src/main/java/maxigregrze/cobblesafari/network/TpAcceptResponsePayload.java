package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TpAcceptResponsePayload(boolean accepted, String source) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TpAcceptResponsePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "tp_accept_response"));

    public static final StreamCodec<FriendlyByteBuf, TpAcceptResponsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            TpAcceptResponsePayload::accepted,
            ByteBufCodecs.STRING_UTF8,
            TpAcceptResponsePayload::source,
            TpAcceptResponsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
