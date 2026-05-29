package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.misc.AuspiciousPokeballGoldBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record OpenAuspiciousPokeballGoldConfigPayload(
        BlockPos pos,
        String poolBerryId,
        String poolCandyId,
        String poolBallsId,
        String poolTreasuresId,
        int minRoll,
        int maxRoll,
        boolean earnable,
        List<String> earners
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenAuspiciousPokeballGoldConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_auspicious_pokeball_gold_config"));

    private static final int MAX_STR = 512;

    public static final StreamCodec<FriendlyByteBuf, OpenAuspiciousPokeballGoldConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeUtf(p.poolBerryId, MAX_STR);
                buf.writeUtf(p.poolCandyId, MAX_STR);
                buf.writeUtf(p.poolBallsId, MAX_STR);
                buf.writeUtf(p.poolTreasuresId, MAX_STR);
                buf.writeInt(p.minRoll);
                buf.writeInt(p.maxRoll);
                buf.writeBoolean(p.earnable);
                writeEarners(buf, p.earners);
            },
            buf -> new OpenAuspiciousPokeballGoldConfigPayload(
                    buf.readBlockPos(),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    readEarners(buf)
            )
    );

    private static void writeEarners(FriendlyByteBuf buf, List<String> earners) {
        int n = Math.min(earners.size(), AuspiciousPokeballGoldBlockEntity.MAX_EARNERS);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            buf.writeUtf(earners.get(i), AuspiciousPokeballGoldBlockEntity.MAX_EARNER_NAME_LENGTH);
        }
    }

    private static List<String> readEarners(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        n = Math.min(n, AuspiciousPokeballGoldBlockEntity.MAX_EARNERS);
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(buf.readUtf(AuspiciousPokeballGoldBlockEntity.MAX_EARNER_NAME_LENGTH));
        }
        return out;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
