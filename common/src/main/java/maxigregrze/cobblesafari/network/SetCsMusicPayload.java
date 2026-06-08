package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * S2C: tells the client which csmusic track to play (or silence), and how to end the
 * currently playing track (plan 105 § 5). Client has no authority (no C2S).
 */
public record SetCsMusicPayload(
        boolean hasTrack,
        String id,
        @Nullable ResourceLocation intro,
        @Nullable ResourceLocation loop,
        @Nullable ResourceLocation outro,
        int outgoingMode
) implements CustomPacketPayload {

    public static final int MODE_CUT = 0;
    public static final int MODE_FADE = 1;
    public static final int MODE_OUTRO = 2;

    public static final CustomPacketPayload.Type<SetCsMusicPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "set_csmusic"));

    public static SetCsMusicPayload silence(int outgoingMode) {
        return new SetCsMusicPayload(false, "", null, null, null, outgoingMode);
    }

    public static SetCsMusicPayload track(String id, @Nullable ResourceLocation intro,
                                          ResourceLocation loop, @Nullable ResourceLocation outro,
                                          int outgoingMode) {
        return new SetCsMusicPayload(true, id, intro, loop, outro, outgoingMode);
    }

    public static final StreamCodec<FriendlyByteBuf, SetCsMusicPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.hasTrack);
                buf.writeUtf(p.id, 256);
                buf.writeInt(p.outgoingMode);
                if (p.hasTrack) {
                    buf.writeResourceLocation(p.loop);
                    writeOptional(buf, p.intro);
                    writeOptional(buf, p.outro);
                }
            },
            buf -> {
                boolean hasTrack = buf.readBoolean();
                String id = buf.readUtf(256);
                int mode = buf.readInt();
                if (!hasTrack) {
                    return new SetCsMusicPayload(false, id, null, null, null, mode);
                }
                ResourceLocation loop = buf.readResourceLocation();
                ResourceLocation intro = readOptional(buf);
                ResourceLocation outro = readOptional(buf);
                return new SetCsMusicPayload(true, id, intro, loop, outro, mode);
            }
    );

    private static void writeOptional(FriendlyByteBuf buf, @Nullable ResourceLocation rl) {
        buf.writeBoolean(rl != null);
        if (rl != null) {
            buf.writeResourceLocation(rl);
        }
    }

    @Nullable
    private static ResourceLocation readOptional(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
