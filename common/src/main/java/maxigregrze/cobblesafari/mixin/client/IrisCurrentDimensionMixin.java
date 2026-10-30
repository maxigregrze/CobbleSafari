package maxigregrze.cobblesafari.mixin.client;

import maxigregrze.cobblesafari.client.renderer.ShaderCompatHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives our End-style dimensions the shader pack's End programs (action plan 197, section 2.1).
 * Only applied when Iris is present ({@code CobbleSafariMixinPlugin}); {@code require = 0} so that an
 * Iris version without this method merely loses the fix.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.Iris", remap = false)
public abstract class IrisCurrentDimensionMixin {

    @Inject(method = "getCurrentDimension", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void cobblesafari$endStyleDimensions(CallbackInfoReturnable<Object> cir) {
        Object override = ShaderCompatHelper.irisEndDimensionOverride(cir.getReturnValue());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
