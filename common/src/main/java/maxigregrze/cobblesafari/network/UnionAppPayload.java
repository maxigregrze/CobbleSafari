package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnionAppPayload(int actionType, int[] code) implements CustomPacketPayload {

    public static final int ACTION_REQUEST_STATE = 0;
    public static final int ACTION_CREATE = 1;
    public static final int ACTION_JOIN = 2;
    public static final int ACTION_CLOSE = 3;

    public static final CustomPacketPayload.Type<UnionAppPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "union_app_action"));

    public static final StreamCodec<FriendlyByteBuf, UnionAppPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.actionType());
                int[] c = p.code();
                buf.writeVarInt(c == null ? 0 : c.length);
                if (c != null) {
                    for (int d : c) {
                        buf.writeVarInt(d);
                    }
                }
            },
            buf -> {
                int action = buf.readVarInt();
                int len = buf.readVarInt();
                int[] arr = new int[len];
                for (int i = 0; i < len; i++) {
                    arr[i] = buf.readVarInt();
                }
                return new UnionAppPayload(action, arr);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
