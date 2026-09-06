package maxigregrze.cobblesafari.warpride.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Every payload of the Ultra Warp Ride mini-game. The three client-to-server payloads carry inputs
 * only, never an outcome: no distance, no energy, no rank, no type, no collision result.
 */
public final class WarpRidePayloads {

    private WarpRidePayloads() {}

    private static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );

    /** Steering intents. The only thing a client is ever allowed to assert. */
    public static final int INPUT_LEFT = 0;
    public static final int INPUT_RIGHT = 1;
    public static final int INPUT_JUMP = 2;

    public static final int EVENT_POSITIVE = 0;
    public static final int EVENT_NEGATIVE = 1;
    public static final int EVENT_DRIFT = 2;
    public static final int EVENT_PORTAL_TAKEN = 3;
    public static final int EVENT_RIDE_ENDED = 4;
    public static final int EVENT_COUNTDOWN = 5;
    /** The parent clock crossed its panic threshold: energy now drains ten times faster. */
    public static final int EVENT_PANIC = 6;
    /**
     * The ride ended on a portal <b>and</b> a sub-dungeon is being generated: hold the black
     * screen instead of showing the result, because the server is about to teleport. Sent only
     * for a parented ride; a parentless (debug) one shows its outcome as before.
     */
    public static final int EVENT_HANDOFF = 7;

    public static final int END_REASON_PORTAL = 0;
    public static final int END_REASON_ABORTED = 1;
    /** The ride was won and the sub-dungeon generated: the player is already through. */
    public static final int END_REASON_HANDOFF = 2;

    /**
     * @param kind 0 positive, 1 negative, 2 portal
     * @param lane 0..7, or -1 for a portal, which also carries {@code rank} 0..3 and {@code typeId}
     */
    public record ObjectData(int kind, int lane, int rank, int typeId) {}

    /** @param trackPos absolute position on the track, in light years */
    public record WaveData(int index, double trackPos, List<ObjectData> objects) {}

    /** One {@code typeId} to tint (ARGB) pair. Species lists and structure pools stay server-side. */
    public record TintData(int typeId, int tint) {}

    /** Tutorial dismissed; start the countdown. Accepted once, only in {@code AWAITING_READY}. */
    public record ReadyPayload(UUID sessionId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ReadyPayload> TYPE =
                new CustomPacketPayload.Type<>(id("warpride_ready"));

        public static final StreamCodec<FriendlyByteBuf, ReadyPayload> STREAM_CODEC =
                StreamCodec.of((buf, p) -> UUID_CODEC.encode(buf, p.sessionId()),
                        buf -> new ReadyPayload(UUID_CODEC.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * The only in-ride C2S payload. {@code clientTick} is the sole client-supplied timing the design
     * trusts, and it is bounded to {@code lagBudgetTicks} of hindsight.
     */
    public record InputPayload(UUID sessionId, int input, long clientTick) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<InputPayload> TYPE =
                new CustomPacketPayload.Type<>(id("warpride_input"));

        public static final StreamCodec<FriendlyByteBuf, InputPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    UUID_CODEC.encode(buf, p.sessionId());
                    buf.writeVarInt(p.input());
                    buf.writeVarLong(p.clientTick());
                },
                buf -> new InputPayload(UUID_CODEC.decode(buf), buf.readVarInt(), buf.readVarLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Screen closed by any means; equivalent to a disconnect. */
    public record ClosePayload(UUID sessionId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClosePayload> TYPE =
                new CustomPacketPayload.Type<>(id("warpride_close"));

        public static final StreamCodec<FriendlyByteBuf, ClosePayload> STREAM_CODEC =
                StreamCodec.of((buf, p) -> UUID_CODEC.encode(buf, p.sessionId()),
                        buf -> new ClosePayload(UUID_CODEC.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Opens the screen with everything the client needs to predict; it never reads the config. */
    public record OpenPayload(
            UUID sessionId,
            boolean showTutorial,
            double baseSpeed,
            double minSpeed,
            double maxSpeed,
            double waveSpacingLy,
            double visibleDepthLy,
            int ringCount,
            double projectionK,
            double startEnergy,
            int inputCooldownMs,
            int jumpTransitMs,
            double ringFourRadius,
            double fifthPlaneDepth,
            double flyPastMaxScale,
            int starCount,
            int canvasWidth,
            int canvasHeight,
            int lagBudgetTicks,
            int laneRotationMs,
            double portalHoldWaves,
            double driftSeconds,
            double warpAnimSeconds,
            double fadeToBlackSeconds
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenPayload> TYPE =
                new CustomPacketPayload.Type<>(id("warpride_open"));

        public static final StreamCodec<FriendlyByteBuf, OpenPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    UUID_CODEC.encode(buf, p.sessionId());
                    buf.writeBoolean(p.showTutorial());
                    buf.writeDouble(p.baseSpeed());
                    buf.writeDouble(p.minSpeed());
                    buf.writeDouble(p.maxSpeed());
                    buf.writeDouble(p.waveSpacingLy());
                    buf.writeDouble(p.visibleDepthLy());
                    buf.writeVarInt(p.ringCount());
                    buf.writeDouble(p.projectionK());
                    buf.writeDouble(p.startEnergy());
                    buf.writeVarInt(p.inputCooldownMs());
                    buf.writeVarInt(p.jumpTransitMs());
                    buf.writeDouble(p.ringFourRadius());
                    buf.writeDouble(p.fifthPlaneDepth());
                    buf.writeDouble(p.flyPastMaxScale());
                    buf.writeVarInt(p.starCount());
                    buf.writeVarInt(p.canvasWidth());
                    buf.writeVarInt(p.canvasHeight());
                    buf.writeVarInt(p.lagBudgetTicks());
                    buf.writeVarInt(p.laneRotationMs());
                    buf.writeDouble(p.portalHoldWaves());
                    buf.writeDouble(p.driftSeconds());
                    buf.writeDouble(p.warpAnimSeconds());
                    buf.writeDouble(p.fadeToBlackSeconds());
                },
                buf -> new OpenPayload(
                        UUID_CODEC.decode(buf),
                        buf.readBoolean(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readVarInt(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** {@code typeId} to tint, at login and on datapack reload. Nothing else is synced. */
    public record DestinationSyncPayload(List<TintData> tints) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DestinationSyncPayload> TYPE =
                new CustomPacketPayload.Type<>(id("warpride_destination_sync"));

        public static final StreamCodec<FriendlyByteBuf, DestinationSyncPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.tints().size());
                    for (TintData t : p.tints()) {
                        buf.writeVarInt(t.typeId());
                        buf.writeInt(t.tint());
                    }
                },
                buf -> {
                    int size = buf.readVarInt();
                    List<TintData> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        list.add(new TintData(buf.readVarInt(), buf.readInt()));
                    }
                    return new DestinationSyncPayload(list);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Newly generated waves, pushed so the client always holds the visible set plus lead. */
    public record WavesPayload(UUID sessionId, List<WaveData> waves) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WavesPayload> TYPE =
                new CustomPacketPayload.Type<>(id("warpride_waves"));

        public static final StreamCodec<FriendlyByteBuf, WavesPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    UUID_CODEC.encode(buf, p.sessionId());
                    buf.writeVarInt(p.waves().size());
                    for (WaveData w : p.waves()) {
                        buf.writeVarInt(w.index());
                        buf.writeDouble(w.trackPos());
                        buf.writeVarInt(w.objects().size());
                        for (ObjectData o : w.objects()) {
                            buf.writeVarInt(o.kind());
                            buf.writeVarInt(o.lane() + 1); // NO_LANE (-1) stays varint-friendly
                            buf.writeVarInt(o.rank());
                            buf.writeVarInt(o.typeId());
                        }
                    }
                },
                buf -> {
                    UUID sessionId = UUID_CODEC.decode(buf);
                    int waveCount = buf.readVarInt();
                    List<WaveData> waves = new ArrayList<>(waveCount);
                    for (int i = 0; i < waveCount; i++) {
                        int index = buf.readVarInt();
                        double trackPos = buf.readDouble();
                        int objectCount = buf.readVarInt();
                        List<ObjectData> objects = new ArrayList<>(objectCount);
                        for (int j = 0; j < objectCount; j++) {
                            objects.add(new ObjectData(
                                    buf.readVarInt(),
                                    buf.readVarInt() - 1,
                                    buf.readVarInt(),
                                    buf.readVarInt()));
                        }
                        waves.add(new WaveData(index, trackPos, objects));
                    }
                    return new WavesPayload(sessionId, waves);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Authoritative state, every other tick. The client eases toward it rather than snapping. */
    public record StatePayload(
            UUID sessionId,
            double odometer,
            double energy,
            int lane,
            double speed,
            int cooldownTicks,
            boolean drifting,
            boolean frozen,
            int phase,
            long serverTick
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StatePayload> TYPE =
                new CustomPacketPayload.Type<>(id("warpride_state"));

        public static final StreamCodec<FriendlyByteBuf, StatePayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    UUID_CODEC.encode(buf, p.sessionId());
                    buf.writeDouble(p.odometer());
                    buf.writeDouble(p.energy());
                    buf.writeVarInt(p.lane());
                    buf.writeDouble(p.speed());
                    buf.writeVarInt(p.cooldownTicks());
                    buf.writeBoolean(p.drifting());
                    buf.writeBoolean(p.frozen());
                    buf.writeVarInt(p.phase());
                    buf.writeVarLong(p.serverTick());
                },
                buf -> new StatePayload(
                        UUID_CODEC.decode(buf),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readVarInt(),
                        buf.readDouble(),
                        buf.readVarInt(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readVarInt(),
                        buf.readVarLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * @param event     one of the {@code EVENT_*} constants
     * @param waveIndex the wave concerned, or -1
     * @param a         portal rank, end reason, or countdown step
     * @param b         portal typeId, or 0
     * @param value     the odometer at the event, for the result screen
     */
    public record EventPayload(UUID sessionId, int event, int waveIndex, int a, int b, double value)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<EventPayload> TYPE =
                new CustomPacketPayload.Type<>(id("warpride_event"));

        public static final StreamCodec<FriendlyByteBuf, EventPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    UUID_CODEC.encode(buf, p.sessionId());
                    buf.writeVarInt(p.event());
                    buf.writeVarInt(p.waveIndex() + 1);
                    buf.writeVarInt(p.a());
                    buf.writeVarInt(p.b());
                    buf.writeDouble(p.value());
                },
                buf -> new EventPayload(
                        UUID_CODEC.decode(buf),
                        buf.readVarInt(),
                        buf.readVarInt() - 1,
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readDouble()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, path);
    }
}
