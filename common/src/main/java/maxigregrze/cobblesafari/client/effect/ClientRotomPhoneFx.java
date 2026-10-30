package maxigregrze.cobblesafari.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * Flashes the player's Rotom device on screen, fed by
 * {@link maxigregrze.cobblesafari.network.RotomPhoneActivationPayload} (action plan 198, part H).
 *
 * <p>Only the totem's item flash, like {@link ClientRainbowFeatherFx} minus its particles and
 * sound: the server plays the Rotom cry for everyone around instead. The flash resolves the
 * stack's model overrides, so it shows the device's current skin.</p>
 */
public final class ClientRotomPhoneFx {

    private ClientRotomPhoneFx() {}

    /** Called on the client thread by the payload receiver. */
    public static void accept(ItemStack device) {
        if (device.isEmpty()) {
            return;
        }
        Minecraft.getInstance().gameRenderer.displayItemActivation(device);
    }
}
