package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.block.DistortionFlowerPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractReactiveDistortionFlowerBlock extends Block {

    public static final EnumProperty<DistortionFlowerPart> PART = EnumProperty.create("part", DistortionFlowerPart.class);
    public static final BooleanProperty VISIBLE = BooleanProperty.create("visible");

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);
    private static final float TOUCH_DAMAGE = 1.0F;
    private static final double PROXIMITY_RADIUS_SQR = 9.0D;
    private static final int VISIBILITY_TICK_INTERVAL = 5;

    private final boolean defaultVisible;
    private final boolean visibleWhenNear;
    private final boolean collisionWhenInvisible;
    private final boolean damageWhenInvisible;

    protected AbstractReactiveDistortionFlowerBlock(
            Properties properties,
            boolean defaultVisible,
            boolean visibleWhenNear,
            boolean collisionWhenInvisible,
            boolean damageWhenInvisible
    ) {
        super(properties);
        this.defaultVisible = defaultVisible;
        this.visibleWhenNear = visibleWhenNear;
        this.collisionWhenInvisible = collisionWhenInvisible;
        this.damageWhenInvisible = damageWhenInvisible;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PART, DistortionFlowerPart.FLOWER)
                .setValue(VISIBLE, defaultVisible));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, VISIBLE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1) {
            return null;
        }
        if (!level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(PART, DistortionFlowerPart.BASE)
                .setValue(VISIBLE, this.defaultVisible);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && state.getValue(PART) == DistortionFlowerPart.BASE) {
            BlockState topState = this.defaultBlockState()
                    .setValue(PART, DistortionFlowerPart.FLOWER)
                    .setValue(VISIBLE, this.defaultVisible);
            level.setBlock(pos.above(), topState, Block.UPDATE_ALL);
            level.scheduleTick(pos, this, VISIBILITY_TICK_INTERVAL);
            level.scheduleTick(pos.above(), this, VISIBILITY_TICK_INTERVAL);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, VISIBILITY_TICK_INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean near = isPlayerNearXZ(level, pos);
        boolean shouldBeVisible = this.visibleWhenNear ? near : !near;
        if (state.getValue(VISIBLE).booleanValue() != shouldBeVisible) {
            level.setBlock(pos, state.setValue(VISIBLE, shouldBeVisible), Block.UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, this, VISIBILITY_TICK_INTERVAL);
    }

    private boolean isPlayerNearXZ(Level level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        for (Player player : level.players()) {
            double dx = player.getX() - centerX;
            double dz = player.getZ() - centerZ;
            if ((dx * dx) + (dz * dz) < PROXIMITY_RADIUS_SQR) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!stack.is(this.asItem())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockPos topPos = findTop(level, pos);
        BlockPos newTopPos = topPos.above();
        if (newTopPos.getY() >= level.getMaxBuildHeight()) {
            return ItemInteractionResult.FAIL;
        }
        BlockState newTopState = level.getBlockState(newTopPos);
        if (!newTopState.canBeReplaced()) {
            return ItemInteractionResult.FAIL;
        }

        BlockState topState = level.getBlockState(topPos);
        if (topState.getValue(PART) == DistortionFlowerPart.FLOWER) {
            level.setBlock(topPos, topState.setValue(PART, DistortionFlowerPart.STEM), Block.UPDATE_ALL);
        }
        level.setBlock(newTopPos, this.defaultBlockState().setValue(PART, DistortionFlowerPart.FLOWER), Block.UPDATE_ALL);
        level.scheduleTick(topPos, this, VISIBILITY_TICK_INTERVAL);
        level.scheduleTick(newTopPos, this, VISIBILITY_TICK_INTERVAL);

        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        DistortionFlowerPart part = state.getValue(PART);
        if (part == DistortionFlowerPart.BASE) {
            BlockPos below = pos.below();
            return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        }
        return level.getBlockState(pos.below()).is(this);
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
            removeFromBrokenToTop(level, pos, dropItems);
            updateBlockBelowAfterBreak(level, pos.below());
            return state;
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void removeFromBrokenToTop(Level level, BlockPos brokenPos, boolean dropItems) {
        BlockPos top = findTop(level, brokenPos);
        for (int y = top.getY(); y >= brokenPos.getY(); y--) {
            BlockPos currentPos = new BlockPos(brokenPos.getX(), y, brokenPos.getZ());
            BlockState currentState = level.getBlockState(currentPos);
            if (!currentState.is(this)) {
                continue;
            }
            if (dropItems && currentState.getValue(PART) != DistortionFlowerPart.BASE) {
                Block.popResource(level, currentPos, new ItemStack(this));
            }
            level.setBlock(currentPos, Blocks.AIR.defaultBlockState(), 35);
        }
    }

    private void updateBlockBelowAfterBreak(Level level, BlockPos belowPos) {
        BlockState belowState = level.getBlockState(belowPos);
        if (!belowState.is(this)) {
            return;
        }
        DistortionFlowerPart belowPart = belowState.getValue(PART);
        if (belowPart == DistortionFlowerPart.BASE) {
            level.setBlock(belowPos, Blocks.AIR.defaultBlockState(), 35);
        } else if (belowPart != DistortionFlowerPart.FLOWER) {
            level.setBlock(belowPos, belowState.setValue(PART, DistortionFlowerPart.FLOWER), Block.UPDATE_ALL);
        }
    }

    private BlockPos findTop(Level level, BlockPos start) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        while (cursor.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(cursor.above()).is(this)) {
            cursor.move(Direction.UP);
        }
        return cursor.immutable();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof Player) {
            boolean visible = state.getValue(VISIBLE);
            if (visible || this.damageWhenInvisible) {
                entity.hurt(level.damageSources().cactus(), TOUCH_DAMAGE);
            }
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(VISIBLE).booleanValue() && !this.collisionWhenInvisible) {
            return Shapes.empty();
        }
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(VISIBLE).booleanValue() ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }
}

