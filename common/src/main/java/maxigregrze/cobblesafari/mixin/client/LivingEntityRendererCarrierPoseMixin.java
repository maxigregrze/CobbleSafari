package maxigregrze.cobblesafari.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import maxigregrze.cobblesafari.entity.carrier.CarrierEntity;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Passager d'un {@link CarrierEntity} rendu debout, pas assis (plan 199 § 6.3) : vanilla 1.21.1 fait
 * {@code model.riding = entity.isPassenger()} sans exception possible.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererCarrierPoseMixin {

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isPassenger()Z")
    )
    private boolean cobblesafari$standOnCarrier(boolean original, @Local(argsOnly = true) LivingEntity entity) {
        return original && !(entity.getVehicle() instanceof CarrierEntity);
    }
}
