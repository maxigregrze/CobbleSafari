package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EmptyPhoneConfirmPayload(boolean confirmed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EmptyPhoneConfirmPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "empty_phone_confirm"));

    public static final StreamCodec<FriendlyByteBuf, EmptyPhoneConfirmPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            EmptyPhoneConfirmPayload::confirmed,
            EmptyPhoneConfirmPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
