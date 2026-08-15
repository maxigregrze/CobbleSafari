package maxigregrze.cobblesafari.block.base;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for <em>non-orientable</em> blocks that are just a custom model plus an optional
 * ground requirement, with no bespoke behaviour. The missing member of the family:
 * {@link HorizontalModelBlock} covers the cardinal case, {@link FaceAttachedModelBlock} the
 * orthonormal one, and nothing covered the case with no facing at all.
 *
 * <p>{@link Waterlogged#WATERLOGGED} is declared <em>unconditionally</em>. The property cannot be
 * made optional (see {@link Waterlogged}), and for a decorative block that is placed in the world
 * by hand, not being waterloggable is more often a defect than a feature. The cost is one extra
 * blockstate variant.</p>
 */
public class GroundedModelBlock extends Block implements SimpleWaterloggedBlock {

    /** What the block must rest on in order to survive. */
    public enum Support { NONE, GROUND }

    private final VoxelShape shape;
    private final VoxelShape collisionShape;
    private final Support support;

    public GroundedModelBlock(Properties properties, VoxelShape shape, Support support) {
        this(properties, shape, shape, support);
    }

    public GroundedModelBlock(Properties properties, VoxelShape shape, VoxelShape collisionShape, Support support) {
        super(properties);
        this.shape = shape;
        this.collisionShape = collisionShape;
        this.support = support;
        this.registerDefaultState(this.stateDefinition.any().setValue(Waterlogged.WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(props -> new GroundedModelBlock(props, this.shape, this.collisionShape, this.support));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Waterlogged.WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(Waterlogged.WATERLOGGED, Waterlogged.atPlacement(context));
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /**
     * Same rule as vanilla {@code BasePressurePlateBlock}: a rigid block below, or anything whose
     * centre is sturdy upward. The second branch is what allows placing on a fence, a wall or an
     * upside-down slab.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (support == Support.NONE) {
            return true;
        }
        BlockPos below = pos.below();
        return Block.canSupportRigidBlock(level, below) || Block.canSupportCenter(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Waterlogged.scheduleTick(state, level, pos);
        if (support != Support.NONE && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Waterlogged.fluidState(state, super.getFluidState(state));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisionShape;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
