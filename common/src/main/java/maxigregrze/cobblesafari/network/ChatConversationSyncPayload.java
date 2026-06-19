package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client sync of chat conversation definitions (contact list only) plus the per-player
 * unlocked flag. Sent at join and after a datapack reload.
 */
public record ChatConversationSyncPayload(List<Entry> conversations) implements CustomPacketPayload {

    public record Entry(String id, String displayName, int priority, String textureFile, boolean unlocked) {}

    public static final CustomPacketPayload.Type<ChatConversationSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "chat_conversation_sync"));

    public static final StreamCodec<FriendlyByteBuf, ChatConversationSyncPayload> STREAM_CODEC =
            StreamCodec.of(ChatConversationSyncPayload::write, ChatConversationSyncPayload::read);

    private static void write(FriendlyByteBuf buf, ChatConversationSyncPayload p) {
        buf.writeVarInt(p.conversations.size());
        for (Entry e : p.conversations) {
            buf.writeUtf(e.id());
            buf.writeUtf(e.displayName());
            buf.writeVarInt(e.priority());
            buf.writeUtf(e.textureFile());
            buf.writeBoolean(e.unlocked());
        }
    }

    private static ChatConversationSyncPayload read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf();
            String name = buf.readUtf();
            int priority = buf.readVarInt();
            String tex = buf.readUtf();
            boolean unlocked = buf.readBoolean();
            list.add(new Entry(id, name, priority, tex, unlocked));
        }
        return new ChatConversationSyncPayload(list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
