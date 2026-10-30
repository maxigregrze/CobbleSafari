package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.entity.carrier.CarrierEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Maj ne libère pas un passager de {@link CarrierEntity} verrouillé (plan 199 § 6.3) : c'est le seul
 * chemin de démontage volontaire ({@code Player#rideTick} côté serveur).
 */
@Mixin(Player.class)
public abstract class PlayerCarrierDismountMixin {

    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void cobblesafari$lockedCarrier(CallbackInfoReturnable<Boolean> cir) {
        if (((Player) (Object) this).getVehicle() instanceof CarrierEntity carrier && !carrier.allowsDismount()) {
            cir.setReturnValue(false);
        }
    }
}
