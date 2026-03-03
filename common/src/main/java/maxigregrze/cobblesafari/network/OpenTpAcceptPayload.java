package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenTpAcceptPayload(
        String dimensionName,
        String dimensionId,
        boolean hasEntryFee,
        boolean isCobbledollarFee,
        int entryFeeAmount,
        String entryFeeItem,
        String source,
        boolean alreadyPaidToday
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenTpAcceptPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_tp_accept"));

    public static final StreamCodec<FriendlyByteBuf, OpenTpAcceptPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.dimensionName);
                buf.writeUtf(payload.dimensionId);
                buf.writeBoolean(payload.hasEntryFee);
                buf.writeBoolean(payload.isCobbledollarFee);
                buf.writeInt(payload.entryFeeAmount);
                buf.writeUtf(payload.entryFeeItem);
                buf.writeUtf(payload.source);
                buf.writeBoolean(payload.alreadyPaidToday);
            },
            buf -> new OpenTpAcceptPayload(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
