package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Orientable 4-cell vertical multiblock (lamp post on the ground).
 * The <em>placed</em> cell is the BOTTOM; CENTER goes at y+1, CENTERTOP at y+2 and TOP at y+3.
 * Lighting is configured per part through {@code Properties.lightLevel} at registration.
 * 4-cell variant of {@link HyperspaceTriBlock}.
 */
public class HyperspaceQuadBlock extends HorizontalDirectionalBlock {

    public static final EnumProperty<HyperspaceQuadPart> PART = EnumProperty.create("part", HyperspaceQuadPart.class);

    private final boolean wallMounted;
    private final boolean hasCollision;
    private final VoxelShape shape;

    private static boolean breaking = false;

    public HyperspaceQuadBlock(Properties properties, boolean wallMounted, boolean hasCollision, VoxelShape shape) {
        super(properties);
        this.wallMounted = wallMounted;
        this.hasCollision = hasCollision;
        this.shape = shape;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, HyperspaceQuadPart.BOTTOM));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(props -> new HyperspaceQuadBlock(props, this.wallMounted, this.hasCollision, this.shape));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos(); // BOTTOM cell
        Level level = context.getLevel();
        Direction facing;
        if (wallMounted) {
            Direction clicked = context.getClickedFace();
            facing = clicked.getAxis().isHorizontal() ? clicked : context.getHorizontalDirection().getOpposite();
        } else {
            facing = context.getHorizontalDirection().getOpposite();
        }
        if (!level.getBlockState(pos.above()).canBeReplaced(context)
                || !level.getBlockState(pos.above(2)).canBeReplaced(context)
                || !level.getBlockState(pos.above(3)).canBeReplaced(context)) {
            return null;
        }
        if (!supportOk(level, pos, facing)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, HyperspaceQuadPart.BOTTOM);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || state.getValue(PART) != HyperspaceQuadPart.BOTTOM) {
            return;
        }
        Direction facing = state.getValue(FACING);
        level.setBlock(pos.above(),
                this.defaultBlockState().setValue(FACING, facing).setValue(PART, HyperspaceQuadPart.CENTER), Block.UPDATE_ALL);
        level.setBlock(pos.above(2),
                this.defaultBlockState().setValue(FACING, facing).setValue(PART, HyperspaceQuadPart.CENTERTOP), Block.UPDATE_ALL);
        level.setBlock(pos.above(3),
                this.defaultBlockState().setValue(FACING, facing).setValue(PART, HyperspaceQuadPart.TOP), Block.UPDATE_ALL);
    }

    /** Support of the whole column, evaluated from its BOTTOM cell. */
    private boolean supportOk(LevelReader level, BlockPos bottomPos, Direction facing) {
        if (!wallMounted) {
            BlockPos below = bottomPos.below();
            return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        }
        Direction back = facing.getOpposite();
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos cell = bottomPos.above(dy);
            BlockPos support = cell.relative(back);
            if (!level.getBlockState(support).isFaceSturdy(level, support, facing)) {
                return false;
            }
        }
        return true;
    }

    private BlockPos bottomOf(BlockPos pos, BlockState state) {
        return switch (state.getValue(PART)) {
            case BOTTOM -> pos;
            case CENTER -> pos.below();
            case CENTERTOP -> pos.below(2);
            case TOP -> pos.below(3);
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return switch (state.getValue(PART)) {
            // Anchor: depends only on its own support (so it survives the moment it is placed).
            case BOTTOM -> supportOk(level, pos, state.getValue(FACING));
            case CENTER -> isPart(level, pos.below(), HyperspaceQuadPart.BOTTOM);
            case CENTERTOP -> isPart(level, pos.below(), HyperspaceQuadPart.CENTER);
            case TOP -> isPart(level, pos.below(), HyperspaceQuadPart.CENTERTOP);
        };
    }

    private boolean isPart(LevelReader level, BlockPos pos, HyperspaceQuadPart part) {
        BlockState s = level.getBlockState(pos);
        return s.is(this) && s.getValue(PART) == part;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        breakStructure(level, pos, state);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            breakStructure(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void breakStructure(Level level, BlockPos pos, BlockState state) {
        if (breaking || level.isClientSide()) {
            return;
        }
        breaking = true;
        BlockPos bottom = bottomOf(pos, state);
        for (BlockPos p : new BlockPos[]{bottom, bottom.above(), bottom.above(2), bottom.above(3)}) {
            if (level.getBlockState(p).is(this)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        breaking = false;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return hasCollision ? shape : Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
