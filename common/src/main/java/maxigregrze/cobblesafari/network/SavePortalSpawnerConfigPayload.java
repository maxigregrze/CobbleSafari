package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: settings saved from the Portal Spawner config GUI; clamped server-side (action plan 188). */
public record SavePortalSpawnerConfigPayload(
        BlockPos pos,
        boolean enabled,
        String destination,
        int lifespanSeconds,
        int mode,
        int intervalSeconds,
        int minIntervalSeconds,
        int maxIntervalSeconds,
        int minSignal
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SavePortalSpawnerConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "save_portal_spawner_config"));

    private static final int MAX_ID = 256;

    public static final StreamCodec<FriendlyByteBuf, SavePortalSpawnerConfigPayload> STREAM_CODEC = StreamCodec.of(
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
            },
            buf -> new SavePortalSpawnerConfigPayload(
                    buf.readBlockPos(),
                    buf.readBoolean(),
                    buf.readUtf(MAX_ID),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
