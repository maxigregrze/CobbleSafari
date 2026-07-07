package maxigregrze.cobblesafari.compat.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RotomEarpieceAccessoryRenderer implements AccessoryRenderer {

    @Override
    public <M extends LivingEntity> void render(
            ItemStack stack,
            SlotReference reference,
            PoseStack poseStack,
            EntityModel<M> model,
            MultiBufferSource multiBufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        LivingEntity entity = reference.entity();

        poseStack.pushPose();

        // 1) Move into head space so the earpiece follows the player's head rotation.
        if (model instanceof HumanoidModel<?> humanoid) {
            humanoid.head.translateAndRotate(poseStack);
        }

        // 2) Earpiece-specific placement (starting values — calibrate in-game):
        //    right side of the head (left when viewed from the front), tilted 45° around X then
        //    90° around Y, at half size.
        poseStack.translate(0.32f, -0.25f, 0.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(135));
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.scale(0.5f, 0.5f, 0.5f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                poseStack, multiBufferSource, entity.level(), 0);

        poseStack.popPose();
    }
}
