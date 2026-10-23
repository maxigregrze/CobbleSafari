package maxigregrze.cobblesafari.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.client.screen.warpride.WarpRideRiderProxy;
import maxigregrze.cobblesafari.item.ultraloot.ReconArmorItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla hides the cape whenever elytra are worn; the warp ride's rider always shows elytra (see
 * {@link ElytraLayerWarpRideMixin}), so its cape must go too (action plan 189, section 8.2). The cape
 * texture still reaches the wings through the elytra layer's own fallback.
 * <p>The same cancel also hides the cape under an Ultra Recon chestplate, whose backpack it would
 * cross (action plan 192, section 10): one condition more here rather than a second mixin.</p>
 */
@Mixin(CapeLayer.class)
public abstract class CapeLayerWarpRideMixin {

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobblesafari$hideWarpRideCape(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                               AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                                               float partialTicks, float ageInTicks, float netHeadYaw,
                                               float headPitch, CallbackInfo ci) {
        if (player instanceof WarpRideRiderProxy
                || player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ReconArmorItem) {
            ci.cancel();
        }
    }
}
