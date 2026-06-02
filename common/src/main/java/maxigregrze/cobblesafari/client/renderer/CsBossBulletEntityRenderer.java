package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.entity.csboss.CsBossBulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * La bullet n'a pas de modèle (plan 100 § 12.3) : seules ses particules cœur (émises
 * côté client dans son tick) la matérialisent. Le renderer ne dessine rien.
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
