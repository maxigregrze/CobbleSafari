package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Shared horizontal (XZ-plane) billboard quads for red chain visuals.
 */
public final class RedChainBillboardHelper {

    private RedChainBillboardHelper() {}

    public static void renderHorizontalSpinningQuad(VertexConsumer vc, PoseStack ps, float halfSize,
                                                  float spinDegrees, int light) {
        ps.pushPose();
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spinDegrees));
        PoseStack.Pose pose = ps.last();
        horizontalQuad(vc, pose, halfSize, light);
        ps.popPose();
    }

    public static void horizontalQuad(VertexConsumer vc, PoseStack.Pose pose, float halfSize, int light) {
        quadTop(vc, pose, halfSize, light);
        quadBottom(vc, pose, halfSize, light);
    }

    private static void quadTop(VertexConsumer vc, PoseStack.Pose pose, float halfSize, int light) {
        v(vc, pose, -halfSize, 0, -halfSize, 0, 0, light);
        v(vc, pose, -halfSize, 0, halfSize, 0, 1, light);
        v(vc, pose, halfSize, 0, halfSize, 1, 1, light);
        v(vc, pose, halfSize, 0, -halfSize, 1, 0, light);
    }

    private static void quadBottom(VertexConsumer vc, PoseStack.Pose pose, float halfSize, int light) {
        v(vc, pose, -halfSize, 0, -halfSize, 0, 0, light);
        v(vc, pose, halfSize, 0, -halfSize, 1, 0, light);
        v(vc, pose, halfSize, 0, halfSize, 1, 1, light);
        v(vc, pose, -halfSize, 0, halfSize, 0, 1, light);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                          float u, float v, int light) {
        vc.addVertex(pose, x, y, z)
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
