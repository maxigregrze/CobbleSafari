package maxigregrze.cobblesafari.block.misc;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import org.slf4j.Logger;

import java.util.UUID;

public class LostItemBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
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
        Direction horizontalFacing = face == AttachFace.WALL
                ? clickedFace
                : context.getHorizontalDirection().getOpposite();
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
        return RenderShape.ENTITYBLOCK_ANIMATED;
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
        
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("[LostItem] Player is not ServerPlayer!");
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof LostItemBlockEntity blockEntity)) {
            LOGGER.warn("[LostItem] No BlockEntity found at position!");
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            LOGGER.warn("[LostItem] Level is not ServerLevel!");
            return InteractionResult.PASS;
        }
        
        if (player.isCreative() && player.isShiftKeyDown()) {
            LOGGER.info("[LostItem] Player {} is resetting claims...", player.getName().getString());
            blockEntity.resetClaims();
            return InteractionResult.CONSUME;
        }

        UUID playerId = serverPlayer.getUUID();
        boolean alreadyClaimed = blockEntity.hasClaimed(playerId);
        
        if (alreadyClaimed) {
            return InteractionResult.CONSUME;
        }

        blockEntity.tryClaim(playerId);
        giveOrDropFromLootTable(serverLevel, pos, serverPlayer);
        return InteractionResult.CONSUME;
    }

    private static void giveOrDropFromLootTable(ServerLevel level, BlockPos pos, ServerPlayer player) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(LOST_ITEM_LOOT_TABLE);
        
        lootTable.getRandomItems(params).stream().findFirst().ifPresent(stack -> {
            if (!stack.isEmpty()) {
                boolean added = player.getInventory().add(stack);
                if (!added) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, stack);
                }
            }
        });
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
