package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C snapshot of the player's current dimensional objectives for the HUD (plan 118 §9.7).
 * {@code visible == false} hides the HUD (player not in an objectives dimension/instance).
 */
public record ObjectivesHudSyncPayload(boolean visible, List<ObjectiveView> objectives)
        implements CustomPacketPayload {

    public record ObjectiveView(String taskId, int targetCount, int progress, boolean complete,
                                String speciesId, int typeIndex) {}

    public static final CustomPacketPayload.Type<ObjectivesHudSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "objectives_hud_sync"));

    public static final StreamCodec<FriendlyByteBuf, ObjectivesHudSyncPayload> STREAM_CODEC =
            StreamCodec.of(ObjectivesHudSyncPayload::encode, ObjectivesHudSyncPayload::decode);

    public static ObjectivesHudSyncPayload hidden() {
        return new ObjectivesHudSyncPayload(false, List.of());
    }

    private static void encode(FriendlyByteBuf buf, ObjectivesHudSyncPayload payload) {
        buf.writeBoolean(payload.visible);
        buf.writeVarInt(payload.objectives.size());
        for (ObjectiveView v : payload.objectives) {
            buf.writeUtf(v.taskId());
            buf.writeVarInt(v.targetCount());
            buf.writeVarInt(v.progress());
            buf.writeBoolean(v.complete());
            buf.writeUtf(v.speciesId());
            buf.writeVarInt(v.typeIndex() + 1); // shift so -1 stays non-negative
        }
    }

    private static ObjectivesHudSyncPayload decode(FriendlyByteBuf buf) {
        boolean visible = buf.readBoolean();
        int size = buf.readVarInt();
        List<ObjectiveView> objectives = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String taskId = buf.readUtf();
            int target = buf.readVarInt();
            int progress = buf.readVarInt();
            boolean complete = buf.readBoolean();
            String species = buf.readUtf();
            int typeIndex = buf.readVarInt() - 1;
            objectives.add(new ObjectiveView(taskId, target, progress, complete, species, typeIndex));
        }
        return new ObjectivesHudSyncPayload(visible, objectives);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
