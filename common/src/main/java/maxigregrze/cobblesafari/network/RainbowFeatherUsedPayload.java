package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Announces a Rainbow Feather save to the clients that can see it.
 *
 * <p>The vanilla entity event 35 cannot be reused: {@code ClientPacketListener#findTotem} scans the
 * hands for a {@code TOTEM_OF_UNDYING} and, finding none, falls back to
 * {@code new ItemStack(Items.TOTEM_OF_UNDYING)} - so a feather save would flash a totem on
 * screen. This payload carries the same information and lets the client show the right item.</p>
 *
 * <p>The entity is identified by its raw network id, never by reference, like
 * {@link ShackleSyncPayload}.</p>
 */
public record RainbowFeatherUsedPayload(int entityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RainbowFeatherUsedPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "rainbow_feather_used"));

    public static final StreamCodec<FriendlyByteBuf, RainbowFeatherUsedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    RainbowFeatherUsedPayload::entityId,
                    RainbowFeatherUsedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
