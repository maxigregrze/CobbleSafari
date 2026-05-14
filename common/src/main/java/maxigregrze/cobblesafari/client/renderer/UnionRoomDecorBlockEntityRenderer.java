package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxigregrze.cobblesafari.block.misc.UnionRoomDecorBlock;
import maxigregrze.cobblesafari.block.misc.UnionRoomDecorBlockEntity;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class UnionRoomDecorBlockEntityRenderer implements BlockEntityRenderer<UnionRoomDecorBlockEntity> {

    private final BlockRenderDispatcher blockRenderDispatcher;

    public UnionRoomDecorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(UnionRoomDecorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && blockEntity.shouldHideWorldModelForLocalPlayer(mc.player)) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(UnionRoomDecorBlock.FACING);
        float facingYaw = facing.toYRot();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYaw));
        poseStack.translate(-0.5, 0.0, -0.5);

        Block display = displayBlockFor(state.getBlock());
        if (display != null) {
            BlockState renderState = display.defaultBlockState();
            this.blockRenderDispatcher.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private static Block displayBlockFor(Block block) {
        if (block == ModBlocks.UNION_ROOM_CROWD) {
            return ModBlocks.UNION_ROOM_CROWD_DISPLAY;
        }
        if (block == ModBlocks.UNION_ROOM_POKEBALL) {
            return ModBlocks.UNION_ROOM_POKEBALL_DISPLAY;
        }
        if (block == ModBlocks.UNION_ROOM_SPOT) {
            return ModBlocks.UNION_ROOM_SPOT_DISPLAY;
        }
        return null;
    }
}
