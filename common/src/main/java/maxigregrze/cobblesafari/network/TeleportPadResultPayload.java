package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C — result of a {@link TeleportPadActionPayload} search, applied to the open config screen.
 * For {@link Status#VALID} / {@link Status#FOUND} the {@code x/y/z} carry the world-axis offset
 * to fill into the GUI (and turn the text green).
 */
public record TeleportPadResultPayload(
        Status status,
        int x,
        int y,
        int z
) implements CustomPacketPayload {

    public enum Status {
        /** Check: the typed offset points at a valid, compatible, reachable pad. */
        VALID,
        /** Check: no pad at the target. */
        NOT_FOUND,
        /** Check: pad found but wrong mode/facing. */
        WRONG_MODE,
        /** Check: target beyond 100-block range / illegal shape for the mode. */
        OUT_OF_RANGE,
        /** Check: path between the two pads is obstructed. */
        OBSTRUCTED,
        /** Auto-detect: a compatible pad was found. */
        FOUND,
        /** Auto-detect: nothing compatible within range. */
        NONE
    }

    public static final CustomPacketPayload.Type<TeleportPadResultPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "teleport_pad_result"));

    public static final StreamCodec<FriendlyByteBuf, TeleportPadResultPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeEnum(p.status);
                buf.writeInt(p.x);
                buf.writeInt(p.y);
                buf.writeInt(p.z);
            },
            buf -> new TeleportPadResultPayload(
                    buf.readEnum(Status.class),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    public boolean fills() {
        return status == Status.VALID || status == Status.FOUND;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
