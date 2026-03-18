package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GiratinaCoreSideBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    private final int offsetXFromCenter;
    private final int offsetZFromCenter;

    public GiratinaCoreSideBlock(int offsetXFromCenter, int offsetZFromCenter, Properties properties) {
        super(properties);
        this.offsetXFromCenter = offsetXFromCenter;
        this.offsetZFromCenter = offsetZFromCenter;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.isCreative()) {
            return 0.0f;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos centerPos = pos.offset(-offsetXFromCenter, 0, -offsetZFromCenter);
        return level.getBlockState(centerPos).is(ModBlocks.GIRATINA_CORE);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            BlockPos centerPos = pos.offset(-offsetXFromCenter, 0, -offsetZFromCenter);
            GiratinaCoreBlock.breakMultiblock(level, centerPos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && !GiratinaCoreBlock.isRemovingMultiblock()) {
            BlockPos centerPos = pos.offset(-offsetXFromCenter, 0, -offsetZFromCenter);
            if (level.getBlockState(centerPos).is(ModBlocks.GIRATINA_CORE)) {
                GiratinaCoreBlock.breakMultiblock(level, centerPos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public String getDescriptionId() {
        return "block.cobblesafari.giratina_core";
    }
}
