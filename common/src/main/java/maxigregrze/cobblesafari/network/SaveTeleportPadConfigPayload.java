package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S — save the teleport-pad config: mode + destination offset (world axes) + tint colour.
 * Server validates (creative / Tinkhammer + proximity) and converts the offset to facing-relative storage.
 */
public record SaveTeleportPadConfigPayload(
        BlockPos pos,
        String mode,
        int x,
        int y,
        int z,
        int color
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SaveTeleportPadConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "save_teleport_pad_config"));

    private static final int MAX_STR = 32;

    public static final StreamCodec<FriendlyByteBuf, SaveTeleportPadConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeUtf(p.mode, MAX_STR);
                buf.writeInt(p.x);
                buf.writeInt(p.y);
                buf.writeInt(p.z);
                buf.writeInt(p.color);
            },
            buf -> new SaveTeleportPadConfigPayload(
                    buf.readBlockPos(),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
