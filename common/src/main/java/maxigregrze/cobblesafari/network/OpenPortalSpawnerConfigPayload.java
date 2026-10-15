package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** S2C: opens the Portal Spawner config GUI with its current settings and status (action plan 188). */
public record OpenPortalSpawnerConfigPayload(
        BlockPos pos,
        boolean enabled,
        String destination,
        int lifespanSeconds,
        int mode,
        int intervalSeconds,
        int minIntervalSeconds,
        int maxIntervalSeconds,
        int minSignal,
        List<String> destinations,
        int status,
        long statusRemainingTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenPortalSpawnerConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_portal_spawner_config"));

    private static final int MAX_ID = 256;

    public static final StreamCodec<FriendlyByteBuf, OpenPortalSpawnerConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeBoolean(p.enabled);
                buf.writeUtf(p.destination, MAX_ID);
                buf.writeVarInt(p.lifespanSeconds);
                buf.writeVarInt(p.mode);
                buf.writeVarInt(p.intervalSeconds);
                buf.writeVarInt(p.minIntervalSeconds);
                buf.writeVarInt(p.maxIntervalSeconds);
                buf.writeVarInt(p.minSignal);
                buf.writeCollection(p.destinations, (b, s) -> b.writeUtf(s, MAX_ID));
                buf.writeVarInt(p.status);
                buf.writeVarLong(p.statusRemainingTicks);
            },
            buf -> new OpenPortalSpawnerConfigPayload(
                    buf.readBlockPos(),
                    buf.readBoolean(),
                    buf.readUtf(MAX_ID),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readList(b -> b.readUtf(MAX_ID)),
                    buf.readVarInt(),
                    buf.readVarLong()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
