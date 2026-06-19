package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C — open the creative teleport-pad config GUI, pre-filled.
 * {@code x/y/z} are the destination offset expressed in <b>world</b> axes
 * (converted server-side from the facing-relative storage); {@code linked} drives the
 * initial green text colour.
 */
public record OpenTeleportPadConfigPayload(
        BlockPos pos,
        String mode,
        int x,
        int y,
        int z,
        boolean linked,
        int color,
        boolean allowColor
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenTeleportPadConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_teleport_pad_config"));

    private static final int MAX_STR = 32;

    public static final StreamCodec<FriendlyByteBuf, OpenTeleportPadConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeUtf(p.mode, MAX_STR);
                buf.writeInt(p.x);
                buf.writeInt(p.y);
                buf.writeInt(p.z);
                buf.writeBoolean(p.linked);
                buf.writeInt(p.color);
                buf.writeBoolean(p.allowColor);
            },
            buf -> new OpenTeleportPadConfigPayload(
                    buf.readBlockPos(),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
