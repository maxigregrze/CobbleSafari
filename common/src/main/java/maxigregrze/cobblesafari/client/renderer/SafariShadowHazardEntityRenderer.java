package maxigregrze.cobblesafari.client.renderer;

import maxigregrze.cobblesafari.entity.safari.SafariShadowHazardEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Invisible driver entity; the shadow is rendered by {@link AttackShadowEntityRenderer}. */
public class SafariShadowHazardEntityRenderer extends EntityRenderer<SafariShadowHazardEntity> {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public SafariShadowHazardEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SafariShadowHazardEntity entity) {
        return FALLBACK;
    }
}
