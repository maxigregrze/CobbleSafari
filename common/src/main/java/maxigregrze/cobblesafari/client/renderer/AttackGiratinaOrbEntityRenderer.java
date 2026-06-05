package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackGiratinaOrbEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Rend l'orbe de Giratina via le modèle/texture du bloc {@code giratina_core_moving}, en pleine
 * luminosité (émissif). La lueur rouge est portée par les particules de l'entité.
 */
public class AttackGiratinaOrbEntityRenderer extends EntityRenderer<AttackGiratinaOrbEntity> {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private final BlockRenderDispatcher blockRenderer;

    public AttackGiratinaOrbEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public ResourceLocation getTextureLocation(AttackGiratinaOrbEntity entity) {
        return FALLBACK;
    }

    @Override
    public void render(AttackGiratinaOrbEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        ps.pushPose();
        ps.translate(-0.5, 0.0, -0.5);
        blockRenderer.renderSingleBlock(ModBlocks.GIRATINA_CORE_MOVING.defaultBlockState(),
                ps, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
