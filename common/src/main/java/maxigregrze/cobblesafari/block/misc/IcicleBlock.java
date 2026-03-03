package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IcicleBlock extends Block implements Fallable {

    public static final MapCodec<IcicleBlock> CODEC = simpleCodec(IcicleBlock::new);
    public static final DirectionProperty TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;

    private static final VoxelShape TIP_SHAPE_UP = Block.box(5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape TIP_SHAPE_DOWN = Block.box(5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape TIP_MERGE_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape FRUSTUM_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape MIDDLE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape BASE_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public IcicleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(TIP_DIRECTION, Direction.UP)
                .setValue(THICKNESS, DripstoneThickness.TIP));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP_DIRECTION, THICKNESS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction preferred = context.getNearestLookingVerticalDirection().getOpposite();
        Direction tipDir = calculateTipDirection(level, pos, preferred);
        if (tipDir == null) {
            return null;
        }
        DripstoneThickness thickness = calculateThickness(level, pos, tipDir);
        return this.defaultBlockState()
                .setValue(TIP_DIRECTION, tipDir)
                .setValue(THICKNESS, thickness);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DripstoneThickness thickness = state.getValue(THICKNESS);
        VoxelShape shape;
        switch (thickness) {
            case TIP:
                shape = state.getValue(TIP_DIRECTION) == Direction.UP ? TIP_SHAPE_UP : TIP_SHAPE_DOWN;
                break;
            case TIP_MERGE:
                shape = TIP_MERGE_SHAPE;
                break;
            case FRUSTUM:
                shape = FRUSTUM_SHAPE;
                break;
            case MIDDLE:
                shape = MIDDLE_SHAPE;
                break;
            default:
                shape = BASE_SHAPE;
                break;
        }
        Vec3 offset = state.getOffset(level, pos);
        return shape.move(offset.x, 0.0, offset.z);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidPlacement(level, pos, state.getValue(TIP_DIRECTION));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction != Direction.UP && direction != Direction.DOWN) {
            return state;
        }

        Direction tipDir = state.getValue(TIP_DIRECTION);
        if (direction == tipDir.getOpposite()
                && !this.canSurvive(state, level, pos)
                && level instanceof ServerLevel) {
            level.scheduleTick(pos, this, 2);
        }

        DripstoneThickness thickness = calculateThickness(level, pos, tipDir);
        return state.setValue(THICKNESS, thickness);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.canSurvive(state, level, pos)) {
            return;
        }
        if (state.getValue(TIP_DIRECTION) == Direction.DOWN) {
            FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
            if (isTipThickness(state)) {
                entity.setHurtsEntities(2.0F, 40);
            }
        } else {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (state.getValue(TIP_DIRECTION) == Direction.UP
                && state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            entity.causeFallDamage(fallDistance + 2.0F, 2.0F, level.damageSources().stalagmite());
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity entity) {
        level.levelEvent(1045, pos, 0);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide()
                && projectile.mayInteract(level, hit.getBlockPos())
                && state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            level.destroyBlock(hit.getBlockPos(), true);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType computationType) {
        return false;
    }

    private Direction calculateTipDirection(LevelReader level, BlockPos pos, Direction preferred) {
        if (isValidPlacement(level, pos, preferred)) {
            return preferred;
        }
        if (isValidPlacement(level, pos, preferred.getOpposite())) {
            return preferred.getOpposite();
        }
        return null;
    }

    private boolean isValidPlacement(LevelReader level, BlockPos pos, Direction tipDirection) {
        BlockPos supportPos = pos.relative(tipDirection.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);
        return supportState.isFaceSturdy(level, supportPos, tipDirection)
                || isIcicleWithDirection(supportState, tipDirection);
    }

    private boolean isIcicleWithDirection(BlockState state, Direction direction) {
        return state.is(this) && state.getValue(TIP_DIRECTION) == direction;
    }

    private DripstoneThickness calculateThickness(LevelReader level, BlockPos pos, Direction tipDirection) {
        Direction baseDirection = tipDirection.getOpposite();
        BlockState towardBase = level.getBlockState(pos.relative(baseDirection));
        BlockState towardTip = level.getBlockState(pos.relative(tipDirection));

        if (towardTip.is(this) && towardTip.getValue(TIP_DIRECTION) == baseDirection) {
            return DripstoneThickness.TIP_MERGE;
        }

        boolean hasBaseNeighbor = isIcicleWithDirection(towardBase, tipDirection);
        boolean hasTipNeighbor = isIcicleWithDirection(towardTip, tipDirection);

        if (!hasBaseNeighbor) {
            if (!hasTipNeighbor) {
                return DripstoneThickness.TIP;
            }
            return isTipBlock(level, pos.relative(tipDirection), tipDirection)
                    ? DripstoneThickness.FRUSTUM : DripstoneThickness.BASE;
        }

        if (!hasTipNeighbor) {
            return DripstoneThickness.TIP;
        }

        if (isTipBlock(level, pos.relative(tipDirection), tipDirection)) {
            return DripstoneThickness.FRUSTUM;
        }

        return DripstoneThickness.MIDDLE;
    }

    private boolean isTipBlock(LevelReader level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) || state.getValue(TIP_DIRECTION) != direction) {
            return false;
        }
        BlockState nextState = level.getBlockState(pos.relative(direction));
        return !nextState.is(this) || nextState.getValue(TIP_DIRECTION) != direction;
    }

    private static boolean isTipThickness(BlockState state) {
        DripstoneThickness thickness = state.getValue(THICKNESS);
        return thickness == DripstoneThickness.TIP || thickness == DripstoneThickness.TIP_MERGE;
    }
}
