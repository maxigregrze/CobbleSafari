package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.init.ModEffects;
import maxigregrze.cobblesafari.rotomphone.RotomPhoneActivationFx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRotoFallMixin {

    /** Rotom only shows itself for a save worth it: more than two hearts (action plan 198, § 13.3). */
    private static final int ACTIVATION_MIN_PREVENTED_DAMAGE = 4;

    @Shadow
    protected abstract int calculateFallDamage(float fallDistance, float damageMultiplier);

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
            // The landing is the moment the guard is used: the device shows itself then, not when
            // the guard went up mid-fall (action plan 198, part H).
            if (self instanceof ServerPlayer player
                    && calculateFallDamage(fallDistance, multiplier) > ACTIVATION_MIN_PREVENTED_DAMAGE) {
                RotomPhoneActivationFx.trigger(player);
            }
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
