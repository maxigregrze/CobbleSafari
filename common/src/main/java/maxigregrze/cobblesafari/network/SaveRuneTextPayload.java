package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveRuneTextPayload(BlockPos pos, String text) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SaveRuneTextPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "save_rune_text"));

    public static final StreamCodec<FriendlyByteBuf, SaveRuneTextPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos);
                buf.writeUtf(payload.text, 1024);
            },
            buf -> new SaveRuneTextPayload(buf.readBlockPos(), buf.readUtf(1024))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
