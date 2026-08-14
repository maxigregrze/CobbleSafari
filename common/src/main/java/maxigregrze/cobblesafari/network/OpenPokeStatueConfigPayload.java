package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C - open the statue config GUI, pre-filled. {@code pos} is the part that stores the data (the
 * lower half for the regular statue, the block itself for the creative one). {@code variant} tells
 * the shared screen which rows, labels and clamps to use; {@code color} is {@code -1} when unset.
 */
public record OpenPokeStatueConfigPayload(
        BlockPos pos,
        int variant,
        String species,
        int mode,
        float scale,
        int color,
        String animation,
        float progress
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenPokeStatueConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "open_pokestatue_config"));

    private static final int MAX_SPECIES = 256;
    private static final int MAX_ANIMATION = 64;

    public static final StreamCodec<FriendlyByteBuf, OpenPokeStatueConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeInt(p.variant);
                buf.writeUtf(p.species, MAX_SPECIES);
                buf.writeInt(p.mode);
                buf.writeFloat(p.scale);
                buf.writeInt(p.color);
                buf.writeUtf(p.animation, MAX_ANIMATION);
                buf.writeFloat(p.progress);
            },
            buf -> new OpenPokeStatueConfigPayload(
                    buf.readBlockPos(),
                    buf.readInt(),
                    buf.readUtf(MAX_SPECIES),
                    buf.readInt(),
                    buf.readFloat(),
                    buf.readInt(),
                    buf.readUtf(MAX_ANIMATION),
                    buf.readFloat()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
