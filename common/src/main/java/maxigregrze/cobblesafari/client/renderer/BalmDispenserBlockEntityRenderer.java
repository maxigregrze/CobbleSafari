package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.balm.BalmDispenserBlock;
import maxigregrze.cobblesafari.block.balm.BalmDispenserBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BalmDispenserBlockEntityRenderer implements BlockEntityRenderer<BalmDispenserBlockEntity> {

    private static final float ROTATION_DEGREES_PER_TICK = 2.0f;
    private static final float BOB_PHASE_PER_TICK = 0.04f;

    public BalmDispenserBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BalmDispenserBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.getValue(BalmDispenserBlock.ACTIVE) || state.getValue(BalmDispenserBlock.CHARGE) != BalmDispenserBlock.CHARGE_READY) {
            return;
        }

        float t = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() + partialTick : partialTick;
        float yaw = t * ROTATION_DEGREES_PER_TICK;
        float bob = Mth.sin(t * BOB_PHASE_PER_TICK) * 0.05f;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.8 + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.scale(0.5f, 0.5f, 0.5f);

        ItemStack floating = new ItemStack(((BalmDispenserBlock) state.getBlock()).getDispensedItem());

        Minecraft.getInstance().getItemRenderer().renderStatic(
                floating,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
        );
        poseStack.popPose();
    }
}
