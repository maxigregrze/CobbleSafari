package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.effect.RedShackledEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRedShackledCombatMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void cobblesafari$blockDamageFromShackled(DamageSource source, float amount,
                                                      CallbackInfoReturnable<Boolean> cir) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && RedShackledEffects.isShackled(living)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void cobblesafari$blockAttackFromShackled(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (RedShackledEffects.isShackled((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
