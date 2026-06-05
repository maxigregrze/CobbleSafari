package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackWaveEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Rend la vague (plan 110) d'après {@code vertical_wave.json} : un mur vertical 3 blocs de large
 * (face principale + crête repliée) orienté selon le cap de l'entité (sa direction de déplacement).
 */
public class AttackWaveEntityRenderer extends EntityRenderer<AttackWaveEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_wave.png");

    // Mur centré : x ∈ [-1.5, 1.5] (3 large), face principale y ∈ [-1, 1.625], crête repliée jusqu'à ~2.
    private static final float HW = 1.5f;
    private static final float Y_BOTTOM = -1.0f;
    private static final float Y_MAIN_TOP = 1.625f;
    private static final float CREST_TOP_Y = 1.971f;
    private static final float CREST_TOP_Z = -0.143f;
    private static final float V_MAIN_TOP = 0.125f; // uv [0,2,16,16] → v = 2/16

    public AttackWaveEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackWaveEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(AttackWaveEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        ps.pushPose();
        // Oriente le mur selon le cap de l'entité (yRot = atan2(dirX, dirZ), cf. WaterWaveAttack),
        // +180° pour mettre l'avant du modèle dans le sens de déplacement.
        ps.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
        PoseStack.Pose pose = ps.last();

        // Face principale (double-face), v de 1 (bas) à 0.125 (haut).
        quad(vc, pose, packedLight,
                -HW, Y_BOTTOM, 0, 0, 1,
                HW, Y_BOTTOM, 0, 1, 1,
                HW, Y_MAIN_TOP, 0, 1, V_MAIN_TOP,
                -HW, Y_MAIN_TOP, 0, 0, V_MAIN_TOP);
        // Crête repliée, v de 0.125 (bas) à 0 (haut).
        quad(vc, pose, packedLight,
                -HW, Y_MAIN_TOP, 0, 0, V_MAIN_TOP,
                HW, Y_MAIN_TOP, 0, 1, V_MAIN_TOP,
                HW, CREST_TOP_Y, CREST_TOP_Z, 1, 0,
                -HW, CREST_TOP_Y, CREST_TOP_Z, 0, 0);

        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }

    /** Quad double-face (avant + arrière) avec UV, opacité pleine. */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float x4, float y4, float z4, float u4, float v4) {
        v(vc, pose, x1, y1, z1, u1, v1, light);
        v(vc, pose, x2, y2, z2, u2, v2, light);
        v(vc, pose, x3, y3, z3, u3, v3, light);
        v(vc, pose, x4, y4, z4, u4, v4, light);
        // arrière (ordre inversé)
        v(vc, pose, x4, y4, z4, u4, v4, light);
        v(vc, pose, x3, y3, z3, u3, v3, light);
        v(vc, pose, x2, y2, z2, u2, v2, light);
        v(vc, pose, x1, y1, z1, u1, v1, light);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                          float u, float w, int light) {
        vc.addVertex(pose, x, y, z)
                .setColor(1.0f, 1.0f, 1.0f, 0.85f)
                .setUv(u, w)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 0.0f, 1.0f);
    }
}
