package maxigregrze.cobblesafari.mixin.client;

import maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhonePcSession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class RotomPhoneScreenMixin {

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
                    shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER
            )
    )
    private void cobblesafari$rotomPhoneBehindPc(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        RotomPhonePcSession.renderBackdropIfNeeded(self, graphics);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void cobblesafari$rotomPhonePcEscape(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        Screen self = (Screen) (Object) this;
        if (RotomPhonePcSession.handleEscapeOnPc(self, keyCode)) {
            cir.setReturnValue(true);
        }
    }
}
