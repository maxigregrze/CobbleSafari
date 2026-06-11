package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowballEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Rend la boule d'ombre ({@code base_ghost_4}) : un quad plat 2×2 toujours orienté face caméra
 * (comme une boue / un appât), qui tourne lentement sur son axe avant (roulis dans le plan écran).
 */
public class AttackShadowballEntityRenderer extends EntityRenderer<AttackShadowballEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_shadowball.png");
    private static final float HALF_SIZE = 1.0f;             // quad 2×2
    private static final float CENTER_Y = 1.0f;              // centre le visuel sur la boîte 2 de haut
    private static final float SPIN_DEGREES_PER_TICK = 4.0f; // rotation lente sur l'axe avant

    public AttackShadowballEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackShadowballEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(AttackShadowballEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        ps.pushPose();
        ps.translate(0.0, CENTER_Y, 0.0);
        // Face caméra (billboard), puis roulis autour de l'axe avant (Z après orientation caméra).
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float spin = (entity.tickCount + partialTicks) * SPIN_DEGREES_PER_TICK;
        ps.mulPose(Axis.ZP.rotationDegrees(spin));

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = ps.last();
        quad(vc, pose, LightTexture.FULL_BRIGHT);
        ps.popPose();

        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }

    /** Quad double face dans le plan XY (z=0), centré sur l'origine. */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light) {
        v(vc, pose, -HALF_SIZE, -HALF_SIZE, 0, 1, light);
        v(vc, pose, HALF_SIZE, -HALF_SIZE, 1, 1, light);
        v(vc, pose, HALF_SIZE, HALF_SIZE, 1, 0, light);
        v(vc, pose, -HALF_SIZE, HALF_SIZE, 0, 0, light);
        // dos (winding inversé)
        v(vc, pose, -HALF_SIZE, HALF_SIZE, 0, 0, light);
        v(vc, pose, HALF_SIZE, HALF_SIZE, 1, 0, light);
        v(vc, pose, HALF_SIZE, -HALF_SIZE, 1, 1, light);
        v(vc, pose, -HALF_SIZE, -HALF_SIZE, 0, 1, light);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float w, int light) {
        vc.addVertex(pose, x, y, 0.0f)
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setUv(u, w)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 0.0f, 1.0f);
    }
}
