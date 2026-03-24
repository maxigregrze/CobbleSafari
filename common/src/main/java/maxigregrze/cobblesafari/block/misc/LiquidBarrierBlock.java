package maxigregrze.cobblesafari.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LiquidBarrierBlock extends BarrierBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape FULL_SHAPE = Shapes.block();

    public LiquidBarrierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        if (stack.is(Items.WATER_BUCKET)) {
            return !state.getValue(WATERLOGGED);
        }
        if (stack.is(Items.BARRIER) || stack.is(this.asItem())) {
            return false;
        }
        return context.getPlayer() != null && context.getPlayer().isCreative();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof Player player
                && (player.getMainHandItem().is(this.asItem()) || player.getOffhandItem().is(this.asItem()))) {
            return FULL_SHAPE;
        }
        return Shapes.empty();
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Boolean.TRUE.equals(state.getValue(WATERLOGGED)) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    @Override
    public boolean canPlaceLiquid(@org.jetbrains.annotations.Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return Boolean.FALSE.equals(state.getValue(WATERLOGGED)) && fluid == Fluids.WATER;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (Boolean.FALSE.equals(state.getValue(WATERLOGGED)) && fluidState.getType() == Fluids.WATER) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(WATERLOGGED, true), Block.UPDATE_ALL);
            }
            return true;
        }
        return false;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
