package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.misc.LightningSpotBlock;
import maxigregrze.cobblesafari.block.misc.LightningSpotBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the Lightning Spot's icon inside its cell, but only for a creative player holding the
 * block - the same visibility rule as the vanilla light block, and the only reason the block owns
 * a {@code BlockEntity} at all (a blockstate cannot depend on what the viewer carries).
 *
 * <p>The icon is the block's own item, billboarded to the camera and lit at full brightness so it
 * stays readable in an unlit dungeon.</p>
 */
public class LightningSpotBlockEntityRenderer implements BlockEntityRenderer<LightningSpotBlockEntity> {

    private static final float ICON_SCALE = 0.5f;

    public LightningSpotBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LightningSpotBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        BlockState state = blockEntity.getBlockState();
        if (player == null || !player.isCreative() || !(state.getBlock() instanceof LightningSpotBlock spot)) {
            return;
        }
        if (!spot.isHolding(player)) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);

        ItemStack icon = new ItemStack(spot.asItem());
        minecraft.getItemRenderer().renderStatic(
                icon,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0);
        poseStack.popPose();
    }
}
