package maxigregrze.cobblesafari.block.dungeon;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.dungeon.sub.SubDungeonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Ultra Wormhole device: a two-cell vertical multiblock the structure author places by hand in
 * a parent dungeon's wormhole room. Right-clicking either half opens the Ultra Warp Ride
 * (action plan 183, sections 4.1 and 8.1).
 *
 * <p>Multiblock mechanics - second-half placement, mutual survival, joint break - are those of
 * {@code HyperspaceDoubleBlock}. This block adds exactly one thing: {@code FACING}. The authored
 * models are a plane on the {@code z = 8} slice spanning {@code x = -16} to {@code 32}, three
 * blocks wide, so the geometry only makes sense oriented.</p>
 *
 * <p><b>No block entity, and that is what dissolves finding S13.</b> There is no confirm payload:
 * the device opens on a vanilla interaction, so reach is server-enforced, the position never comes
 * from the client, and the target block is read from the world. The once-per-run entry lives on the
 * parent instance record, not here - state carried by a block entity dies with the block, which is
 * exactly what a per-run counter must not do.</p>
 */
public class UltraEntrancePortalBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<UltraEntrancePortalBlock> CODEC = simpleCodec(UltraEntrancePortalBlock::new);

    /**
     * The post, taken from the three <b>solid</b> elements of the authored models - the two other
     * elements are the flat, zero-thickness plane carrying the swirling texture, which would make a
     * hitbox you cannot see the edges of and a selection box three blocks wide.
     *
     * <p>Authored north-facing, in model units. {@code LOWER} is {@code ultra_entranceportal_down},
     * {@code UPPER} is {@code ultra_entranceportal_up}.</p>
     */
    private static final double[][] POST_LOWER = {
            {7.0, 2.0, 7.5, 9.0, 12.0, 8.5},
            {6.0, 12.0, 7.5, 10.0, 15.0, 8.5},
            {5.0, 15.0, 7.5, 11.0, 16.0, 8.5}
    };
    private static final double[][] POST_UPPER = {
            {7.0, 4.0, 7.5, 9.0, 14.0, 8.5},
            {6.0, 1.0, 7.5, 10.0, 4.0, 8.5},
            {5.0, 0.0, 7.5, 11.0, 1.0, 8.5}
    };

    private static final Map<Direction, VoxelShape> SHAPES_LOWER = buildShapes(POST_LOWER);
    private static final Map<Direction, VoxelShape> SHAPES_UPPER = buildShapes(POST_UPPER);

    /** Unions the boxes, then turns the result to each horizontal facing the blockstate can hold. */
    private static Map<Direction, VoxelShape> buildShapes(double[][] boxes) {
        Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                shape = Shapes.or(shape, rotate(b, facing));
            }
            byFacing.put(facing, shape);
        }
        return byFacing;
    }

    /** A quarter turn about the block's vertical centre line, matching the blockstate's y rotation. */
    private static VoxelShape rotate(double[] b, Direction facing) {
        double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        return switch (facing) {
            case EAST -> Block.box(16.0 - z1, y0, x0, 16.0 - z0, y1, x1);
            case SOUTH -> Block.box(16.0 - x1, y0, 16.0 - z1, 16.0 - x0, y1, 16.0 - z0);
            case WEST -> Block.box(z0, y0, 16.0 - x1, z1, y1, 16.0 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    public UltraEntrancePortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.DOUBLE_BLOCK_HALF);
    }

    /**
     * Outline and collision both follow the post. The block is no longer {@code noCollission()}:
     * the frame is something you bump into, while the portal plane it holds up stays empty air.
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Map<Direction, VoxelShape> shapes =
                state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                        ? SHAPES_UPPER
                        : SHAPES_LOWER;
        return shapes.getOrDefault(state.getValue(FACING), Shapes.block());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1
                || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(pos.above(),
                    state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER),
                    Block.UPDATE_ALL);
        }
    }

    /**
     * Unlike {@code HyperspaceDoubleBlock} the lower half needs no floor: this block is baked into
     * a structure and may well hang over a void. Each half only requires the other.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            return true;
        }
        BlockState below = level.getBlockState(pos.below());
        return below.is(this)
                && below.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(other);
        if (otherState.is(this)) {
            level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * Both halves open the ride; the upper one delegates to the lower, which is the only position
     * {@link SubDungeonManager#openDevice} resolves (action plan 183, section 4.1 bis).
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos device = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                ? pos.below()
                : pos;
        SubDungeonManager.openDevice(serverPlayer, device);
        return InteractionResult.CONSUME;
    }
}
