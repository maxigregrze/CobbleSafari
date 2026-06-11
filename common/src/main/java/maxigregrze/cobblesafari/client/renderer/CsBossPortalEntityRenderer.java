package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.attacks.CsBossPortalEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Rend le portail d'invocation (plan 122 § 8.2) : 3 couches plates horizontales (5×5 blocs)
 * empilées, chacune avec sa texture {@code csboss_spawnportal_type{T}_layer{i}}. Mis à l'échelle
 * par {@link CsBossPortalEntity#getAnim()} (ouverture / fermeture) et en rotation Y continue
 * autour de son centre. Couches double‑face, translucides, émissives.
 */
public class CsBossPortalEntityRenderer extends EntityRenderer<CsBossPortalEntity> {

    /** Demi-emprise de base : 5 blocs de côté → ±2,5. */
    private static final float HALF_SIZE = 2.5F;
    /** Facteur de taille du portail (plan 122) : 2 ⇒ emprise rendue 10×10 blocs à pleine ouverture. */
    private static final float PORTAL_SCALE = 2.0F;
    /** Espacement vertical des couches : 2 px ≈ 0,125 bloc (plan 122, réduit de moitié). */
    private static final float LAYER_GAP = 0.125F;
    /** Vitesse de rotation permanente autour de Y. */
    private static final float SPIN_PER_TICK = 1.5F; // degrés / tick
    private static final int LAYERS = 3;

    public CsBossPortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(CsBossPortalEntity entity) {
        return layerTexture(entity.getPortalType(), 1);
    }

    private static ResourceLocation layerTexture(int portalType, int layer) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID,
                "textures/entity/csboss/csboss_spawnportal_type" + portalType + "_layer" + layer + ".png");
    }

    @Override
    public void render(CsBossPortalEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        float open = entity.getAnim();
        if (open <= 0.001F) {
            return; // échelle nulle : rien à dessiner
        }
        float spin = (entity.tickCount + partialTicks) * SPIN_PER_TICK;
        int portalType = entity.getPortalType();
        float halfSize = HALF_SIZE * PORTAL_SCALE; // emprise 2× ; l'espacement des couches reste fixe

        ps.pushPose();
        ps.scale(open, open, open); // animation d'ouverture / fermeture (uniforme)
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        PoseStack.Pose pose = ps.last();

        for (int i = 0; i < LAYERS; i++) {
            float y = i * LAYER_GAP;
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(layerTexture(portalType, i + 1)));
            horizontalQuad(vc, pose, halfSize, y);
        }

        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }

    /** Quad horizontal double‑face (visible du dessus et du dessous) centré sur l'entité. */
    private static void horizontalQuad(VertexConsumer vc, PoseStack.Pose pose, float hs, float y) {
        // Face supérieure (normale +Y).
        v(vc, pose, -hs, y, -hs, 0, 0, 0, 1, 0);
        v(vc, pose, -hs, y, hs, 0, 1, 0, 1, 0);
        v(vc, pose, hs, y, hs, 1, 1, 0, 1, 0);
        v(vc, pose, hs, y, -hs, 1, 0, 0, 1, 0);
        // Face inférieure (normale -Y, ordre inversé).
        v(vc, pose, -hs, y, -hs, 0, 0, 0, -1, 0);
        v(vc, pose, hs, y, -hs, 1, 0, 0, -1, 0);
        v(vc, pose, hs, y, hs, 1, 1, 0, -1, 0);
        v(vc, pose, -hs, y, hs, 0, 1, 0, -1, 0);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                          float u, float w, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, w)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }
}
