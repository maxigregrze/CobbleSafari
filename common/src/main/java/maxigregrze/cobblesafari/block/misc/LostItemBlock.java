package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class LostItemBlock extends BaseEntityBlock {
    public static final MapCodec<LostItemBlock> CODEC = simpleCodec(LostItemBlock::new);
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_ITEM = BooleanProperty.create("has_item");

    private static final VoxelShape FLOOR_SHAPE = Block.box(2, 0, 2, 14, 4, 14);
    private static final VoxelShape CEILING_SHAPE = Block.box(2, 12, 2, 14, 16, 14);
    private static final VoxelShape WALL_NORTH_SHAPE = Block.box(2, 2, 12, 14, 14, 16);
    private static final VoxelShape WALL_SOUTH_SHAPE = Block.box(2, 2, 0, 14, 14, 4);
    private static final VoxelShape WALL_EAST_SHAPE = Block.box(0, 2, 2, 4, 14, 14);
    private static final VoxelShape WALL_WEST_SHAPE = Block.box(12, 2, 2, 16, 14, 14);

    private static final ResourceKey<LootTable> LOST_ITEM_LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("cobblesafari", "lostitem")
    );

    public LostItemBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_ITEM, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, HAS_ITEM);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        AttachFace face = faceForDirection(clickedFace);
        Direction horizontalFacing = face == AttachFace.WALL ? clickedFace : Direction.NORTH;
        BlockPos pos = context.getClickedPos();
        BlockState candidate = this.defaultBlockState()
                .setValue(FACE, face)
                .setValue(FACING, horizontalFacing)
                .setValue(HAS_ITEM, true);
        if (this.canSurvive(candidate, context.getLevel(), pos)) {
            return candidate;
        }
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR_SHAPE;
            case CEILING -> CEILING_SHAPE;
            case WALL -> switch (state.getValue(FACING)) {
                case NORTH -> WALL_NORTH_SHAPE;
                case SOUTH -> WALL_SOUTH_SHAPE;
                case EAST -> WALL_EAST_SHAPE;
                case WEST -> WALL_WEST_SHAPE;
                default -> WALL_NORTH_SHAPE;
            };
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction supportDirection = supportDirection(state);
        BlockPos supportPos = pos.relative(supportDirection.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);
        return supportState.isFaceSturdy(level, supportPos, supportDirection);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction supportDirection = supportDirection(state);
        if (direction == supportDirection.getOpposite() && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return this.handleUse(state, level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = this.handleUse(state, level, pos, player);
        if (result.consumesAction()) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private InteractionResult handleUse(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof LostItemBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer && blockEntity.hasClaimed(serverPlayer.getUUID())) {
            return InteractionResult.CONSUME;
        }

        boolean hasItem = state.getValue(HAS_ITEM);
        if (player.isCreative() && player.getMainHandItem().isEmpty() && !hasItem) {
            level.setBlock(pos, state.setValue(HAS_ITEM, true), Block.UPDATE_ALL);
            blockEntity.resetClaims();
            return InteractionResult.CONSUME;
        }

        if (!hasItem) {
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer && !blockEntity.tryClaim(serverPlayer.getUUID())) {
            return InteractionResult.CONSUME;
        }

        dropFromLootTable(serverLevel, pos);

        if (!isLootrLoaded()) {
            level.setBlock(pos, state.setValue(HAS_ITEM, false), Block.UPDATE_ALL);
        }

        return InteractionResult.CONSUME;
    }

    private static void dropFromLootTable(ServerLevel level, BlockPos pos) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(LOST_ITEM_LOOT_TABLE);
        for (ItemStack stack : lootTable.getRandomItems(params)) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, stack);
        }
    }

    private static boolean isLootrLoaded() {
        return Services.PLATFORM.isModLoaded("lootr");
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LostItemBlockEntity(pos, state);
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

    private static Direction supportDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }
}
