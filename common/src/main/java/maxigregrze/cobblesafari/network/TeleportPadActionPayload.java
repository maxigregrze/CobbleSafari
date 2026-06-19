package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S — GUI "Check" / "Auto-detect" request. The server runs the authoritative search
 * and replies with a {@link TeleportPadResultPayload}.
 */
public record TeleportPadActionPayload(
        BlockPos pos,
        Action action,
        String mode,
        int x,
        int y,
        int z
) implements CustomPacketPayload {

    public enum Action {
        CHECK,
        AUTODETECT
    }

    public static final CustomPacketPayload.Type<TeleportPadActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "teleport_pad_action"));

    private static final int MAX_STR = 32;

    public static final StreamCodec<FriendlyByteBuf, TeleportPadActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeEnum(p.action);
                buf.writeUtf(p.mode, MAX_STR);
                buf.writeInt(p.x);
                buf.writeInt(p.y);
                buf.writeInt(p.z);
            },
            buf -> new TeleportPadActionPayload(
                    buf.readBlockPos(),
                    buf.readEnum(Action.class),
                    buf.readUtf(MAX_STR),
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
