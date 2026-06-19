package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S — the player pressed the jump key while standing on a teleport pad.
 * The server locates the pad under the player and attempts the teleport (never trusts a position).
 */
public record TeleportPadJumpPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TeleportPadJumpPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "teleport_pad_jump"));

    public static final StreamCodec<FriendlyByteBuf, TeleportPadJumpPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> new TeleportPadJumpPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
