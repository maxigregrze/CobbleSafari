package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * S2C: tells the client which csmusic track to play (or silence), how to end the currently
 * playing track, and an optional start offset (ms) into the loop. Client has no authority (no C2S).
 *
 * <p>{@code outgoingMode}: {@link #MODE_CUT} / {@link #MODE_FADE} / {@link #MODE_OUTRO} /
 * {@link #MODE_CROSSFADE}. In {@code MODE_CROSSFADE} the incoming track is started at the parent's
 * <b>current loop playhead</b> (computed client-side) and cross-ramped over ~1&nbsp;s.</p>
 */
public record SetCsMusicPayload(
        boolean hasTrack,
        String id,
        @Nullable ResourceLocation intro,
        @Nullable ResourceLocation loop,
        @Nullable ResourceLocation outro,
        int outgoingMode,
        int startMs
) implements CustomPacketPayload {

    public static final int MODE_CUT = 0;
    public static final int MODE_FADE = 1;
    public static final int MODE_OUTRO = 2;
    public static final int MODE_CROSSFADE = 3;

    public static final CustomPacketPayload.Type<SetCsMusicPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "set_csmusic"));

    public static SetCsMusicPayload silence(int outgoingMode) {
        return new SetCsMusicPayload(false, "", null, null, null, outgoingMode, 0);
    }

    public static SetCsMusicPayload track(String id, @Nullable ResourceLocation intro,
                                          ResourceLocation loop, @Nullable ResourceLocation outro,
                                          int outgoingMode) {
        return track(id, intro, loop, outro, outgoingMode, 0);
    }

    public static SetCsMusicPayload track(String id, @Nullable ResourceLocation intro,
                                          ResourceLocation loop, @Nullable ResourceLocation outro,
                                          int outgoingMode, int startMs) {
        return new SetCsMusicPayload(true, id, intro, loop, outro, outgoingMode, startMs);
    }

    public static final StreamCodec<FriendlyByteBuf, SetCsMusicPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.hasTrack);
                buf.writeUtf(p.id, 256);
                buf.writeInt(p.outgoingMode);
                buf.writeInt(p.startMs);
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
                int startMs = buf.readInt();
                if (!hasTrack) {
                    return new SetCsMusicPayload(false, id, null, null, null, mode, startMs);
                }
                ResourceLocation loop = buf.readResourceLocation();
                ResourceLocation intro = readOptional(buf);
                ResourceLocation outro = readOptional(buf);
                return new SetCsMusicPayload(true, id, intro, loop, outro, mode, startMs);
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
