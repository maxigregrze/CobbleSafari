package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackPileProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Flying sludge/mud cube. */
public class AttackPileProjectileEntityRenderer extends EntityRenderer<AttackPileProjectileEntity> {

    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final float CUBE_SCALE = 0.5F;

    private final BlockRenderDispatcher blockRenderer;

    public AttackPileProjectileEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public ResourceLocation getTextureLocation(AttackPileProjectileEntity entity) {
        return FALLBACK;
    }

    @Override
    public void render(AttackPileProjectileEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        BlockState state = entity.getKind().displayBlock().defaultBlockState();
        float spin = (entity.tickCount + partialTicks) * 12.0F;
        ps.pushPose();
        ps.translate(0.0, 0.5, 0.0);
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        ps.mulPose(Axis.XP.rotationDegrees(spin * 0.7F));
        ps.scale(CUBE_SCALE, CUBE_SCALE, CUBE_SCALE);
        ps.translate(-0.5, -0.5, -0.5);
        blockRenderer.renderSingleBlock(state, ps, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
