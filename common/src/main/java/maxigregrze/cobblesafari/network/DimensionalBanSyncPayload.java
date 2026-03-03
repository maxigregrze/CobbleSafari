package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.DimensionalBanData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public record DimensionalBanSyncPayload(
        Map<String, DimensionalBanData.DimensionRestrictions> dimensions
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DimensionalBanSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dimensional_ban_sync"));

    public static final StreamCodec<FriendlyByteBuf, DimensionalBanSyncPayload> STREAM_CODEC =
            StreamCodec.of(DimensionalBanSyncPayload::encode, DimensionalBanSyncPayload::decode);

    private static void encode(FriendlyByteBuf buf, DimensionalBanSyncPayload payload) {
        buf.writeVarInt(payload.dimensions.size());
        for (var entry : payload.dimensions.entrySet()) {
            buf.writeUtf(entry.getKey());
            var r = entry.getValue();

            buf.writeVarInt(r.bannedItems.size());
            for (String item : r.bannedItems) {
                buf.writeUtf(item);
            }

            buf.writeVarInt(r.bannedBlocks.size());
            for (String block : r.bannedBlocks) {
                buf.writeUtf(block);
            }

            buf.writeBoolean(r.allowBattle);
            buf.writeBoolean(r.allowBlockBreaking);
            buf.writeBoolean(r.allowBlockPlacing);
        }
    }

    private static DimensionalBanSyncPayload decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, DimensionalBanData.DimensionRestrictions> dimensions = new HashMap<>();

        for (int i = 0; i < size; i++) {
            String dimId = buf.readUtf();
            var r = new DimensionalBanData.DimensionRestrictions();

            int itemCount = buf.readVarInt();
            r.bannedItems = new ArrayList<>(itemCount);
            for (int j = 0; j < itemCount; j++) {
                r.bannedItems.add(buf.readUtf());
            }

            int blockCount = buf.readVarInt();
            r.bannedBlocks = new ArrayList<>(blockCount);
            for (int j = 0; j < blockCount; j++) {
                r.bannedBlocks.add(buf.readUtf());
            }

            r.allowBattle = buf.readBoolean();
            r.allowBlockBreaking = buf.readBoolean();
            r.allowBlockPlacing = buf.readBoolean();
            dimensions.put(dimId, r);
        }

        return new DimensionalBanSyncPayload(dimensions);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
