package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Tells a player that their Rotom device just did its job - a Roto-Glide boost or a Roto-Fall
 * landing (action plan 198, part H) - so their client flashes it on screen like a totem.
 *
 * <p>The stack travels whole, custom data included, so the flash shows the device's current skin.
 * It is sent to that player only: the item flash is a screen overlay nobody else would see.</p>
 */
public record RotomPhoneActivationPayload(ItemStack device) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RotomPhoneActivationPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "rotomphone_activation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RotomPhoneActivationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC,
                    RotomPhoneActivationPayload::device,
                    RotomPhoneActivationPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
