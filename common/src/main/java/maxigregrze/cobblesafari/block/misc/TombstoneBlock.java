package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Orientable tombstone. Placeable only on solid ground, mined with pickaxe,
 * drops itself. Hitbox aligned to the model, rotated by FACING.
 */
public abstract class TombstoneBlock extends HorizontalDirectionalBlock {

    public static final VoxelShape TOMBSTONE_SHAPE = Block.box(1, 0, 0, 15, 8, 16);
    public static final VoxelShape TOMBSTONE_SMALL_SHAPE = Block.box(0, 0, 5, 16, 16, 11);

    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    protected TombstoneBlock(VoxelShape southShape, Properties properties) {
        super(properties);
        AABB bounds = southShape.bounds();
        this.minX = bounds.minX * 16;
        this.minY = bounds.minY * 16;
        this.minZ = bounds.minZ * 16;
        this.maxX = bounds.maxX * 16;
        this.maxY = bounds.maxY * 16;
        this.maxZ = bounds.maxZ * 16;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    private VoxelShape shapeForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> Block.box(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
            case NORTH -> Block.box(16 - this.maxX, this.minY, 16 - this.maxZ, 16 - this.minX, this.maxY, 16 - this.minZ);
            case EAST -> Block.box(this.minZ, this.minY, 16 - this.maxX, this.maxZ, this.maxY, 16 - this.minX);
            case WEST -> Block.box(16 - this.maxZ, this.minY, this.minX, 16 - this.minZ, this.maxY, this.maxX);
            default -> Block.box(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
        };
    }

    public static final class Standard extends TombstoneBlock {
        public static final MapCodec<Standard> CODEC = simpleCodec(Standard::new);

        public Standard(Properties properties) {
            super(TOMBSTONE_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }

    public static final class Small extends TombstoneBlock {
        public static final MapCodec<Small> CODEC = simpleCodec(Small::new);

        public Small(Properties properties) {
            super(TOMBSTONE_SMALL_SHAPE, properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }
    }
}
