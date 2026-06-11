package maxigregrze.cobblesafari.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.client.renderer.CsBossEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.RedShackledChainRenderer;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CsBossEntityRenderer.class)
public abstract class CsBossEntityRendererRedShackledMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void cobblesafari$renderShackledChainsOnBoss(CsBossEntity entity, float yaw, float partialTicks,
                                                         PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                         CallbackInfo ci) {
        RedShackledChainRenderer.renderOnEntity(entity, partialTicks, poseStack, buffer);
    }
}
