package maxigregrze.cobblesafari.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerRotoFallMixin {

    @WrapOperation(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/core/Holder;)Z"
            )
    )
    private boolean cobblesafari$rotoFallActsLikeSlowFallForHasEffect(
            Player instance,
            Holder<MobEffect> effect,
            Operation<Boolean> original
    ) {
        return original.call(instance, effect)
                || (MobEffects.SLOW_FALLING.equals(effect) && instance.hasEffect(ModEffects.ROTO_FALL.holder));
    }
}
