package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDigdirtEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renders the dirt pile via the {@code attack_digdirt} block model (plan 126 : scale + vibration). */
public class AttackDigdirtEntityRenderer extends EntityRenderer<AttackDigdirtEntity> {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private final BlockRenderDispatcher blockRenderer;

    public AttackDigdirtEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public ResourceLocation getTextureLocation(AttackDigdirtEntity entity) {
        return FALLBACK;
    }

    @Override
    public void render(AttackDigdirtEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        float alpha = entity.getAlpha();
        if (alpha <= 0.01F) {
            super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
            return;
        }

        float scale = entity.getRenderScale();
        ps.pushPose();
        ps.translate(0.0, 0.5, 0.0);
        if (entity.isVibrating()) {
            double t = entity.tickCount + partialTicks;
            ps.translate(Math.sin(t * 1.7) * 0.05, 0.0, Math.cos(t * 2.1) * 0.05);
        }
        if (scale != 1.0F) {
            ps.scale(scale, scale, scale);
        }
        ps.translate(-0.5, -0.5, -0.5);
        var display = entity.usesDirtModel()
                ? ModBlocks.ATTACK_DIGDIRT_DIRT_DISPLAY
                : ModBlocks.ATTACK_DIGDIRT_DISPLAY;
        blockRenderer.renderSingleBlock(display.defaultBlockState(),
                ps, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
