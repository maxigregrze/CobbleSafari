package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackShadowEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the attack shadow as a textured horizontal quad, parallel to the
 * ground, ~½ pixel above the ground, visible from both sides.
 */
public class AttackShadowEntityRenderer extends EntityRenderer<AttackShadowEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_shadow.png");
    private static final ResourceLocation TEXTURE_LARGE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_shadow_large.png");

    private static final float Y = 0.03f; // ~½ pixel above the ground

    public AttackShadowEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackShadowEntity entity) {
        return entity.isLarge() ? TEXTURE_LARGE : TEXTURE;
    }

    @Override
    public void render(AttackShadowEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        ResourceLocation tex = entity.isLarge() ? TEXTURE_LARGE : TEXTURE;
        float half = entity.isLarge() ? 1.5f: 0.5f; // 3×3 or 1×1
        float a = entity.getAlpha();
        if (a > 0.001f) {
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(tex));
            PoseStack.Pose pose = ps.last();
            int overlay = OverlayTexture.NO_OVERLAY;
            quad(vc, pose, packedLight, overlay, a, half);
            quadDown(vc, pose, packedLight, overlay, a, half);
        }
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, float a, float h) {
        v(vc, pose, -h, Y, -h, 0, 0, light, overlay, a);
        v(vc, pose, -h, Y, h, 0, 1, light, overlay, a);
        v(vc, pose, h, Y, h, 1, 1, light, overlay, a);
        v(vc, pose, h, Y, -h, 1, 0, light, overlay, a);
    }

    private static void quadDown(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, float a, float h) {
        v(vc, pose, -h, Y, -h, 0, 0, light, overlay, a);
        v(vc, pose, h, Y, -h, 1, 0, light, overlay, a);
        v(vc, pose, h, Y, h, 1, 1, light, overlay, a);
        v(vc, pose, -h, Y, h, 0, 1, light, overlay, a);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                          float u, float w, int light, int overlay, float a) {
        vc.addVertex(pose, x, y, z)
                .setColor(1.0f, 1.0f, 1.0f, a)
                .setUv(u, w)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
