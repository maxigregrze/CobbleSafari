package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackAirSlashEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Lame d'Air ({@code base_flying_2}) : quad horizontal 2×2 texturé ({@code attack_airslash.png},
 * fourni par Robin), orienté selon la direction de vol et tournoyant sur lui-même.
 */
public class AttackAirSlashEntityRenderer extends EntityRenderer<AttackAirSlashEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_airslash.png");
    private static final float HALF_SIZE = 1.0F;
    private static final float CENTER_Y = 0.25F;
    private static final float SPIN_DEGREES_PER_TICK = 24.0F;

    public AttackAirSlashEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackAirSlashEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(AttackAirSlashEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        ps.pushPose();
        ps.translate(0.0F, CENTER_Y, 0.0F);
        ps.mulPose(Axis.YP.rotationDegrees(-entity.getHeading()));
        ps.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTicks) * SPIN_DEGREES_PER_TICK));
        VertexConsumer vc = buffer.getBuffer(CsBossRenderTypes.decal(TEXTURE));
        PoseStack.Pose pose = ps.last();
        // Quad horizontal double face (plan XZ).
        v(vc, pose, -HALF_SIZE, -HALF_SIZE, 0, 0, 1.0F);
        v(vc, pose, -HALF_SIZE, HALF_SIZE, 0, 1, 1.0F);
        v(vc, pose, HALF_SIZE, HALF_SIZE, 1, 1, 1.0F);
        v(vc, pose, HALF_SIZE, -HALF_SIZE, 1, 0, 1.0F);
        v(vc, pose, HALF_SIZE, -HALF_SIZE, 1, 0, -1.0F);
        v(vc, pose, HALF_SIZE, HALF_SIZE, 1, 1, -1.0F);
        v(vc, pose, -HALF_SIZE, HALF_SIZE, 0, 1, -1.0F);
        v(vc, pose, -HALF_SIZE, -HALF_SIZE, 0, 0, -1.0F);
        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float z, float u, float w, float ny) {
        vc.addVertex(pose, x, 0.0F, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, w)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, ny, 0.0F);
    }
}
