package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackDistortionStemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the distortion stem (plan 107 § 4.3) as a cross of 4 vertical planes (see
 * {@code distortion_stem.json}), semi-transparent (alpha 0.5), double-sided.
 */
public class AttackDistortionStemEntityRenderer extends EntityRenderer<AttackDistortionStemEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_distortion_stem.png");

    private static final float A = 5.0f / 16.0f;
    private static final float B = 11.0f / 16.0f;
    private static final float ALPHA = 0.5f;

    public AttackDistortionStemEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackDistortionStemEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(AttackDistortionStemEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        ps.pushPose();
        if (entity.isVertical()) {
            // Wall: vertical stem (pointing up), no tilt.
            ps.translate(-0.5, 0.0, -0.5);
        } else {
            // Rotate the model 90°: stem points radially (outward from the boss).
            ps.mulPose(Axis.YP.rotationDegrees(-yaw));
            ps.mulPose(Axis.XP.rotationDegrees(90.0F));
            ps.translate(-0.5, -0.5, -0.5);
        }
        PoseStack.Pose pose = ps.last();

        // Planes perpendicular to Z (at z = A and z = B), double-sided.
        planeZ(vc, pose, packedLight, A);
        planeZ(vc, pose, packedLight, B);
        // Planes perpendicular to X (at x = A and x = B), double-sided.
        planeX(vc, pose, packedLight, A);
        planeX(vc, pose, packedLight, B);

        ps.popPose();
        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }

    private static void planeZ(VertexConsumer vc, PoseStack.Pose pose, int light, float z) {
        // face +Z
        v(vc, pose, 0, 0, z, 0, 1, light, 0, 0, 1);
        v(vc, pose, 1, 0, z, 1, 1, light, 0, 0, 1);
        v(vc, pose, 1, 1, z, 1, 0, light, 0, 0, 1);
        v(vc, pose, 0, 1, z, 0, 0, light, 0, 0, 1);
        // face -Z
        v(vc, pose, 0, 0, z, 0, 1, light, 0, 0, -1);
        v(vc, pose, 0, 1, z, 0, 0, light, 0, 0, -1);
        v(vc, pose, 1, 1, z, 1, 0, light, 0, 0, -1);
        v(vc, pose, 1, 0, z, 1, 1, light, 0, 0, -1);
    }

    private static void planeX(VertexConsumer vc, PoseStack.Pose pose, int light, float x) {
        // face +X
        v(vc, pose, x, 0, 0, 0, 1, light, 1, 0, 0);
        v(vc, pose, x, 1, 0, 0, 0, light, 1, 0, 0);
        v(vc, pose, x, 1, 1, 1, 0, light, 1, 0, 0);
        v(vc, pose, x, 0, 1, 1, 1, light, 1, 0, 0);
        // face -X
        v(vc, pose, x, 0, 0, 0, 1, light, -1, 0, 0);
        v(vc, pose, x, 0, 1, 1, 1, light, -1, 0, 0);
        v(vc, pose, x, 1, 1, 1, 0, light, -1, 0, 0);
        v(vc, pose, x, 1, 0, 0, 0, light, -1, 0, 0);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                          float u, float w, int light, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(1.0f, 1.0f, 1.0f, ALPHA)
                .setUv(u, w)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
