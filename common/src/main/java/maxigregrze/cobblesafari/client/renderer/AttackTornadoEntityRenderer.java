package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackTornadoEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tornade ({@code base_flying_4}) : le modèle {@code whirlwind_display} (comme
 * {@link WhirlwindBlockEntityRenderer}) empilé sur 3 étages d'échelles croissantes, chacun tournant
 * sur Y avec un décalage de phase. Se réduit pendant le fondu de fin (alpha synchronisé).
 */
public class AttackTornadoEntityRenderer extends EntityRenderer<AttackTornadoEntity> {

    private static final float[] TIER_SCALES = {1.2F, 1.6F, 2.0F};
    private static final float TIER_PHASE_DEGREES = 40.0F;
    private static final float SPIN_DEGREES_PER_TICK = 18.0F;

    private final BlockRenderDispatcher blockRenderer;

    public AttackTornadoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(AttackTornadoEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(AttackTornadoEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        float alpha = entity.getAlpha();
        if (alpha <= 0.01F) {
            return;
        }
        BlockState state = ModBlocks.WHIRLWIND_DISPLAY.defaultBlockState();
        float t = entity.tickCount + partialTicks;
        float y = 0.0F;
        for (int i = 0; i < TIER_SCALES.length; i++) {
            float scale = TIER_SCALES[i] * alpha;
            ps.pushPose();
            ps.translate(0.0F, y, 0.0F);
            ps.mulPose(Axis.YP.rotationDegrees(t * SPIN_DEGREES_PER_TICK + i * TIER_PHASE_DEGREES));
            ps.scale(scale, scale, scale);
            ps.translate(-0.5F, 0.0F, -0.5F);
            blockRenderer.renderSingleBlock(state, ps, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            ps.popPose();
            y += scale;
        }
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
