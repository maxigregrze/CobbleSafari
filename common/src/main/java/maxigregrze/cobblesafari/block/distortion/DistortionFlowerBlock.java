package maxigregrze.cobblesafari.block.distortion;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.DistortionFlowerPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DistortionFlowerBlock extends Block {

    public static final MapCodec<DistortionFlowerBlock> CODEC = simpleCodec(DistortionFlowerBlock::new);
    public static final EnumProperty<DistortionFlowerPart> PART = EnumProperty.create("part", DistortionFlowerPart.class);
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE_FLOOR_CEILING = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape SHAPE_WALL_NORTH_SOUTH = Block.box(3, 3, 0, 13, 13, 16);
    private static final VoxelShape SHAPE_WALL_EAST_WEST = Block.box(0, 3, 3, 16, 13, 13);
    private static final float TOUCH_DAMAGE = 1.0F;

    public DistortionFlowerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PART, DistortionFlowerPart.FLOWER)
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Direction clickedFace = context.getClickedFace();
        AttachFace face = faceForDirection(clickedFace);
        Direction horizontalFacing = face == AttachFace.WALL
                ? clickedFace
                : context.getHorizontalDirection();
        BlockState candidate = this.defaultBlockState()
                .setValue(PART, DistortionFlowerPart.BASE)
                .setValue(FACE, face)
                .setValue(FACING, horizontalFacing);
        Direction growthDirection = growthDirection(candidate);
        BlockPos flowerPos = pos.relative(growthDirection);
        if (level.isOutsideBuildHeight(flowerPos)) {
            return null;
        }
        if (!level.getBlockState(flowerPos).canBeReplaced(context)) {
            return null;
        }
        if (this.canSurvive(candidate, level, pos)) {
            return candidate;
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && state.getValue(PART) == DistortionFlowerPart.BASE) {
            Direction growthDirection = growthDirection(state);
            BlockState topState = this.defaultBlockState()
                    .setValue(PART, DistortionFlowerPart.FLOWER)
                    .setValue(FACE, state.getValue(FACE))
                    .setValue(FACING, state.getValue(FACING));
            level.setBlock(pos.relative(growthDirection), topState, Block.UPDATE_ALL);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!stack.is(this.asItem())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Direction growthDirection = growthDirection(state);
        BlockPos topPos = findTop(level, pos, growthDirection);
        BlockPos newTopPos = topPos.relative(growthDirection);
        if (level.isOutsideBuildHeight(newTopPos)) {
            return ItemInteractionResult.FAIL;
        }
        BlockState newTopState = level.getBlockState(newTopPos);
        if (!newTopState.canBeReplaced()) {
            return ItemInteractionResult.FAIL;
        }

        BlockState topState = level.getBlockState(topPos);
        DistortionFlowerPart topPart = topState.getValue(PART);
        if (topPart == DistortionFlowerPart.FLOWER) {
            level.setBlock(topPos, topState.setValue(PART, DistortionFlowerPart.STEM), Block.UPDATE_ALL);
        }
        level.setBlock(newTopPos, this.defaultBlockState()
                .setValue(PART, DistortionFlowerPart.FLOWER)
                .setValue(FACE, state.getValue(FACE))
                .setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        DistortionFlowerPart part = state.getValue(PART);
        Direction growthDirection = growthDirection(state);
        if (part == DistortionFlowerPart.BASE) {
            Direction supportDirection = supportDirection(state);
            BlockPos supportPos = pos.relative(supportDirection.getOpposite());
            return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, supportDirection);
        }
        BlockState previousState = level.getBlockState(pos.relative(growthDirection.getOpposite()));
        return previousState.is(this) && sameOrientation(state, previousState);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (this.canSurvive(state, level, pos)) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
        if (state.getValue(PART) != DistortionFlowerPart.BASE && level instanceof Level realLevel && !realLevel.isClientSide()) {
            Block.popResource(realLevel, pos, new ItemStack(this));
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            boolean dropItems = !player.isCreative();
            Direction growthDirection = growthDirection(state);
            removeFromBrokenToTop(level, pos, growthDirection, dropItems);
            updateBlockBehindAfterBreak(level, pos.relative(growthDirection.getOpposite()), state);
            return state;
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void removeFromBrokenToTop(Level level, BlockPos brokenPos, Direction growthDirection, boolean dropItems) {
        BlockPos top = findTop(level, brokenPos, growthDirection);
        BlockPos.MutableBlockPos currentPos = top.mutable();
        while (true) {
            BlockState currentState = level.getBlockState(currentPos);
            if (currentState.is(this) && dropItems && currentState.getValue(PART) != DistortionFlowerPart.BASE) {
                Block.popResource(level, currentPos, new ItemStack(this));
            }
            if (currentState.is(this)) {
                level.setBlock(currentPos, Blocks.AIR.defaultBlockState(), 35);
            }
            if (currentPos.equals(brokenPos)) {
                break;
            }
            currentPos.move(growthDirection.getOpposite());
        }
    }

    private void updateBlockBehindAfterBreak(Level level, BlockPos previousPos, BlockState brokenState) {
        BlockState previousState = level.getBlockState(previousPos);
        if (!previousState.is(this) || !sameOrientation(previousState, brokenState)) {
            return;
        }
        DistortionFlowerPart previousPart = previousState.getValue(PART);
        if (previousPart == DistortionFlowerPart.BASE) {
            level.setBlock(previousPos, Blocks.AIR.defaultBlockState(), 35);
        } else if (previousPart != DistortionFlowerPart.FLOWER) {
            level.setBlock(previousPos, previousState.setValue(PART, DistortionFlowerPart.FLOWER), Block.UPDATE_ALL);
        }
    }

    private BlockPos findTop(Level level, BlockPos start, Direction growthDirection) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        while (!level.isOutsideBuildHeight(cursor.relative(growthDirection)) && level.getBlockState(cursor.relative(growthDirection)).is(this)) {
            cursor.move(growthDirection);
        }
        return cursor.immutable();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof Player) {
            entity.hurt(level.damageSources().cactus(), TOUCH_DAMAGE);
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR, CEILING -> SHAPE_FLOOR_CEILING;
            case WALL -> switch (state.getValue(FACING)) {
                case NORTH, SOUTH -> SHAPE_WALL_NORTH_SOUTH;
                case EAST, WEST -> SHAPE_WALL_EAST_WEST;
                default -> SHAPE_WALL_NORTH_SOUTH;
            };
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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

    private static Direction growthDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }

    private static Direction supportDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static boolean sameOrientation(BlockState first, BlockState second) {
        return first.getValue(FACE) == second.getValue(FACE) && first.getValue(FACING) == second.getValue(FACING);
    }
}

