package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenEmptyPhoneConfirmPayload(
        String rotomName,
        int rotomLevel,
        boolean rotomIsShiny,
        boolean isBlock
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenEmptyPhoneConfirmPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_empty_phone_confirm"));

    public static final StreamCodec<FriendlyByteBuf, OpenEmptyPhoneConfirmPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.rotomName);
                buf.writeInt(payload.rotomLevel);
                buf.writeBoolean(payload.rotomIsShiny);
                buf.writeBoolean(payload.isBlock);
            },
            buf -> new OpenEmptyPhoneConfirmPayload(
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
