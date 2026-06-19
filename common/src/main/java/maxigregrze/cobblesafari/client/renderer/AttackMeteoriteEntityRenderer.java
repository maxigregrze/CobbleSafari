package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackMeteoriteEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the falling meteorite via the {@code meteorite} or
 * {@code draco_meteorite} block model depending on variant, centered on the entity.
 */
public class AttackMeteoriteEntityRenderer extends EntityRenderer<AttackMeteoriteEntity> {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private final BlockRenderDispatcher blockRenderer;

    public AttackMeteoriteEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public ResourceLocation getTextureLocation(AttackMeteoriteEntity entity) {
        return FALLBACK;
    }

    @Override
    public void render(AttackMeteoriteEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        BlockState state = (entity.isDraco() ? ModBlocks.DRACO_METEORITE : ModBlocks.METEORITE).defaultBlockState();
        float scale = entity.getRenderScale();
        float spin = entity.getSpin();
        ps.pushPose();
        ps.translate(0.0, 0.5, 0.0); // pivot around block center
        if (spin != 0.0F) {
            ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(spin));
            ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spin));
            ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(spin));
        }
        if (scale != 1.0F) {
            ps.scale(scale, scale, scale);
        }
        ps.translate(-0.5, -0.5, -0.5); // re-center the model [0,1]³
        blockRenderer.renderSingleBlock(state, ps, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
