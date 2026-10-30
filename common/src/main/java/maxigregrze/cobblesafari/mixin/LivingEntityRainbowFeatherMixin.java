package maxigregrze.cobblesafari.mixin;

import maxigregrze.cobblesafari.event.RainbowFeatherRevive;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRainbowFeatherMixin {

    /**
     * Lets a Rainbow Feather stand in for a Totem of Undying, with a stronger effect set.
     *
     * <p>Injected here rather than into {@code hurt} because this is where the vanilla
     * {@code BYPASSES_INVULNERABILITY} exemption lives: the feather must not save from the void
     * or from {@code /kill}, exactly like the totem. Running at HEAD also means the feather is
     * consumed first when the entity holds both.</p>
     */
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void cobblesafari$rainbowFeather(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        // In a timed dimension the timer rescue evacuates the player instead: neither the feather
        // nor a vanilla totem is spent (NeoForge reaches this check before its LivingDeathEvent).
        if (self instanceof net.minecraft.server.level.ServerPlayer player
                && maxigregrze.cobblesafari.event.DimensionTimerDeathHandler.wouldRescue(player)) {
            cir.setReturnValue(false);
            return;
        }
        if (RainbowFeatherRevive.tryConsume(self, source)) {
            cir.setReturnValue(true);
        }
    }
}
