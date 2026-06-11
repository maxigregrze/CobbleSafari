package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.effect.RedShackledEffectHandler;
import maxigregrze.cobblesafari.effect.RedShackledEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRedShackledMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void cobblesafari$redShackledTick(CallbackInfo ci) {
        RedShackledEffectHandler.tick((LivingEntity) (Object) this);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void cobblesafari$redShackledFreezeTravel(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (RedShackledEffects.isShackled(self)) {
            self.setDeltaMovement(Vec3.ZERO);
            ci.cancel();
        }
    }
}
