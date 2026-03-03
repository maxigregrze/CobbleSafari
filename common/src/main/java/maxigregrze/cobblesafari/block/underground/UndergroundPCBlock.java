package maxigregrze.cobblesafari.block.underground;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.BlockPart;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class UndergroundPCBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<UndergroundPCBlock> CODEC = simpleCodec(UndergroundPCBlock::new);
    public static final EnumProperty<BlockPart> PART = EnumProperty.create("part", BlockPart.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<PCState> PC_STATE = EnumProperty.create("pc_state", PCState.class);

    private static final VoxelShape SHAPE_BOX = Block.box(0, 0, 0, 16, 16, 16);

    public enum PCState implements StringRepresentable {
        EMPTY("empty"),
        REGULAR("regular"),
        BRONZE("bronze"),
        SILVER("silver"),
        GOLD("gold"),
        PLATINUM("platinum");

        private final String name;

        PCState(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    private static final Map<PCState, Item> STATE_TO_FLAG = Map.of(
            PCState.REGULAR, ModItems.FLAG_REGULAR,
            PCState.BRONZE, ModItems.FLAG_BRONZE,
            PCState.SILVER, ModItems.FLAG_SILVER,
            PCState.GOLD, ModItems.FLAG_GOLD,
            PCState.PLATINUM, ModItems.FLAG_PLATINUM
    );

    private static final Map<Item, PCState> FLAG_TO_STATE = Map.of(
            ModItems.FLAG_REGULAR, PCState.REGULAR,
            ModItems.FLAG_BRONZE, PCState.BRONZE,
            ModItems.FLAG_SILVER, PCState.SILVER,
            ModItems.FLAG_GOLD, PCState.GOLD,
            ModItems.FLAG_PLATINUM, PCState.PLATINUM
    );

    public UndergroundPCBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BlockPart.CENTER)
                .setValue(PC_STATE, PCState.EMPTY));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, PC_STATE);
    }

    private static Direction getSideDirection(Direction facing) {
        return facing.getCounterClockWise();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockPos sidePos = pos.relative(getSideDirection(facing));
        BlockPos topPos = pos.above();
        boolean sideFree = level.getBlockState(sidePos).canBeReplaced(context);
        boolean topFree = level.getBlockState(topPos).canBeReplaced(context);
        boolean inBounds = pos.getY() < level.getMaxBuildHeight() - 1;
        if (sideFree && topFree && inBounds) {
            return this.defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(PART, BlockPart.CENTER)
                    .setValue(PC_STATE, PCState.EMPTY);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (state.getValue(PART) != BlockPart.CENTER) {
            return;
        }
        Direction facing = state.getValue(FACING);
        Direction sideDir = getSideDirection(facing);
        BlockState partState = state.setValue(PART, BlockPart.SIDE).setValue(FACING, facing);
        level.setBlock(pos.relative(sideDir), partState, Block.UPDATE_ALL);
        level.setBlock(pos.above(), state.setValue(PART, BlockPart.TOP).setValue(FACING, facing), Block.UPDATE_ALL);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        return switch (state.getValue(PART)) {
            case CENTER -> true;
            case SIDE -> {
                Direction facing = state.getValue(FACING);
                Direction sideDir = getSideDirection(facing);
                BlockState origin = level.getBlockState(pos.relative(sideDir.getOpposite()));
                yield origin.is(this) && origin.getValue(PART) == BlockPart.CENTER;
            }
            case TOP -> {
                BlockState belowState = level.getBlockState(pos.below());
                yield belowState.is(this) && belowState.getValue(PART) == BlockPart.CENTER;
            }
        };
    }

    private BlockPos getCenterPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(PART)) {
            case CENTER -> pos;
            case SIDE -> pos.relative(getSideDirection(facing).getOpposite());
            case TOP -> pos.below();
        };
    }

    private void removePartAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(this)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
        }
    }

    private void dropBlockAndFlag(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isCreative()) {
            return;
        }
        Block.popResource(level, pos, new ItemStack(this));
        PCState pcState = state.getValue(PC_STATE);
        if (pcState != PCState.EMPTY) {
            Item flagItem = STATE_TO_FLAG.get(pcState);
            if (flagItem != null) {
                Block.popResource(level, pos, new ItemStack(flagItem));
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos centerPos = getCenterPos(pos, state);
            BlockState centerState = level.getBlockState(centerPos);
            Direction facing = centerState.getValue(FACING);
            removePartAt(level, centerPos.relative(getSideDirection(facing)));
            removePartAt(level, centerPos.above());
            if (state.getValue(PART) != BlockPart.CENTER) {
                dropBlockAndFlag(level, centerPos, centerState, player);
                removePartAt(level, centerPos);
            } else {
                dropBlockAndFlag(level, pos, state, player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return rotateShape(SHAPE_BOX, state.getValue(FACING));
    }

    private static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        return switch (facing) {
            case NORTH -> shape;
            case SOUTH -> Shapes.box(
                    1 - shape.max(Direction.Axis.X), shape.min(Direction.Axis.Y), 1 - shape.max(Direction.Axis.Z),
                    1 - shape.min(Direction.Axis.X), shape.max(Direction.Axis.Y), 1 - shape.min(Direction.Axis.Z));
            case EAST -> Shapes.box(
                    shape.min(Direction.Axis.Z), shape.min(Direction.Axis.Y), 1 - shape.max(Direction.Axis.X),
                    shape.max(Direction.Axis.Z), shape.max(Direction.Axis.Y), 1 - shape.min(Direction.Axis.X));
            case WEST -> Shapes.box(
                    1 - shape.max(Direction.Axis.Z), shape.min(Direction.Axis.Y), shape.min(Direction.Axis.X),
                    1 - shape.min(Direction.Axis.Z), shape.max(Direction.Axis.Y), shape.max(Direction.Axis.X));
            default -> shape;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        BlockPos centerPos = getCenterPos(pos, state);
        BlockState centerState = level.getBlockState(centerPos);
        PCState currentState = centerState.getValue(PC_STATE);
        if (currentState != PCState.EMPTY) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        PCState newState = FLAG_TO_STATE.get(stack.getItem());
        if (newState == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        BlockState newBlockState = centerState.setValue(PC_STATE, newState);
        Direction facing = centerState.getValue(FACING);
        level.setBlock(centerPos, newBlockState, Block.UPDATE_ALL);
        level.setBlock(centerPos.relative(getSideDirection(facing)), newBlockState.setValue(PART, BlockPart.SIDE), Block.UPDATE_ALL);
        level.setBlock(centerPos.above(), newBlockState.setValue(PART, BlockPart.TOP), Block.UPDATE_ALL);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos centerPos = getCenterPos(pos, state);
        BlockState centerState = level.getBlockState(centerPos);
        PCState currentState = centerState.getValue(PC_STATE);
        if (currentState == PCState.EMPTY) {
            return InteractionResult.PASS;
        }
        Item flagItem = STATE_TO_FLAG.get(currentState);
        if (flagItem == null) {
            return InteractionResult.PASS;
        }
        ItemStack flagStack = new ItemStack(flagItem);
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, flagStack);
        } else {
            player.getInventory().placeItemBackInInventory(flagStack);
        }
        BlockState emptyState = centerState.setValue(PC_STATE, PCState.EMPTY);
        Direction facing = centerState.getValue(FACING);
        level.setBlock(centerPos, emptyState, Block.UPDATE_ALL);
        level.setBlock(centerPos.relative(getSideDirection(facing)), emptyState.setValue(PART, BlockPart.SIDE), Block.UPDATE_ALL);
        level.setBlock(centerPos.above(), emptyState.setValue(PART, BlockPart.TOP), Block.UPDATE_ALL);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) {
            return;
        }
        if (!level.isClientSide() && state.getValue(PART) == BlockPart.CENTER) {
            BlockPos sidePos = pos.relative(getSideDirection(state.getValue(FACING)));
            BlockPos topPos = pos.above();
            if (level.getBlockState(sidePos).is(this)) {
                level.setBlock(sidePos, Blocks.AIR.defaultBlockState(), 35);
            }
            if (level.getBlockState(topPos).is(this)) {
                level.setBlock(topPos, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
