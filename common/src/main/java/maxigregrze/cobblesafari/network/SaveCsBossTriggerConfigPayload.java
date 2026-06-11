package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveCsBossTriggerConfigPayload(
        BlockPos pos,
        String bossRef,
        String costItemId,
        String playerRadiusStr,
        String blockRadiusStr,
        String variant
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SaveCsBossTriggerConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "save_csboss_trigger_config"));

    private static final int MAX_STR = 256;

    public static final StreamCodec<FriendlyByteBuf, SaveCsBossTriggerConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeUtf(p.bossRef, MAX_STR);
                buf.writeUtf(p.costItemId, MAX_STR);
                buf.writeUtf(p.playerRadiusStr, 16);
                buf.writeUtf(p.blockRadiusStr, 16);
                buf.writeUtf(p.variant, MAX_STR);
            },
            buf -> new SaveCsBossTriggerConfigPayload(
                    buf.readBlockPos(),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(16),
                    buf.readUtf(16),
                    buf.readUtf(MAX_STR)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
