package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.init.ModEffects;
import maxigregrze.cobblesafari.rotomphone.RotoFallGroundClear;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRotoFallMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void cobblesafari$clearRotoFallOnGround(CallbackInfo ci) {
        RotoFallGroundClear.tick((LivingEntity) (Object) this);
    }

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
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
