package maxigregrze.cobblesafari.block.underground;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Function;

public class UndergroundTimberBlock extends Block {

    public static final MapCodec<UndergroundTimberBlock> CODEC = simpleCodec(UndergroundTimberBlock::new);

    private final boolean requiresWall;
    private final Function<Direction, VoxelShape> shapeGetter;

    public UndergroundTimberBlock(Properties properties) {
        this(properties, true, UndergroundTimberBlock::horizontal);
    }

    public UndergroundTimberBlock(Properties properties, boolean requiresWall, Function<Direction, VoxelShape> shapeGetter) {
        super(properties);
        this.requiresWall = requiresWall;
        this.shapeGetter = shapeGetter;
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HorizontalDirectionalBlock.FACING, rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeGetter.apply(state.getValue(HorizontalDirectionalBlock.FACING));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!requiresWall) {
            return true;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos wallPos = pos.relative(facing);
        BlockState wallState = level.getBlockState(wallPos);
        return wallState.isFaceSturdy(level, wallPos, facing.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (requiresWall) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            if (direction == facing && !this.canSurvive(state, level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    public static VoxelShape horizontal(Direction facing) {
        return switch (facing) {
            case SOUTH -> Block.box(0, 3, 13, 16, 7, 16);
            case EAST -> Block.box(13, 3, 0, 16, 7, 16);
            case WEST -> Block.box(0, 3, 0, 3, 7, 16);
            default -> Block.box(0, 3, 0, 16, 7, 3);
        };
    }

    public static VoxelShape verticalSmall(Direction facing) {
        return switch (facing) {
            case SOUTH -> Block.box(6, 0, 14, 10, 16, 16);
            case EAST -> Block.box(14, 0, 6, 16, 16, 10);
            case WEST -> Block.box(0, 0, 6, 2, 16, 10);
            default -> Block.box(6, 0, 0, 10, 16, 2);
        };
    }

    public static VoxelShape verticalLarge(Direction facing) {
        return switch (facing) {
            case SOUTH -> Shapes.or(
                    Block.box(0, 3, 13, 16, 7, 16),
                    Block.box(6, 0, 14, 10, 16, 16)
            );
            case EAST -> Shapes.or(
                    Block.box(13, 3, 0, 16, 7, 16),
                    Block.box(14, 0, 6, 16, 16, 10)
            );
            case WEST -> Shapes.or(
                    Block.box(0, 3, 0, 3, 7, 16),
                    Block.box(0, 0, 6, 2, 16, 10)
            );
            default -> Shapes.or(
                    Block.box(0, 3, 0, 16, 7, 3),
                    Block.box(6, 0, 0, 10, 16, 2)
            );
        };
    }

    public static VoxelShape cornerHorizontal(Direction facing) {
        return switch (facing) {
            case EAST -> Shapes.or(
                    Block.box(13, 3, 0, 16, 7, 16),
                    Block.box(0, 3, 13, 16, 7, 16),
                    Block.box(14, 0, 14, 16, 16, 16)
            );
            case SOUTH -> Shapes.or(
                    Block.box(0, 3, 13, 16, 7, 16),
                    Block.box(0, 3, 0, 3, 7, 16),
                    Block.box(0, 0, 14, 2, 16, 16)
            );
            case WEST -> Shapes.or(
                    Block.box(0, 3, 0, 3, 7, 16),
                    Block.box(0, 3, 0, 16, 7, 3),
                    Block.box(0, 0, 0, 2, 16, 2)
            );
            default -> Shapes.or(
                    Block.box(0, 3, 0, 16, 7, 3),
                    Block.box(13, 3, 0, 16, 7, 16),
                    Block.box(14, 0, 0, 16, 16, 2)
            );
        };
    }

    public static VoxelShape cornerVerticalSmall(Direction facing) {
        return switch (facing) {
            case EAST -> Block.box(14, 0, 14, 16, 16, 16);
            case SOUTH -> Block.box(0, 0, 14, 2, 16, 16);
            case WEST -> Block.box(0, 0, 0, 2, 16, 2);
            default -> Block.box(14, 0, 0, 16, 16, 2);
        };
    }

    public static VoxelShape cornerVertical(Direction facing) {
        return cornerVerticalSmall(facing);
    }
}
