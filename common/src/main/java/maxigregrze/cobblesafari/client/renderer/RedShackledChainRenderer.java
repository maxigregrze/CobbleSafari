package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.effect.RedShackledEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class RedShackledChainRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_redchain.png");
    private static final float HALF_SIZE = 1.5f;
    private static final float SPIN_DEGREES_PER_TICK = 3.0f;
    /**
     * Bounding-box extent (in blocks) the default {@link #HALF_SIZE} was tuned for. Entities whose
     * hitbox stays within this size keep the original look; bigger ones scale the chains up so the
     * effect remains visible around them.
     */
    private static final float REFERENCE_BB_SIZE = 2.0f;

    private RedShackledChainRenderer() {}

    public static void renderOnEntity(LivingEntity entity, float partialTick, PoseStack poseStack,
                                      MultiBufferSource bufferSource) {
        if (!RedShackledEffects.isShackled(entity)) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE, false));
        int light = LightTexture.FULL_BRIGHT;
        float spin = (entity.tickCount + partialTick) * SPIN_DEGREES_PER_TICK;
        float halfSize = HALF_SIZE * scaleFor(entity);

        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);

        // Orient the whole frame with the entity's facing so the ±45° tilt axis follows the body yaw
        // (e.g. a mob facing south-east tilts its chains along that diagonal).
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw + 90.0F));

        // Tilt the frame first, then spin around the (now tilted) Y axis, so each chain rotates
        // within its own ±45° plane instead of coning around the world vertical.
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        RedChainBillboardHelper.horizontalQuad(consumer, poseStack.last(), halfSize, light);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-spin));
        RedChainBillboardHelper.horizontalQuad(consumer, poseStack.last(), halfSize, light);
        poseStack.popPose();

        poseStack.popPose();
    }

    /**
     * Returns a multiplier (>= 1.0) for the chain size based on the entity's hitbox. Entities up to
     * {@link #REFERENCE_BB_SIZE} keep the original scale; larger ones scale up proportionally to the
     * biggest hitbox dimension so the chains stay visible. The effect never scales below the default.
     */
    private static float scaleFor(LivingEntity entity) {
        float largestExtent = Math.max(entity.getBbWidth(), entity.getBbHeight());
        return Math.max(1.0F, largestExtent / REFERENCE_BB_SIZE);
    }
}
