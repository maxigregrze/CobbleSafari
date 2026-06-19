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
 * Renders the shadow ball ({@code base_ghost_4}): a flat 2×2 quad always camera-facing
 * (like mud / bait), slowly spinning on its forward axis (roll in the screen plane).
 */
public class AttackShadowballEntityRenderer extends EntityRenderer<AttackShadowballEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_shadowball.png");
    private static final float HALF_SIZE = 1.0f; // quad 2×2
    private static final float CENTER_Y = 1.0f; // center the visual on the 2-block-tall box
    private static final float SPIN_DEGREES_PER_TICK = 4.0f; // slow rotation on the forward axis

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
        // Camera-facing (billboard), then roll around the forward axis (Z after camera orientation).
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float spin = (entity.tickCount + partialTicks) * SPIN_DEGREES_PER_TICK;
        ps.mulPose(Axis.ZP.rotationDegrees(spin));

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = ps.last();
        quad(vc, pose, LightTexture.FULL_BRIGHT);
        ps.popPose();

        super.render(entity, yaw, partialTicks, ps, buffer, packedLight);
    }

    /** Double-sided quad in the XY plane (z=0), centered on the origin. */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light) {
        v(vc, pose, -HALF_SIZE, -HALF_SIZE, 0, 1, light);
        v(vc, pose, HALF_SIZE, -HALF_SIZE, 1, 1, light);
        v(vc, pose, HALF_SIZE, HALF_SIZE, 1, 0, light);
        v(vc, pose, -HALF_SIZE, HALF_SIZE, 0, 0, light);
        // back face (reversed winding)
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
