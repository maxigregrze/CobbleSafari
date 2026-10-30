package maxigregrze.cobblesafari.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.item.RotomPhoneItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns a Rotom device round in the totem-style item flash (action plan 198, part H): in the
 * {@code FIXED} display the flash uses, the model would otherwise show its back. The half turn goes
 * last, after vanilla's spin and wobble, so it turns the model about its own vertical axis.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererRotomPhoneActivationMixin {

    @Shadow
    private ItemStack itemActivationItem;

    @Inject(
            method = "renderItemActivationAnimation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawManaged(Ljava/lang/Runnable;)V"
            )
    )
    private void cobblesafari$turnRotomDevice(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci,
                                              @Local PoseStack poseStack) {
        if (itemActivationItem != null && itemActivationItem.getItem() instanceof RotomPhoneItem) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }
    }
}
