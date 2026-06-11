package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenCsBossTriggerConfigPayload(
        BlockPos pos,
        String bossRef,
        String costItemId,
        int playerRadius,
        int blockRadius,
        String variant
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenCsBossTriggerConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_csboss_trigger_config"));

    private static final int MAX_STR = 256;

    public static final StreamCodec<FriendlyByteBuf, OpenCsBossTriggerConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeUtf(p.bossRef, MAX_STR);
                buf.writeUtf(p.costItemId, MAX_STR);
                buf.writeInt(p.playerRadius);
                buf.writeInt(p.blockRadius);
                buf.writeUtf(p.variant, MAX_STR);
            },
            buf -> new OpenCsBossTriggerConfigPayload(
                    buf.readBlockPos(),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(MAX_STR)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
