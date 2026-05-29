package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WonderAppPayload(int actionType, int slot) implements CustomPacketPayload {

    public static final int ACTION_REQUEST_STATE = 0;
    public static final int ACTION_TRADE = 1;

    public static final CustomPacketPayload.Type<WonderAppPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "wonder_app_action"));

    public static final StreamCodec<FriendlyByteBuf, WonderAppPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.actionType());
                buf.writeVarInt(p.slot());
            },
            buf -> new WonderAppPayload(buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
