package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.manager.BannedItemsManager;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * forbidElytra (action plan 198, § 15). Every elytra starts here, whichever item provides it:
 * vanilla's own check, NeoForge's {@code canElytraFly} and Fabric's {@code EntityElytraEvents} all
 * live inside this method, so refusing it at HEAD refuses them all. Runs on both sides - the client
 * reads the synced restrictions, so the glide never starts locally and no packet is sent.
 */
@Mixin(Player.class)
public abstract class PlayerElytraRestrictionMixin {

    @Inject(
            method = "tryToStartFallFlying",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobblesafari$forbidElytra(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (BannedItemsManager.isElytraAllowed(self.level().dimension())) {
            return;
        }
        // Fabric calls this on every mid-air jump press, elytra or not (a Roto-Glide double jump
        // included), so the message waits for the conditions under which the wings would open.
        if (!self.onGround() && !self.isFallFlying() && !self.isInWater()
                && !self.hasEffect(MobEffects.LEVITATION)
                && Services.PLATFORM.canWornElytraFly(self)) {
            self.displayClientMessage(Component.translatable("cobblesafari.ban.elytra_banned"), true);
        }
        cir.setReturnValue(false);
    }
}
