package maxigregrze.cobblesafari.mixin.client;

import maxigregrze.cobblesafari.client.renderer.CsBossRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the CSBoss attack decals (plan 199, iteration 5) right after the block entity batches
 * (solid / cutout sheets, where mimic blocks land) and before translucent geometry. The outline
 * flush is the one call that sits exactly there, in both the Fabulous and the default pipeline.
 * Should this ever miss, the final {@code endBatch()} still draws them, only later.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererCsBossDecalMixin {

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"
            )
    )
    private void cobblesafari$flushCsBossDecals(CallbackInfo ci) {
        CsBossRenderTypes.flushDecals(Minecraft.getInstance().renderBuffers().bufferSource());
    }
}
