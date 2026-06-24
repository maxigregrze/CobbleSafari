package maxigregrze.cobblesafari.block.hyperspace;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Ladder-like climbable block (climbing comes from the {@code #minecraft:climbable} tag).
 * Survives on a sturdy wall behind it <em>or</em> on another Hyperspace ladder below (bottom anchor).
 * Right-clicking a <em>side</em> face with the ladder item extends the column upward (scaffolding-like);
 * clicking the top does not extend.
 */
public class HyperspaceLadderBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<HyperspaceLadderBlock> CODEC = simpleCodec(HyperspaceLadderBlock::new);

    private static final VoxelShape EAST = Block.box(0, 0, 0, 3, 16, 16);
    private static final VoxelShape WEST = Block.box(13, 0, 0, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 3);
    private static final VoxelShape NORTH = Block.box(0, 0, 13, 16, 16, 16);

    private static final int MAX_EXTEND = 32;

    public HyperspaceLadderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> EAST;
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Anchored from below: needs a sturdy block under it, or another Hyperspace ladder below.
        BlockPos below = pos.below();
        if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return true;
        }
        return level.getBlockState(below).is(this);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!canSurvive(this.defaultBlockState(), level, pos)) {
            return null;
        }
        Direction clicked = context.getClickedFace();
        if (clicked.getAxis().isHorizontal()) {
            return this.defaultBlockState().setValue(FACING, clicked);
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || blockItem.getBlock() != this) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!hit.getDirection().getAxis().isHorizontal()) {
            // Only side clicks extend; top/bottom anchor must stay vital.
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Direction facing = state.getValue(FACING);
        BlockPos top = pos;
        for (int i = 0; i < MAX_EXTEND; i++) {
            BlockPos next = top.above();
            BlockState above = level.getBlockState(next);
            if (above.is(this)) {
                top = next;
                continue;
            }
            if (above.canBeReplaced()) {
                if (!level.isClientSide()) {
                    level.setBlock(next, this.defaultBlockState().setValue(FACING, facing), Block.UPDATE_ALL);
                    SoundType sound = this.defaultBlockState().getSoundType();
                    level.playSound(null, next, sound.getPlaceSound(), SoundSource.BLOCKS,
                            (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            break;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
}
