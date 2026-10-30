package maxigregrze.cobblesafari.mixin.client;

import maxigregrze.cobblesafari.client.renderer.HyperspaceSkyRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Depth copy for the Hyperspace sky overlay (action plan 197, section 4.1), taken right before the
 * translucent terrain layer. Iris hooks the {@code "translucent"} profiler string just above this
 * call to draw the solid hand and start its translucent pass, so the copy holds every opaque thing,
 * hand included, and nothing translucent. Both branches (Fabulous or not) have one such call.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererHyperspaceDepthMixin {

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;translucent()Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private void cobblesafari$capturePreTranslucentDepth(CallbackInfo ci) {
        HyperspaceSkyRenderer.capturePreTranslucentDepth();
    }
}
