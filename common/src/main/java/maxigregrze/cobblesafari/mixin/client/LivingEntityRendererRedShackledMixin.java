package maxigregrze.cobblesafari.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.client.renderer.RedShackledChainRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draw chains after the model pose stack is popped, still in entity world space from
 * {@link net.minecraft.client.renderer.entity.EntityRenderDispatcher}.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererRedShackledMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void cobblesafari$renderShackledChains(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                                                   MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        RedShackledChainRenderer.renderOnEntity(entity, partialTick, poseStack, bufferSource);
    }
}
