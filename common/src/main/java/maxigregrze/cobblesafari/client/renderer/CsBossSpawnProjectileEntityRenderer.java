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
 * Renders the summon projectile via the {@code bossanchor_moving} block model,
 * centered on the entity, spinning continuously (top effect) during ascent.
 */
public class CsBossSpawnProjectileEntityRenderer extends EntityRenderer<CsBossSpawnProjectileEntity> {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final float SPIN_PER_TICK = 18.0F; // degrees / tick around Y

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
        ps.translate(0.0, 0.5, 0.0); // pivot around block center
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        ps.translate(-0.5, -0.5, -0.5); // recenter the [0,1]³ model
        blockRenderer.renderSingleBlock(ModBlocks.BOSSANCHOR_MOVING.defaultBlockState(),
                ps, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
