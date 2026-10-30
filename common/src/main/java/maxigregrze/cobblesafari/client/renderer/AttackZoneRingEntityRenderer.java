package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackZoneRingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Zone statique : bande translucide + liseré extérieur, recette de {@link AttackShockwaveEntityRenderer}
 * (mêmes hauteurs anti z-fighting), opacité pilotée par l'entité.
 */
public class AttackZoneRingEntityRenderer extends EntityRenderer<AttackZoneRingEntity> {

    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final int SEGMENTS = 96;
    private static final float FILL_Y = 1.0F / 16.0F;
    private static final float BORDER_Y = 3.0F / 32.0F;
    private static final float BORDER_WIDTH = 0.12F;

    public AttackZoneRingEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackZoneRingEntity entity) {
        return WHITE;
    }

    @Override
    public void render(AttackZoneRingEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        float outer = entity.getOuter();
        float inner = Math.max(0.0F, entity.getInner());
        float alpha = Mth.clamp(entity.getAlpha(), 0.0F, 1.0F);
        if (outer <= inner || alpha <= 0.01F) {
            return;
        }
        int color = entity.getColorRgb();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        PoseStack.Pose pose = ps.last();
        VertexConsumer vc = buffer.getBuffer(CsBossRenderTypes.decal(WHITE));
        drawAnnulus(vc, pose, packedLight, FILL_Y, inner, outer, r, g, b, alpha);
        drawAnnulus(vc, pose, packedLight, BORDER_Y, Math.max(inner, outer - BORDER_WIDTH), outer,
                r, g, b, Math.min(1.0F, alpha * 1.8F));
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }

    private static void drawAnnulus(VertexConsumer vc, PoseStack.Pose pose, int light, float y,
                                    float inner, float outer, float r, float g, float b, float alpha) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (2.0 * Math.PI * i / SEGMENTS);
            float a1 = (float) (2.0 * Math.PI * (i + 1) / SEGMENTS);
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            vertex(vc, pose, inner * c0, y, inner * s0, r, g, b, alpha, light);
            vertex(vc, pose, outer * c0, y, outer * s0, r, g, b, alpha, light);
            vertex(vc, pose, outer * c1, y, outer * s1, r, g, b, alpha, light);
            vertex(vc, pose, inner * c1, y, inner * s1, r, g, b, alpha, light);
            // sens inverse : le type « decal » cull les faces arrière
            vertex(vc, pose, inner * c1, y, inner * s1, r, g, b, alpha, light);
            vertex(vc, pose, outer * c1, y, outer * s1, r, g, b, alpha, light);
            vertex(vc, pose, outer * c0, y, outer * s0, r, g, b, alpha, light);
            vertex(vc, pose, inner * c0, y, inner * s0, r, g, b, alpha, light);
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                               float r, float g, float b, float a, int light) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
