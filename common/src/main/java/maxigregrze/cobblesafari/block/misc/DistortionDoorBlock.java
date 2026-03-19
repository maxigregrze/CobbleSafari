package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DistortionDoorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DistortionDoorBlock> CODEC = simpleCodec(DistortionDoorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 1, 7);

    private static final int[][] PART_COORDS = {
            {0, 0},
            {0, 2},
            {1, 2},
            {2, 2},
            {0, 1},
            {2, 1},
            {0, 0},
            {2, 0}
    };

    private static final VoxelShape FULL_SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape WEST_HALF = Block.box(0, 0, 0, 8, 16, 16);
    private static final VoxelShape EAST_HALF = Block.box(8, 0, 0, 16, 16, 16);
    private static final VoxelShape NORTH_HALF = Block.box(0, 0, 0, 16, 16, 8);
    private static final VoxelShape SOUTH_HALF = Block.box(0, 0, 8, 16, 16, 16);

    private static boolean removingStructure = false;

    public DistortionDoorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 7));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos anchorPos = context.getClickedPos();
        if (!canPlaceStructure(context, anchorPos, facing)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, 7);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        Direction facing = state.getValue(FACING);
        for (int part = 1; part <= 7; part++) {
            BlockPos targetPos = getPosFromAnchor(pos, facing, part);
            BlockState partState = this.defaultBlockState().setValue(FACING, facing).setValue(PART, part);
            level.setBlock(targetPos, partState, Block.UPDATE_ALL);
        }
    }

    private boolean canPlaceStructure(BlockPlaceContext context, BlockPos anchorPos, Direction facing) {
        Level level = context.getLevel();
        for (int part = 1; part <= 7; part++) {
            BlockPos targetPos = getPosFromAnchor(anchorPos, facing, part);
            if (!level.getBlockState(targetPos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos getPosFromAnchor(BlockPos anchorPos, Direction facing, int part) {
        int[] coords = PART_COORDS[part];
        int col = coords[0];
        int row = coords[1];
        int deltaCol = col - 2;
        Direction right = facing.getClockWise();
        return anchorPos.relative(right, deltaCol).above(row);
    }

    private static BlockPos getAnchorFromPart(BlockPos partPos, BlockState state) {
        int part = state.getValue(PART);
        int[] coords = PART_COORDS[part];
        int col = coords[0];
        int row = coords[1];
        Direction right = state.getValue(FACING).getClockWise();
        return partPos.relative(right, 2 - col).below(row);
    }

    private static void breakStructure(Level level, BlockPos anchorPos, Direction facing) {
        if (removingStructure) {
            return;
        }
        removingStructure = true;
        for (int part = 1; part <= 7; part++) {
            BlockPos targetPos = getPosFromAnchor(anchorPos, facing, part);
            if (level.getBlockState(targetPos).getBlock() instanceof DistortionDoorBlock) {
                level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        removingStructure = false;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos anchor = getAnchorFromPart(pos, state);
            breakStructure(level, anchor, state.getValue(FACING));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && !removingStructure) {
            BlockPos anchor = getAnchorFromPart(pos, state);
            breakStructure(level, anchor, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = state.getValue(PART);
        if (part <= 3) {
            return FULL_SHAPE;
        }

        boolean left = part == 4 || part == 6;
        Direction facing = state.getValue(FACING);
        if (facing == Direction.NORTH) {
            return left ? WEST_HALF : EAST_HALF;
        }
        if (facing == Direction.SOUTH) {
            return left ? EAST_HALF : WEST_HALF;
        }
        if (facing == Direction.EAST) {
            return left ? NORTH_HALF : SOUTH_HALF;
        }
        return left ? SOUTH_HALF : NORTH_HALF;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCollisionShape(state, level, pos, context);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
