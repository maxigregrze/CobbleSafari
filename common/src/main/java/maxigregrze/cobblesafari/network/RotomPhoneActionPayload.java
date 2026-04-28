package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RotomPhoneActionPayload(
        int actionType,
        String data
) implements CustomPacketPayload {

    public static final int ACTION_CHANGE_SKIN = 0;
    public static final int ACTION_TOGGLE_SAFETY = 1;
    public static final int ACTION_OPEN_PC = 2;
    public static final int ACTION_CLOSE = 3;

    public static final CustomPacketPayload.Type<RotomPhoneActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "rotom_phone_action"));

    public static final StreamCodec<FriendlyByteBuf, RotomPhoneActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.actionType);
                buf.writeUtf(payload.data);
            },
            buf -> new RotomPhoneActionPayload(
                    buf.readInt(),
                    buf.readUtf()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
