package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.entity.safari.SafariBallisticMeteorEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class SafariBallisticMeteorEntityRenderer extends EntityRenderer<SafariBallisticMeteorEntity> {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private final BlockRenderDispatcher blockRenderer;

    public SafariBallisticMeteorEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public ResourceLocation getTextureLocation(SafariBallisticMeteorEntity entity) {
        return FALLBACK;
    }

    @Override
    public void render(SafariBallisticMeteorEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        float spin = entity.getSpin();
        if (spin <= 0.0F) {
            super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
            return;
        }
        BlockState state = ModBlocks.DRACO_METEORITE.defaultBlockState();
        ps.pushPose();
        ps.translate(0.0, 0.5, 0.0);
        if (spin != 0.0F) {
            ps.mulPose(Axis.XP.rotationDegrees(spin));
            ps.mulPose(Axis.YP.rotationDegrees(spin));
            ps.mulPose(Axis.ZP.rotationDegrees(spin));
        }
        ps.translate(-0.5, -0.5, -0.5);
        blockRenderer.renderSingleBlock(state, ps, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
