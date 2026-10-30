package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.model.AttackBellModel;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackBellEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * Cloche de {@code jump_1} : modèle de Robin ×4 (corps 2×2×2 blocs), balancée autour du centre de
 * la face supérieure du corps, assombrie à chaque coup reçu.
 */
public class AttackBellEntityRenderer extends EntityRenderer<AttackBellEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_bell.png");
    private static final float SCALE = 4.0F;
    /** Sommet du corps 8×8×8 : 8 px au-dessus des pieds, ×4. */
    private static final float PIVOT_Y = 8.0F / 16.0F * SCALE;
    private static final float SWING_DEGREES = 25.0F;
    private static final float SWING_SPEED = 0.25F;

    private final AttackBellModel model;

    public AttackBellEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new AttackBellModel(context.bakeLayer(AttackBellModel.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(AttackBellEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(AttackBellEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        ps.pushPose();
        float t = entity.tickCount + partialTicks;
        ps.translate(0.0F, PIVOT_Y, 0.0F);
        ps.mulPose(Axis.ZP.rotationDegrees(SWING_DEGREES * Mth.sin(t * SWING_SPEED)));
        ps.translate(0.0F, -PIVOT_Y, 0.0F);
        ps.scale(SCALE, SCALE, SCALE);
        // Repère modèle d'entité vanilla (Y vers le bas, origine 24 px au-dessus des pieds).
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0F, -1.501F, 0.0F);
        int shade = 255 - entity.getHits() * 50;
        int color = FastColor.ARGB32.color(255, shade, shade, shade);
        model.renderToBuffer(ps, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight,
                OverlayTexture.NO_OVERLAY, color);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }
}
