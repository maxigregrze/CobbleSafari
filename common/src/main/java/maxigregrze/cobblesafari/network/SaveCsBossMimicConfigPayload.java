package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveCsBossMimicConfigPayload(
        BlockPos pos,
        String mimicBlockId,
        boolean reverse
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SaveCsBossMimicConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "save_csboss_mimic_config"));

    private static final int MAX_STR = 256;

    public static final StreamCodec<FriendlyByteBuf, SaveCsBossMimicConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeUtf(p.mimicBlockId, MAX_STR);
                buf.writeBoolean(p.reverse);
            },
            buf -> new SaveCsBossMimicConfigPayload(
                    buf.readBlockPos(),
                    buf.readUtf(MAX_STR),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
