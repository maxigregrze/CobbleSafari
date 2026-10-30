package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenAuspiciousPokeballConfigPayload(
        BlockPos pos,
        String poolBerryId,
        String poolCandyId,
        String poolBallsId,
        String poolTreasuresId,
        int minRoll,
        int maxRoll,
        LootDefaults defaults
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenAuspiciousPokeballConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_auspicious_pokeball_config"));

    private static final int MAX_STR = 512;

    public static final StreamCodec<FriendlyByteBuf, OpenAuspiciousPokeballConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeUtf(p.poolBerryId, MAX_STR);
                buf.writeUtf(p.poolCandyId, MAX_STR);
                buf.writeUtf(p.poolBallsId, MAX_STR);
                buf.writeUtf(p.poolTreasuresId, MAX_STR);
                buf.writeInt(p.minRoll);
                buf.writeInt(p.maxRoll);
                LootDefaults.STREAM_CODEC.encode(buf, p.defaults);
            },
            buf -> new OpenAuspiciousPokeballConfigPayload(
                    buf.readBlockPos(),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readInt(),
                    LootDefaults.STREAM_CODEC.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
