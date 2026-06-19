package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDistortionStemCoreEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The distortion core is invisible: logical propagation node, no rendering.
 */
public class AttackDistortionStemCoreEntityRenderer extends EntityRenderer<AttackDistortionStemCoreEntity> {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public AttackDistortionStemCoreEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackDistortionStemCoreEntity entity) {
        return FALLBACK;
    }

    @Override
    public void render(AttackDistortionStemCoreEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        // no-op
    }
}
