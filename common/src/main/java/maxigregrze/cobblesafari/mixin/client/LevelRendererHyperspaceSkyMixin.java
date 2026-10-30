package maxigregrze.cobblesafari.mixin.client;

import maxigregrze.cobblesafari.client.renderer.HyperspaceSkyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hyperspace sky without a shader pack (action plan 197, section 4.1): drawn in place of the vanilla
 * sky, which is skipped. A thick-fog frame (boss fog) is left to vanilla, which draws no sky either.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererHyperspaceSkyMixin {

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void cobblesafari$hyperspaceSky(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
                                           Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        if (!isFoggy && HyperspaceSkyRenderer.renderInSkyPass(frustumMatrix, projectionMatrix)) {
            ci.cancel();
        }
    }
}
