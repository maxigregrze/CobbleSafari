package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.block.dungeon.HoopaRingPortalBlock;
import maxigregrze.cobblesafari.block.dungeon.HoopaRingPortalBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class HoopaRingPortalBlockEntityRenderer implements BlockEntityRenderer<HoopaRingPortalBlockEntity> {

    private final BlockRenderDispatcher blockRenderDispatcher;

    public HoopaRingPortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(HoopaRingPortalBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(HoopaRingPortalBlock.FACING);
        DungeonPortalLikeRenderer.render(
                blockEntity,
                partialTick,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                facing,
                blockRenderDispatcher
        );
    }

    @Override
    public boolean shouldRenderOffScreen(HoopaRingPortalBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
