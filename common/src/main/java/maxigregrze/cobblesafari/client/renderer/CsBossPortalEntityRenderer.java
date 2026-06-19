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
 * Renders the summon portal: 3 stacked horizontal flat layers (5×5 blocks),
 * each with its own {@code csboss_spawnportal_type{T}_layer{i}} texture. Scaled by
 * {@link CsBossPortalEntity#getAnim()} (opening / closing) and continuously rotated around Y
 * at its center. Double-sided, translucent, emissive layers.
 */
public class CsBossPortalEntityRenderer extends EntityRenderer<CsBossPortalEntity> {

    /** Base half-extent: 5 blocks per side → ±2.5. */
    private static final float HALF_SIZE = 2.5F;
    /** Vertical layer spacing: 2 px ≈ 0.125 block (halved). */
    private static final float LAYER_GAP = 0.5F;
    /** Permanent rotation speed around Y. */
    private static final float SPIN_PER_TICK = 1.5F; // degrees / tick
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
            return; // zero scale: nothing to draw
        }
        float spin = (entity.tickCount + partialTicks) * SPIN_PER_TICK;
        int portalType = entity.getPortalType();
        float halfSize = HALF_SIZE * entity.getPortalSize();

        ps.pushPose();
        ps.scale(open, open, open); // opening / closing animation (uniform)
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

    /** Double-sided horizontal quad (visible from above and below) centered on the entity. */
    private static void horizontalQuad(VertexConsumer vc, PoseStack.Pose pose, float hs, float y) {
        // Top face (normal +Y).
        v(vc, pose, -hs, y, -hs, 0, 0, 0, 1, 0);
        v(vc, pose, -hs, y, hs, 0, 1, 0, 1, 0);
        v(vc, pose, hs, y, hs, 1, 1, 0, 1, 0);
        v(vc, pose, hs, y, -hs, 1, 0, 0, 1, 0);
        // Bottom face (normal -Y, reversed order).
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
