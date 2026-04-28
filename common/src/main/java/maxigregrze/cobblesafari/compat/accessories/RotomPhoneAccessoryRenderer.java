package maxigregrze.cobblesafari.compat.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RotomPhoneAccessoryRenderer implements AccessoryRenderer {

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

        poseStack.translate(0.75f, -0.25f, 0.0f);

        poseStack.mulPose(Axis.XP.rotationDegrees(180));

        poseStack.scale(1.0f, 1.0f, 1.0f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
            stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
            poseStack, multiBufferSource, entity.level(), 0
    );

        poseStack.popPose();
    }
}