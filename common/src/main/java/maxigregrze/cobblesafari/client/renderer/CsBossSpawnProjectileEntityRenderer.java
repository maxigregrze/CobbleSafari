package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.entity.csboss.attacks.CsBossSpawnProjectileEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Rend le projectile d'invocation (plan 122 § 8.1) via le modèle de bloc {@code bossanchor_moving},
 * centré sur l'entité, en rotation continue (effet toupie) pendant la montée.
 */
public class CsBossSpawnProjectileEntityRenderer extends EntityRenderer<CsBossSpawnProjectileEntity> {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final float SPIN_PER_TICK = 18.0F; // degrés / tick autour de Y

    private final BlockRenderDispatcher blockRenderer;

    public CsBossSpawnProjectileEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public ResourceLocation getTextureLocation(CsBossSpawnProjectileEntity entity) {
        return FALLBACK;
    }

    @Override
    public void render(CsBossSpawnProjectileEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        float spin = (entity.tickCount + partialTicks) * SPIN_PER_TICK;
        ps.pushPose();
        ps.translate(0.0, 0.5, 0.0); // pivot autour du centre du bloc
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        ps.translate(-0.5, -0.5, -0.5); // recentre le modèle [0,1]³
        blockRenderer.renderSingleBlock(ModBlocks.BOSSANCHOR_MOVING.defaultBlockState(),
                ps, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
