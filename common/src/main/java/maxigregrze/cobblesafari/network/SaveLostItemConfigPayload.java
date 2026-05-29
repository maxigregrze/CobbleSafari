package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveLostItemConfigPayload(
        BlockPos pos,
        int mode,
        String poolBerryId,
        String poolCandyId,
        String poolBallsId,
        String poolTreasuresId,
        String minRollStr,
        String maxRollStr,
        String lostItemLootTableId,
        String lootItemId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SaveLostItemConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "save_lost_item_config"));

    private static final int MAX_STR = 512;

    public static final StreamCodec<FriendlyByteBuf, SaveLostItemConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeInt(p.mode);
                buf.writeUtf(p.poolBerryId, MAX_STR);
                buf.writeUtf(p.poolCandyId, MAX_STR);
                buf.writeUtf(p.poolBallsId, MAX_STR);
                buf.writeUtf(p.poolTreasuresId, MAX_STR);
                buf.writeUtf(p.minRollStr, 32);
                buf.writeUtf(p.maxRollStr, 32);
                buf.writeUtf(p.lostItemLootTableId, MAX_STR);
                buf.writeUtf(p.lootItemId, MAX_STR);
            },
            buf -> new SaveLostItemConfigPayload(
                    buf.readBlockPos(),
                    buf.readInt(),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(32),
                    buf.readUtf(32),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
