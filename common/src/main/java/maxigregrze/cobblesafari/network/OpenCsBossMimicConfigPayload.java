package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenCsBossMimicConfigPayload(
        BlockPos pos,
        String mimicBlockId,
        String battleMimicBlockId,
        int mode
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenCsBossMimicConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_csboss_mimic_config"));

    private static final int MAX_STR = 256;

    public static final StreamCodec<FriendlyByteBuf, OpenCsBossMimicConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeUtf(p.mimicBlockId, MAX_STR);
                buf.writeUtf(p.battleMimicBlockId, MAX_STR);
                buf.writeVarInt(p.mode);
            },
            buf -> new OpenCsBossMimicConfigPayload(
                    buf.readBlockPos(),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
