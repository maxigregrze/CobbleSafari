package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * @param subscreen 0 Begin, 1 host room, 2 error, 3 guest read-only, 4 close all GUI
 */
public record UnionAppResultPayload(
        int subscreen,
        int instancesUsed,
        int instancesMax,
        int[] currentCode,
        String errorKey
) implements CustomPacketPayload {

    public static final int SUB_BEGIN = 0;
    public static final int SUB_UNION_HOST = 1;
    public static final int SUB_ERROR = 2;
    public static final int SUB_GUEST = 3;
    public static final int SUB_CLOSE_GUI = 4;

    public static final CustomPacketPayload.Type<UnionAppResultPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "union_app_result"));

    public static final StreamCodec<FriendlyByteBuf, UnionAppResultPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.subscreen());
                buf.writeVarInt(p.instancesUsed());
                buf.writeVarInt(p.instancesMax());
                int[] c = p.currentCode();
                buf.writeVarInt(c == null ? 0 : c.length);
                if (c != null) {
                    for (int d : c) {
                        buf.writeVarInt(d);
                    }
                }
                buf.writeUtf(p.errorKey() == null ? "" : p.errorKey());
            },
            buf -> {
                int sub = buf.readVarInt();
                int used = buf.readVarInt();
                int max = buf.readVarInt();
                int len = buf.readVarInt();
                int[] code = new int[len];
                for (int i = 0; i < len; i++) {
                    code[i] = buf.readVarInt();
                }
                String err = buf.readUtf();
                return new UnionAppResultPayload(sub, used, max, code, err);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnionAppResultPayload that)) return false;
        return subscreen == that.subscreen
                && instancesUsed == that.instancesUsed
                && instancesMax == that.instancesMax
                && Arrays.equals(currentCode, that.currentCode)
                && Objects.equals(errorKey, that.errorKey);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(subscreen, instancesUsed, instancesMax, errorKey) + Arrays.hashCode(currentCode);
    }

    @Override
    public String toString() {
        return "UnionAppResultPayload[subscreen=" + subscreen
                + ", instancesUsed=" + instancesUsed
                + ", instancesMax=" + instancesMax
                + ", currentCode=" + Arrays.toString(currentCode)
                + ", errorKey=" + errorKey + "]";
    }
}
