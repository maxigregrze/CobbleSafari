package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S - save a statue config. Every field is re-validated and clamped server-side by
 * {@link PokeStatueConfigServerHandler}, which derives the variant from the block actually present
 * rather than trusting the one announced here.
 */
public record SavePokeStatueConfigPayload(
        BlockPos pos,
        int variant,
        String species,
        int mode,
        float scale,
        int color,
        String animation,
        float progress,
        boolean fixed,
        float speed,
        boolean showDummy,
        boolean particles
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SavePokeStatueConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "save_pokestatue_config"));

    private static final int MAX_SPECIES = 256;
    private static final int MAX_ANIMATION = 64;

    public static final StreamCodec<FriendlyByteBuf, SavePokeStatueConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeInt(p.variant);
                buf.writeUtf(p.species, MAX_SPECIES);
                buf.writeInt(p.mode);
                buf.writeFloat(p.scale);
                buf.writeInt(p.color);
                buf.writeUtf(p.animation, MAX_ANIMATION);
                buf.writeFloat(p.progress);
                buf.writeBoolean(p.fixed);
                buf.writeFloat(p.speed);
                buf.writeBoolean(p.showDummy);
                buf.writeBoolean(p.particles);
            },
            buf -> new SavePokeStatueConfigPayload(
                    buf.readBlockPos(),
                    buf.readInt(),
                    buf.readUtf(MAX_SPECIES),
                    buf.readInt(),
                    buf.readFloat(),
                    buf.readInt(),
                    buf.readUtf(MAX_ANIMATION),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
