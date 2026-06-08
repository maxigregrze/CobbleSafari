package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.entity.csboss.CsBossBulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The bullet has no model (plan 100 § 12.3): only its core particles (emitted
 * client-side in its tick) materialize it. The renderer draws nothing.
 */
public class CsBossBulletEntityRenderer extends EntityRenderer<CsBossBulletEntity> {

    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public CsBossBulletEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(CsBossBulletEntity entity) {
        return FALLBACK_TEXTURE;
    }

    @Override
    public void render(CsBossBulletEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        // no-op
    }
}
