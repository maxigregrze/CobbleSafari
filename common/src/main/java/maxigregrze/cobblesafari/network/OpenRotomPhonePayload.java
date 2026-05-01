package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenRotomPhonePayload(
        String rotomName,
        boolean shinyStatus,
        String currentSkin,
        boolean safetyMode,
        boolean rotoGlide
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenRotomPhonePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_rotom_phone"));

    public static final StreamCodec<FriendlyByteBuf, OpenRotomPhonePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.rotomName());
                buf.writeBoolean(payload.shinyStatus());
                buf.writeUtf(payload.currentSkin());
                buf.writeBoolean(payload.safetyMode());
                buf.writeBoolean(payload.rotoGlide());
            },
            buf -> new OpenRotomPhonePayload(
                    buf.readUtf(),
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readBoolean(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
