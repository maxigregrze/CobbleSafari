package maxigregrze.cobblesafari.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import maxigregrze.cobblesafari.client.renderer.HyperspaceSkyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hyperspace sky under a shader pack (action plan 197, section 4.1). Iris finishes its pipeline
 * when {@code LevelRenderer.renderLevel} returns, so drawing right after that call lands on the
 * pack's final image, with the scene depth still in the main target.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererHyperspaceOverlayMixin {

    @WrapOperation(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"
            )
    )
    private void cobblesafari$hyperspaceSkyAfterShaderPipeline(
            LevelRenderer levelRenderer,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            Operation<Void> original
    ) {
        original.call(levelRenderer, deltaTracker, renderBlockOutline, camera, gameRenderer, lightTexture,
                frustumMatrix, projectionMatrix);
        HyperspaceSkyRenderer.renderAfterShaderPipeline(frustumMatrix, projectionMatrix);
    }
}
