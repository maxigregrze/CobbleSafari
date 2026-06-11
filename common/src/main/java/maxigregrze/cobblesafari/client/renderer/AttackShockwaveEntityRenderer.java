package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShockwaveEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Rendu procédural de l'anneau de choc (plan 124) : bande translucide + bordure opaque, sans texture dédiée.
 */
public class AttackShockwaveEntityRenderer extends EntityRenderer<AttackShockwaveEntity> {

    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final int SEGMENTS = 64;
    // Décollé du sol pour éviter le z-fighting (« clignote dans le sol ») ; la bordure est posée
    // encore au-dessus de la bande pour ne pas se battre avec elle (deux plans coplanaires).
    private static final float FILL_Y = 1.0F / 16.0F;     // 2/32 au-dessus du sol
    private static final float BORDER_Y = 3.0F / 32.0F;   // au-dessus de la bande
    private static final float FILL_ALPHA = 0.35F;
    private static final float BORDER_WIDTH = 0.12F;

    public AttackShockwaveEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackShockwaveEntity entity) {
        return WHITE;
    }

    @Override
    public boolean shouldRender(AttackShockwaveEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(AttackShockwaveEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        float radius = entity.getRadius(partialTicks);
        if (radius <= 0.01F) {
            super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
            return;
        }

        int color = entity.getColorRgb();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        float inner = (float) Math.max(0.0, radius - AttackShockwaveEntity.THICKNESS);
        float outer = radius;

        ps.pushPose();
        PoseStack.Pose pose = ps.last();

        // Sans backface-cull (comme l'ombre) pour rester visible depuis le dessus comme le dessous.
        VertexConsumer fill = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        drawAnnulus(fill, pose, packedLight, FILL_Y, inner, outer, r, g, b, FILL_ALPHA);

        VertexConsumer border = buffer.getBuffer(RenderType.entityTranslucent(WHITE));
        drawAnnulus(border, pose, packedLight, BORDER_Y, outer - BORDER_WIDTH, outer, r, g, b, 1.0F);

        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }

    /**
     * Bande radiale (anneau) entre {@code inner} et {@code outer}, dessinée en <b>quads</b>
     * (un quad complet par segment angulaire — l'ordre triangle-strip produisait un quad sur deux,
     * d'où les « rectangles » manquants).
     */
    private static void drawAnnulus(VertexConsumer vc, PoseStack.Pose pose, int light, float y,
                                    float inner, float outer,
                                    float r, float g, float b, float alpha) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (2.0 * Math.PI * i / SEGMENTS);
            float a1 = (float) (2.0 * Math.PI * (i + 1) / SEGMENTS);
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            colorVertex(vc, pose, inner * c0, y, inner * s0, r, g, b, alpha, light);
            colorVertex(vc, pose, outer * c0, y, outer * s0, r, g, b, alpha, light);
            colorVertex(vc, pose, outer * c1, y, outer * s1, r, g, b, alpha, light);
            colorVertex(vc, pose, inner * c1, y, inner * s1, r, g, b, alpha, light);
        }
    }

    private static void colorVertex(VertexConsumer vc, PoseStack.Pose pose,
                                    float x, float y, float z,
                                    float r, float g, float b, float a, int light) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
