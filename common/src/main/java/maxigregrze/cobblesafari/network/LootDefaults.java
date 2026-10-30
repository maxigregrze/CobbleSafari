package maxigregrze.cobblesafari.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * The server's {@code misc_config.json} defaults for a loot block's pools and rolls, sent with its
 * config screen so Reset restores the server's values, never the client's file (action plan 198, F6).
 */
public record LootDefaults(
        String poolBerryId,
        String poolCandyId,
        String poolBallsId,
        String poolTreasuresId,
        int minRoll,
        int maxRoll
) {

    private static final int MAX_STR = 512;

    public static final StreamCodec<FriendlyByteBuf, LootDefaults> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> {
                buf.writeUtf(d.poolBerryId, MAX_STR);
                buf.writeUtf(d.poolCandyId, MAX_STR);
                buf.writeUtf(d.poolBallsId, MAX_STR);
                buf.writeUtf(d.poolTreasuresId, MAX_STR);
                buf.writeInt(d.minRoll);
                buf.writeInt(d.maxRoll);
            },
            buf -> new LootDefaults(
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readInt()
            )
    );
}
