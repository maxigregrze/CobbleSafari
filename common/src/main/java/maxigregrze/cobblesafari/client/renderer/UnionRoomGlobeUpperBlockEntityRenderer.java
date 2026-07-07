package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.misc.UnionRoomGlobeUpperBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class UnionRoomGlobeUpperBlockEntityRenderer implements BlockEntityRenderer<UnionRoomGlobeUpperBlockEntity> {

    private static final float SPIN_DEGREES_PER_TICK = 2.0f;

    private final BlockRenderDispatcher blockRenderDispatcher;

    public UnionRoomGlobeUpperBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(UnionRoomGlobeUpperBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && blockEntity.shouldHideWorldModelForLocalPlayer(mc.player)) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        float facingYaw = facing.toYRot();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYaw));
        poseStack.translate(-0.5, 0.0, -0.5);

        double t = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() + (double) partialTick : partialTick;
        float spin = (float) ((t * SPIN_DEGREES_PER_TICK) % 360.0);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.translate(-0.5, -0.5, -0.5);
        BlockState moving = ModBlocks.UNION_ROOM_GLOBE_DISPLAY_MOVING.defaultBlockState();
        this.blockRenderDispatcher.renderSingleBlock(moving, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }
}
