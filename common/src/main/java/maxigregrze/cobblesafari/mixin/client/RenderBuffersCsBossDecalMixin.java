package maxigregrze.cobblesafari.mixin.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import maxigregrze.cobblesafari.client.renderer.CsBossRenderTypes;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.SequencedMap;

/**
 * CSBoss attack decals become fixed buffers of the main buffer source (plan 199, iteration 5): a
 * non-fixed type is drawn as soon as another type is requested, i.e. during the entity pass, before
 * block entities. Mimic blocks are drawn by their block entity renderer, so they painted over the
 * depth-less decals. Fixed, the decals wait for {@link LevelRendererCsBossDecalMixin}.
 */
@Mixin(RenderBuffers.class)
public abstract class RenderBuffersCsBossDecalMixin {

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;immediateWithBuffers(Ljava/util/SequencedMap;Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;)Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
            ),
            index = 0
    )
    private SequencedMap<RenderType, ByteBufferBuilder> cobblesafari$addDecalBuffers(
            SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers) {
        CsBossRenderTypes.registerDecalBuffers(fixedBuffers);
        return fixedBuffers;
    }

}
