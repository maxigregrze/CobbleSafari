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
 * Renders the Giratina orb via the {@code giratina_core_moving} block model/texture at full
 * brightness (emissive). The red glow is driven by the entity's particles.
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
        // Rotate around the block centre so the orb faces the player it follows.
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
        ps.translate(-0.5, 0.0, -0.5);
        blockRenderer.renderSingleBlock(ModBlocks.GIRATINA_CORE_MOVING.defaultBlockState(),
                ps, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
