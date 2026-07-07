package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRotoFallMixin {

    @Inject(
            method = "causeFallDamage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobblesafari$noFallDamageWithRotoFall(
            float fallDistance,
            float multiplier,
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(ModEffects.ROTO_FALL.holder)) {
            // Consume the guard as it does its job (the landing frame). The per-player safety tick
            // also clears it on the ground, but removing it here avoids any lingering frame.
            self.removeEffect(ModEffects.ROTO_FALL.holder);
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
