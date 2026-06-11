package maxigregrze.cobblesafari.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import maxigregrze.cobblesafari.client.effect.RedShackledScreenOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererRedShackledMixin {

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void cobblesafari$redShackledVignette(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci,
                                                  @Local GuiGraphics guiGraphics) {
        RedShackledScreenOverlay.renderVanillaStyle(guiGraphics, 1.0F);
    }
}
