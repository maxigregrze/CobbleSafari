package maxigregrze.cobblesafari.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.client.screen.warpride.WarpRideRiderProxy;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The warp ride's rider always shows open elytra, over whatever armour it wears (action plan 189,
 * section 8.2). The chest stack is swapped right where the layer stores it, before NeoForge's
 * {@code shouldRender} / {@code getElytraTexture} or vanilla's {@code is(Items.ELYTRA)} read it, so
 * the same injection serves both loaders. Any other entity is untouched.
 */
@Mixin(ElytraLayer.class)
public abstract class ElytraLayerWarpRideMixin {

    @ModifyVariable(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("STORE"),
            ordinal = 0
    )
    private ItemStack cobblesafari$warpRideElytra(ItemStack chest, PoseStack poseStack, MultiBufferSource buffer,
                                                  int packedLight, LivingEntity entity, float limbSwing,
                                                  float limbSwingAmount, float partialTicks, float ageInTicks,
                                                  float netHeadYaw, float headPitch) {
        return entity instanceof WarpRideRiderProxy rider ? rider.elytraStack() : chest;
    }
}
