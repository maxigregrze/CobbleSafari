package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GtsAppPayload(
        int actionType,
        int intArg1,
        int intArg2,
        String stringArg1,
        String stringArg2,
        String stringArg3
) implements CustomPacketPayload {

    private static final int MAX_STRING_LEN = 96;

    public static final int ACTION_REQUEST_STATE = 0;
    public static final int ACTION_VALIDATE_SPECIES = 1;
    public static final int ACTION_DEPOSIT = 2;
    public static final int ACTION_RETRIEVE = 3;
    public static final int ACTION_CLAIM = 4;
    public static final int ACTION_SEARCH = 5;
    public static final int ACTION_START_TRADE = 6;
    public static final int ACTION_CONFIRM_TRADE = 7;
    public static final int ACTION_ABORT_TRADE = 8;

    public static final CustomPacketPayload.Type<GtsAppPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "gts_app_action"));

    public static final StreamCodec<FriendlyByteBuf, GtsAppPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.actionType());
                buf.writeVarInt(p.intArg1());
                buf.writeVarInt(p.intArg2());
                buf.writeUtf(p.stringArg1() == null ? "" : p.stringArg1(), MAX_STRING_LEN);
                buf.writeUtf(p.stringArg2() == null ? "" : p.stringArg2(), MAX_STRING_LEN);
                buf.writeUtf(p.stringArg3() == null ? "" : p.stringArg3(), MAX_STRING_LEN);
            },
            buf -> new GtsAppPayload(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readUtf(MAX_STRING_LEN),
                    buf.readUtf(MAX_STRING_LEN),
                    buf.readUtf(MAX_STRING_LEN)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
