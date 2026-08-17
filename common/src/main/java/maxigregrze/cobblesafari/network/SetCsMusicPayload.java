package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: tells the client which csmusic instruction to play (or silence), how to end the currently
 * playing track, and an optional start offset (ms) into the loop. Client has no authority (no C2S).
 *
 * <p>An instruction carries <b>one or more</b> entries. A single entry is a plain track. Several
 * entries are a tag pool the client sequences itself: it draws one, plays {@code intro → loop ×
 * repeat → outro}, draws again, and so on until a new instruction arrives. {@code repeat == 0}
 * means the drawn track loops forever, which is also what a pool of one does.</p>
 *
 * <p>{@code outgoingMode}: {@link #MODE_CUT} / {@link #MODE_FADE} / {@link #MODE_OUTRO} /
 * {@link #MODE_CROSSFADE}. In {@code MODE_CROSSFADE} the incoming track is started at the outgoing
 * track's <b>current loop playhead</b> (computed client-side) and cross-ramped.</p>
 */
public record SetCsMusicPayload(
        boolean hasTrack,
        String id,
        List<Entry> entries,
        int repeat,
        int outgoingMode,
        int startMs
) implements CustomPacketPayload {

    public static final int MODE_CUT = 0;
    public static final int MODE_FADE = 1;
    public static final int MODE_OUTRO = 2;
    public static final int MODE_CROSSFADE = 3;

    /** Hard bound on the entry list, so decoding cannot be talked into an unbounded allocation. */
    public static final int MAX_ENTRIES = 32;

    /**
     * One playable track of an instruction. {@code parent} travels so the client can restore the
     * synced crossfade inside a pool, where the server does not know which entry is playing.
     */
    public record Entry(String id,
                        @Nullable ResourceLocation intro,
                        ResourceLocation loop,
                        @Nullable ResourceLocation outro,
                        @Nullable String parent) {}

    public static final CustomPacketPayload.Type<SetCsMusicPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "set_csmusic"));

    public static SetCsMusicPayload silence(int outgoingMode) {
        return new SetCsMusicPayload(false, "", List.of(), 0, outgoingMode, 0);
    }

    public static SetCsMusicPayload track(String id, @Nullable ResourceLocation intro,
                                          ResourceLocation loop, @Nullable ResourceLocation outro,
                                          int outgoingMode) {
        return track(id, intro, loop, outro, outgoingMode, 0);
    }

    public static SetCsMusicPayload track(String id, @Nullable ResourceLocation intro,
                                          ResourceLocation loop, @Nullable ResourceLocation outro,
                                          int outgoingMode, int startMs) {
        return new SetCsMusicPayload(true, id, List.of(new Entry(id, intro, loop, outro, null)),
                0, outgoingMode, startMs);
    }

    public static SetCsMusicPayload instruction(String id, List<Entry> entries, int repeat, int outgoingMode) {
        return new SetCsMusicPayload(true, id, List.copyOf(entries), Math.max(0, repeat), outgoingMode, 0);
    }

    public static final StreamCodec<FriendlyByteBuf, SetCsMusicPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.hasTrack);
                buf.writeUtf(p.id, 256);
                buf.writeInt(p.outgoingMode);
                buf.writeInt(p.startMs);
                if (p.hasTrack) {
                    buf.writeVarInt(p.repeat);
                    int count = Math.min(p.entries.size(), MAX_ENTRIES);
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) {
                        writeEntry(buf, p.entries.get(i));
                    }
                }
            },
            buf -> {
                boolean hasTrack = buf.readBoolean();
                String id = buf.readUtf(256);
                int mode = buf.readInt();
                int startMs = buf.readInt();
                if (!hasTrack) {
                    return new SetCsMusicPayload(false, id, List.of(), 0, mode, startMs);
                }
                int repeat = buf.readVarInt();
                int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
                List<Entry> entries = new ArrayList<>(Math.max(0, count));
                for (int i = 0; i < count; i++) {
                    entries.add(readEntry(buf));
                }
                return new SetCsMusicPayload(true, id, List.copyOf(entries), repeat, mode, startMs);
            }
    );

    private static void writeEntry(FriendlyByteBuf buf, Entry entry) {
        buf.writeUtf(entry.id(), 256);
        buf.writeResourceLocation(entry.loop());
        writeOptional(buf, entry.intro());
        writeOptional(buf, entry.outro());
        buf.writeBoolean(entry.parent() != null);
        if (entry.parent() != null) {
            buf.writeUtf(entry.parent(), 256);
        }
    }

    private static Entry readEntry(FriendlyByteBuf buf) {
        String id = buf.readUtf(256);
        ResourceLocation loop = buf.readResourceLocation();
        ResourceLocation intro = readOptional(buf);
        ResourceLocation outro = readOptional(buf);
        String parent = buf.readBoolean() ? buf.readUtf(256) : null;
        return new Entry(id, intro, loop, outro, parent);
    }

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
