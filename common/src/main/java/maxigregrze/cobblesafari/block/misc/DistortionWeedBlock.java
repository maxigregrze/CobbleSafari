package maxigregrze.cobblesafari.block.misc;

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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DistortionWeedBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<DistortionWeedBlock> CODEC = simpleCodec(DistortionWeedBlock::new);
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    private static final VoxelShape FLOOR_SHAPE = Block.box(2, 0, 2, 14, 4, 14);
    private static final VoxelShape CEILING_SHAPE = Block.box(2, 12, 2, 14, 16, 14);
    private static final VoxelShape WALL_NORTH_SHAPE = Block.box(2, 2, 12, 14, 14, 16);
    private static final VoxelShape WALL_SOUTH_SHAPE = Block.box(2, 2, 0, 14, 14, 4);
    private static final VoxelShape WALL_EAST_SHAPE = Block.box(0, 2, 2, 4, 14, 14);
    private static final VoxelShape WALL_WEST_SHAPE = Block.box(12, 2, 2, 16, 14, 14);

    private final boolean allowWallPlacement;

    public DistortionWeedBlock(Properties properties) {
        this(properties, true);
    }

    public DistortionWeedBlock(Properties properties, boolean allowWallPlacement) {
        super(properties);
        this.allowWallPlacement = allowWallPlacement;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.FLOOR));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        AttachFace face = faceForDirection(clickedFace);
        if (face == AttachFace.WALL && !this.allowWallPlacement) {
            return null;
        }

        Direction horizontalFacing = face == AttachFace.WALL ? clickedFace : Direction.NORTH;
        BlockPos pos = context.getClickedPos();
        BlockState candidate = this.defaultBlockState()
                .setValue(FACING, horizontalFacing)
                .setValue(FACE, face);
        if (this.canSurvive(candidate, context.getLevel(), pos)) {
            return candidate;
        }
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR_SHAPE;
            case CEILING -> CEILING_SHAPE;
            case WALL -> switch (state.getValue(FACING)) {
                case NORTH -> WALL_NORTH_SHAPE;
                case SOUTH -> WALL_SOUTH_SHAPE;
                case EAST -> WALL_EAST_SHAPE;
                case WEST -> WALL_WEST_SHAPE;
                default -> WALL_NORTH_SHAPE;
            };
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction supportDirection = supportDirection(state);
        BlockPos supportPos = pos.relative(supportDirection.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);
        return supportState.isFaceSturdy(level, supportPos, supportDirection);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction supportDirection = supportDirection(state);
        if (direction == supportDirection.getOpposite() && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private static AttachFace faceForDirection(Direction direction) {
        if (direction == Direction.UP) {
            return AttachFace.FLOOR;
        }
        if (direction == Direction.DOWN) {
            return AttachFace.CEILING;
        }
        return AttachFace.WALL;
    }

    private static Direction supportDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }
}
