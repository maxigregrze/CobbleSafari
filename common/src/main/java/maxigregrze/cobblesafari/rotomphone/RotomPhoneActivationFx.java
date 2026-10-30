package maxigregrze.cobblesafari.rotomphone;

import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.network.ModNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Feedback when a Rotom device saves its owner (action plan 198, part H): the device flashes on
 * their screen like a totem, and Rotom cries for everyone around.
 *
 * <p>Called on the server for a Roto-Glide boost and for a Roto-Fall landing.</p>
 */
public final class RotomPhoneActivationFx {

    /**
     * Cobblemon's Rotom cry. It is only declared in Cobblemon's {@code sounds.json}, not registered,
     * so it is played as a direct holder.
     */
    private static final SoundEvent ROTOM_CRY = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("cobblemon", "pokemon.rotom.cry"));

    private RotomPhoneActivationFx() {}

    public static void trigger(ServerPlayer player) {
        ItemStack device = RotomPhoneEquipped.findPhoneForHandOrAccessory(player);
        // The device may have left the hand mid-fall: still show a phone rather than nothing.
        ModNetworking.sendRotomPhoneActivation(player,
                device.isEmpty() ? new ItemStack(ModItems.ROTOM_PHONE) : device.copy());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ROTOM_CRY, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
