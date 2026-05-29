package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AuspiciousPokeballResetClaimsPayload(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AuspiciousPokeballResetClaimsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "auspicious_pokeball_reset_claims"));

    public static final StreamCodec<FriendlyByteBuf, AuspiciousPokeballResetClaimsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeBlockPos(p.pos),
            buf -> new AuspiciousPokeballResetClaimsPayload(buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
