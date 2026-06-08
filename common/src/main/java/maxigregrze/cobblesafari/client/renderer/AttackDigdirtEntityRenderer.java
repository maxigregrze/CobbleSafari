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

/** Renders the dirt pile via the {@code attack_digdirt} block model (meteorite texture). */
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
        ps.pushPose();
        ps.translate(-0.5, 0.0, -0.5);
        blockRenderer.renderSingleBlock(ModBlocks.ATTACK_DIGDIRT_DISPLAY.defaultBlockState(),
                ps, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
