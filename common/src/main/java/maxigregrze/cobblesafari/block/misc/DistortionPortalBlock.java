package maxigregrze.cobblesafari.block.misc;

import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DistortionPortalBlock extends BaseEntityBlock {
    public static final MapCodec<DistortionPortalBlock> CODEC = simpleCodec(DistortionPortalBlock::new);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    private static final int TELEPORT_OFFSET_BLOCKS = 128;
    private static final int TELEPORT_COOLDOWN_TICKS = 60;
    private static final Map<UUID, Long> LAST_TELEPORT_GAME_TIME = new ConcurrentHashMap<>();

    public DistortionPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, Mode.TOP));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(MODE, Mode.TOP);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            handlePlayerInPortalVolume(serverLevel, pos, state, serverPlayer);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isCreative()) {
            return InteractionResult.PASS;
        }

        Mode nextMode = state.getValue(MODE) == Mode.TOP ? Mode.BOTTOM : Mode.TOP;
        level.setBlock(pos, state.setValue(MODE, nextMode), Block.UPDATE_ALL);
        player.sendSystemMessage(Component.translatable("message.cobblesafari.distortion_portal.mode." + nextMode.getSerializedName()));
        return InteractionResult.CONSUME;
    }

    public static void handlePlayerInPortalVolume(ServerLevel serverLevel, BlockPos pos, BlockState state, ServerPlayer serverPlayer) {
        long gameTime = serverLevel.getGameTime();
        UUID id = serverPlayer.getUUID();
        long last = LAST_TELEPORT_GAME_TIME.getOrDefault(id, Long.MIN_VALUE);
        if (gameTime - last < TELEPORT_COOLDOWN_TICKS) {
            return;
        }

        int delta = state.getValue(MODE) == Mode.TOP ? TELEPORT_OFFSET_BLOCKS : -TELEPORT_OFFSET_BLOCKS;
        int targetY = pos.getY() + delta;
        int clampedY = Math.clamp(targetY, serverLevel.getMinBuildHeight() + 1, serverLevel.getMaxBuildHeight() - 2);

        double destX = pos.getX() + 0.5D;
        double destZ = pos.getZ() + 0.5D;
        double destY = clampedY + 0.01D;

        serverPlayer.teleportTo(serverLevel, destX, destY, destZ, serverPlayer.getYRot(), serverPlayer.getXRot());
        serverPlayer.resetFallDistance();
        LAST_TELEPORT_GAME_TIME.put(id, gameTime);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DistortionPortalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DISTORTION_PORTAL, DistortionPortalBlockEntity::tick);
    }

    public enum Mode implements StringRepresentable {
        TOP("top"),
        BOTTOM("bottom");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
