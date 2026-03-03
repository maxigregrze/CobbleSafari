package maxigregrze.cobblesafari.block.basepc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.block.BlockPart;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BasePCBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<BasePCBlock> CODEC = simpleCodec(BasePCBlock::new);
    public static final EnumProperty<BlockPart> PART = EnumProperty.create("part", BlockPart.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty RANK = IntegerProperty.create("rank", 0, 5);

    private static final VoxelShape SHAPE_BOX = Block.box(0, 0, 0, 16, 16, 16);

    public BasePCBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BlockPart.CENTER)
                .setValue(RANK, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, RANK);
    }

    public static Direction getSideDirection(Direction facing) {
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
                    .setValue(RANK, 0);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (state.getValue(PART) != BlockPart.CENTER) {
            return;
        }
        Direction facing = state.getValue(FACING);
        BlockState sideState = state.setValue(PART, BlockPart.SIDE);
        BlockState topState = state.setValue(PART, BlockPart.TOP);
        level.setBlock(pos.relative(getSideDirection(facing)), sideState, Block.UPDATE_ALL);
        level.setBlock(pos.above(), topState, Block.UPDATE_ALL);
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BasePCBlockEntity basePCBE) {
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    int rank = tag.getInt("Rank");
                    int battery = tag.getInt("Battery");
                    basePCBE.setRank(rank);
                    basePCBE.setBattery(battery);
                    int r = Math.min(rank, 5);
                    level.setBlock(pos, state.setValue(RANK, r), Block.UPDATE_ALL);
                    level.setBlock(pos.relative(getSideDirection(facing)), sideState.setValue(RANK, r), Block.UPDATE_ALL);
                    level.setBlock(pos.above(), topState.setValue(RANK, r), Block.UPDATE_ALL);
                }
            }
        }
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

    private static ItemStack createBasePCDrop(BlockEntity be) {
        ItemStack drop = new ItemStack(maxigregrze.cobblesafari.init.ModBlocks.SECRETBASE_PC);
        int rank = 0;
        int battery = 0;
        if (be instanceof BasePCBlockEntity basePCBE) {
            rank = basePCBE.getRank();
            battery = basePCBE.getBattery();
        }
        if (rank == 5) {
            rank = 0;
            battery = 0;
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("Rank", rank);
        tag.putInt("Battery", battery);
        drop.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        drop.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(rank));
        return drop;
    }

    private void removePartAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(this)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
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
                if (!player.isCreative()) {
                    BlockEntity be = level.getBlockEntity(centerPos);
                    ItemStack drop = createBasePCDrop(be);
                    Block.popResource(level, centerPos, drop);
                }
                removePartAt(level, centerPos);
            } else if (!player.isCreative()) {
                BlockEntity be = level.getBlockEntity(pos);
                ItemStack drop = createBasePCDrop(be);
                Block.popResource(level, pos, drop);
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos centerPos = getCenterPos(pos, state);
        BlockEntity be = level.getBlockEntity(centerPos);
        if (be instanceof BasePCBlockEntity basePCBE) {
            player.openMenu(basePCBE);
        }
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART) == BlockPart.CENTER) {
            return new BasePCBlockEntity(pos, state);
        }
        return null;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() && type == ModBlockEntities.SECRETBASE_PC) {
            return (lvl, pos, st, be) -> ((BasePCBlockEntity) be).serverTick(lvl, pos, st);
        }
        return null;
    }
}
