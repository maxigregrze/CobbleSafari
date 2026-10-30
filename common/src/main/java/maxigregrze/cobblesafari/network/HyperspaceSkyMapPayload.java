package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

/**
 * S2C: the overworld map shown in the Hyperspace sky of the instance the player is in (action plan
 * 197, section 6.1). {@code colors} holds {@link #MAP_SIZE}² packed map colours (vanilla
 * {@code MapColor} packed ids, row by row from the north-west corner, 0 = no data) when
 * {@code kind == KIND_MAP}, and is empty otherwise. The client never learns where the map was taken.
 */
public record HyperspaceSkyMapPayload(byte kind, byte[] colors) implements CustomPacketPayload {

    public static final int MAP_SIZE = 256;
    public static final int MAP_BYTES = MAP_SIZE * MAP_SIZE;

    public static final byte KIND_NONE = 0;
    public static final byte KIND_MAP = 1;
    public static final byte KIND_STATIC = 2;

    public static final CustomPacketPayload.Type<HyperspaceSkyMapPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "hyperspace_sky_map"));

    public static final StreamCodec<FriendlyByteBuf, HyperspaceSkyMapPayload> STREAM_CODEC =
            StreamCodec.of(HyperspaceSkyMapPayload::encode, HyperspaceSkyMapPayload::decode);

    public static HyperspaceSkyMapPayload none() {
        return new HyperspaceSkyMapPayload(KIND_NONE, new byte[0]);
    }

    public static HyperspaceSkyMapPayload staticImage() {
        return new HyperspaceSkyMapPayload(KIND_STATIC, new byte[0]);
    }

    public static HyperspaceSkyMapPayload map(byte[] colors) {
        return new HyperspaceSkyMapPayload(KIND_MAP, colors);
    }

    private static void encode(FriendlyByteBuf buf, HyperspaceSkyMapPayload payload) {
        buf.writeByte(payload.kind);
        buf.writeByteArray(payload.colors);
    }

    private static HyperspaceSkyMapPayload decode(FriendlyByteBuf buf) {
        byte kind = buf.readByte();
        byte[] colors = buf.readByteArray(MAP_BYTES);
        if (kind == KIND_MAP && colors.length != MAP_BYTES) {
            return none();
        }
        return new HyperspaceSkyMapPayload(kind, colors);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HyperspaceSkyMapPayload other && kind == other.kind && Arrays.equals(colors, other.colors);
    }

    @Override
    public int hashCode() {
        return 31 * kind + Arrays.hashCode(colors);
    }

    @Override
    public String toString() {
        return "HyperspaceSkyMapPayload[kind=" + kind + ", colors=" + colors.length + " bytes]";
    }
}
