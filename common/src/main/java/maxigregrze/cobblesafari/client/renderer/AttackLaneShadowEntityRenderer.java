package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackLaneShadowEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Ombre couloir : la texture {@code attack_shadow_large} découpée en trois — demi-disque de départ,
 * colonne centrale étirée sur la longueur, demi-disque d'arrivée — pour une bande aux bords doux.
 */
public class AttackLaneShadowEntityRenderer extends EntityRenderer<AttackLaneShadowEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_shadow_large.png");
    private static final float Y = 0.03F;

    public AttackLaneShadowEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackLaneShadowEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(AttackLaneShadowEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        float a = entity.getAlpha();
        if (a > 0.001F) {
            float len = entity.getLength();
            float hw = entity.getWidth() * 0.5F;
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(-entity.getHeading()));
            VertexConsumer vc = buffer.getBuffer(CsBossRenderTypes.decal(TEXTURE));
            PoseStack.Pose pose = ps.last();
            // x = le long du couloir, z = en travers.
            quad(vc, pose, -hw, 0.0F, 0.0F, 0.5F, hw, packedLight, a);
            quad(vc, pose, 0.0F, len, 0.5F, 0.5F, hw, packedLight, a);
            quad(vc, pose, len, len + hw, 0.5F, 1.0F, hw, packedLight, a);
            ps.popPose();
        }
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }

    /** Quad double face de x0 à x1, u de u0 à u1, sur toute la largeur. */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float x0, float x1, float u0, float u1,
                             float hw, int light, float a) {
        v(vc, pose, x0, -hw, u0, 0, light, a);
        v(vc, pose, x0, hw, u0, 1, light, a);
        v(vc, pose, x1, hw, u1, 1, light, a);
        v(vc, pose, x1, -hw, u1, 0, light, a);
        v(vc, pose, x0, -hw, u0, 0, light, a);
        v(vc, pose, x1, -hw, u1, 0, light, a);
        v(vc, pose, x1, hw, u1, 1, light, a);
        v(vc, pose, x0, hw, u0, 1, light, a);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float z, float u, float w, int light, float a) {
        vc.addVertex(pose, x, Y, z)
                .setColor(1.0F, 1.0F, 1.0F, a)
                .setUv(u, w)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
