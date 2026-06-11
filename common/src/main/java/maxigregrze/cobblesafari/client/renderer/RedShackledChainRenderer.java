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
import net.minecraft.world.entity.LivingEntity;

public final class RedShackledChainRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "textures/entity/csboss/attack_redchain.png");
    private static final float HALF_SIZE = 1.5f;
    private static final float SPIN_DEGREES_PER_TICK = 3.0f;

    private RedShackledChainRenderer() {}

    public static void renderOnEntity(LivingEntity entity, float partialTick, PoseStack poseStack,
                                      MultiBufferSource bufferSource) {
        if (!RedShackledEffects.isShackled(entity)) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE, false));
        int light = LightTexture.FULL_BRIGHT;
        float spin = (entity.tickCount + partialTick) * SPIN_DEGREES_PER_TICK;

        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);

        // Tilt the frame first, then spin around the (now tilted) Y axis, so each chain rotates
        // within its own ±45° plane instead of coning around the world vertical.
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        RedChainBillboardHelper.horizontalQuad(consumer, poseStack.last(), HALF_SIZE, light);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-spin));
        RedChainBillboardHelper.horizontalQuad(consumer, poseStack.last(), HALF_SIZE, light);
        poseStack.popPose();

        poseStack.popPose();
    }
}
