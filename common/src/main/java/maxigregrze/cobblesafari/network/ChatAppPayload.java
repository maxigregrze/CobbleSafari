package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → server actions for the Rotom Phone chat app.
 */
public record ChatAppPayload(int actionType, String convId, int intArg) implements CustomPacketPayload {

    public static final int ACTION_REQUEST_CONTACTS = 0; // convId="", intArg=0
    public static final int ACTION_OPEN = 1; // convId
    public static final int ACTION_ADVANCE_MESSAGE = 2; // convId, intArg=newIndex
    public static final int ACTION_POLL_TASK = 3; // convId
    public static final int ACTION_CLAIM = 4; // convId
    public static final int ACTION_AFTER_DONE = 5; // convId

    public static final CustomPacketPayload.Type<ChatAppPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "chat_app"));

    public static final StreamCodec<FriendlyByteBuf, ChatAppPayload> STREAM_CODEC =
            StreamCodec.of(ChatAppPayload::write, ChatAppPayload::read);

    private static void write(FriendlyByteBuf buf, ChatAppPayload p) {
        buf.writeVarInt(p.actionType);
        buf.writeUtf(p.convId == null ? "" : p.convId);
        buf.writeVarInt(p.intArg);
    }

    private static ChatAppPayload read(FriendlyByteBuf buf) {
        int action = buf.readVarInt();
        String convId = buf.readUtf();
        int intArg = buf.readVarInt();
        return new ChatAppPayload(action, convId, intArg);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
