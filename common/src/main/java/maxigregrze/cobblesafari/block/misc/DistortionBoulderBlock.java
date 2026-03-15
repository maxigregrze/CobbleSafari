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
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DistortionBoulderBlock extends FaceAttachedHorizontalDirectionalBlock {

    public static final MapCodec<DistortionBoulderBlock> CODEC = simpleCodec(DistortionBoulderBlock::new);

    private static final VoxelShape SHAPE_FLOOR   = Block.box(-2,  0, -2, 18, 18, 18);
    private static final VoxelShape SHAPE_CEILING = Block.box(-2, -2, -2, 18, 16, 18);
    private static final VoxelShape SHAPE_WALL    = Block.box(-2, -2, -2, 18, 18, 18);

    public DistortionBoulderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        AttachFace face;
        Direction facing;

        switch (clickedFace) {
            case DOWN -> { face = AttachFace.CEILING; facing = context.getHorizontalDirection().getOpposite(); }
            case UP   -> { face = AttachFace.FLOOR;   facing = context.getHorizontalDirection().getOpposite(); }
            default   -> { face = AttachFace.WALL;    facing = clickedFace; }
        }

        return this.defaultBlockState()
                .setValue(FACE, face)
                .setValue(FACING, facing);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR   -> SHAPE_FLOOR;
            case CEILING -> SHAPE_CEILING;
            case WALL    -> SHAPE_WALL;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction attachDir = getConnectedDirection(state);
        BlockPos supportPos = pos.relative(attachDir.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);
        return supportState.isFaceSturdy(level, supportPos, attachDir);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == getConnectedDirection(state).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
