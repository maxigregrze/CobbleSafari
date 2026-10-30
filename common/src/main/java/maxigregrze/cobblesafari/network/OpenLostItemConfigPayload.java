package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenLostItemConfigPayload(
        BlockPos pos,
        String poolBerryId,
        String poolCandyId,
        String poolBallsId,
        String poolTreasuresId,
        int minRoll,
        int maxRoll,
        String lostItemLootTableId,
        String lootItemId,
        int mode,
        String skin,
        String pokeball,
        int lootItemCount,
        String lootItemComponents,
        LootDefaults defaults,
        String defaultLootTableId,
        String defaultLootItemId,
        int defaultMode
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenLostItemConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_lost_item_config"));

    private static final int MAX_STR = 512;
    private static final int MAX_SKIN = 32;
    /** FriendlyByteBuf's default string limit: a written book or a heavily enchanted stack still fits. */
    private static final int MAX_DATA = 32767;

    public static final StreamCodec<FriendlyByteBuf, OpenLostItemConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeUtf(p.poolBerryId, MAX_STR);
                buf.writeUtf(p.poolCandyId, MAX_STR);
                buf.writeUtf(p.poolBallsId, MAX_STR);
                buf.writeUtf(p.poolTreasuresId, MAX_STR);
                buf.writeInt(p.minRoll);
                buf.writeInt(p.maxRoll);
                buf.writeUtf(p.lostItemLootTableId, MAX_STR);
                buf.writeUtf(p.lootItemId, MAX_STR);
                buf.writeInt(p.mode);
                buf.writeUtf(p.skin, MAX_SKIN);
                buf.writeUtf(p.pokeball, MAX_STR);
                buf.writeInt(p.lootItemCount);
                buf.writeUtf(p.lootItemComponents, MAX_DATA);
                LootDefaults.STREAM_CODEC.encode(buf, p.defaults);
                buf.writeUtf(p.defaultLootTableId, MAX_STR);
                buf.writeUtf(p.defaultLootItemId, MAX_STR);
                buf.writeInt(p.defaultMode);
            },
            buf -> new OpenLostItemConfigPayload(
                    buf.readBlockPos(),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readUtf(MAX_SKIN),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readUtf(MAX_DATA),
                    LootDefaults.STREAM_CODEC.decode(buf),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
