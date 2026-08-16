package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Replicates the "shackled" state of a living entity to the clients tracking it.
 *
 * <p>The state is carried by a dedicated payload rather than an extra {@code SynchedEntityData}
 * entry: adding one to {@code LivingEntity} shifts every data id of its subclasses (including
 * {@code Player}) by one, which desynchronises a CobbleSafari client from any server that does not
 * run the mod.</p>
 *
 * <p>The entity is identified by its raw network id, never by reference: the start-tracking hook
 * fires <em>before</em> the entity's spawn packet reaches the client, so the payload can legitimately
 * arrive before the entity exists client-side.</p>
 */
public record ShackleSyncPayload(int entityId, boolean shackled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShackleSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "shackle_sync"));

    public static final StreamCodec<FriendlyByteBuf, ShackleSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ShackleSyncPayload::entityId,
            ByteBufCodecs.BOOL,
            ShackleSyncPayload::shackled,
            ShackleSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
