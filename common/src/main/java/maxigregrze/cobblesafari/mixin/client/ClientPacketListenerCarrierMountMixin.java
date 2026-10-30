package maxigregrze.cobblesafari.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import maxigregrze.cobblesafari.entity.carrier.CarrierEntity;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Pas de message « mount.onboard » (ni de narration) quand le joueur local est pris par un
 * {@link CarrierEntity} (plan 199 § 6.3) : il ne peut de toute façon pas descendre.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerCarrierMountMixin {

    @WrapWithCondition(
            method = "handleSetEntityPassengersPacket",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V")
    )
    private boolean cobblesafari$hideCarrierMountMessage(Gui gui, Component message, boolean animate) {
        return !cobblesafari$ridingCarrier();
    }

    @WrapWithCondition(
            method = "handleSetEntityPassengersPacket",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/GameNarrator;sayNow(Lnet/minecraft/network/chat/Component;)V")
    )
    private boolean cobblesafari$muteCarrierMountNarration(GameNarrator narrator, Component message) {
        return !cobblesafari$ridingCarrier();
    }

    private static boolean cobblesafari$ridingCarrier() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof CarrierEntity;
    }
}
