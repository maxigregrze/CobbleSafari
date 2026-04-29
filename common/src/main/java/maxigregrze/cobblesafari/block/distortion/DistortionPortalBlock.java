package maxigregrze.cobblesafari.block.distortion;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DistortionPortalBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<DistortionPortalBlock> CODEC = simpleCodec(DistortionPortalBlock::new);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    private static final double TRIGGER_INSET = 0.25D;
    private static final int TELEPORT_OFFSET_BLOCKS = 128;
    private static final int TELEPORT_COOLDOWN_TICKS = 20;
    private static final Map<UUID, Long> LAST_DISTORTION_TELEPORT_TICK = new ConcurrentHashMap<>();

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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCreative() && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            Mode nextMode = state.getValue(MODE) == Mode.TOP ? Mode.BOTTOM : Mode.TOP;
            level.setBlock(pos, state.setValue(MODE, nextMode), Block.UPDATE_ALL);
            player.sendSystemMessage(Component.translatable("cobblesafari.distortion_portal.mode." + nextMode.getSerializedName()));
            return InteractionResult.CONSUME;
        }

        if (player.isCreative() || player.isSpectator() || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && tryDistortionTeleport(serverLevel, pos, state, serverPlayer, false, true)) {
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    static AABB triggerBounds(BlockPos pos) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        return new AABB(
                x + TRIGGER_INSET,
                y + TRIGGER_INSET,
                z + TRIGGER_INSET,
                x + 1.0D - TRIGGER_INSET,
                y + 1.0D - TRIGGER_INSET,
                z + 1.0D - TRIGGER_INSET
        );
    }

    public static void handlePlayerInPortalVolume(ServerLevel serverLevel, BlockPos pos, BlockState state, ServerPlayer serverPlayer) {
        tryDistortionTeleport(serverLevel, pos, state, serverPlayer, true, false);
    }

    private static boolean tryDistortionTeleport(
            ServerLevel serverLevel,
            BlockPos portalPos,
            BlockState state,
            ServerPlayer player,
            boolean requireInsideTriggerVolume,
            boolean playPortalTriggerAtPortalOnUse
    ) {
        if (requireInsideTriggerVolume && !player.getBoundingBox().intersects(triggerBounds(portalPos))) {
            return false;
        }

        MinecraftServer server = serverLevel.getServer();
        long tick = server.getTickCount();
        UUID id = player.getUUID();
        Long last = LAST_DISTORTION_TELEPORT_TICK.get(id);
        if (last != null && tick - last < TELEPORT_COOLDOWN_TICKS) {
            return false;
        }

        Mode mode = state.getValue(MODE);
        int delta = mode == Mode.TOP ? TELEPORT_OFFSET_BLOCKS : -TELEPORT_OFFSET_BLOCKS;
        int targetY = portalPos.getY() + delta;
        int clampedY = Math.clamp(targetY, serverLevel.getMinBuildHeight() + 1, serverLevel.getMaxBuildHeight() - 2);

        double destX = portalPos.getX() + 0.5D;
        double destZ = portalPos.getZ() + 0.5D;
        double destY = clampedY + 0.01D;

        LAST_DISTORTION_TELEPORT_TICK.put(id, tick);

        try {
            BlockPos destChunkPos = BlockPos.containing(destX, destY, destZ);
            serverLevel.getChunk(destChunkPos.getX() >> 4, destChunkPos.getZ() >> 4);

            if (playPortalTriggerAtPortalOnUse && !player.getBoundingBox().intersects(triggerBounds(portalPos))) {
                serverLevel.playSound(player, portalPos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.5f, 1.0f);
            }

            player.teleportTo(serverLevel, destX, destY, destZ, player.getYRot(), player.getXRot());
            player.resetFallDistance();
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

            serverLevel.playSound(null, destX, destY, destZ, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.5f, 1.0f);
            return true;
        } catch (Exception e) {
            LAST_DISTORTION_TELEPORT_TICK.remove(id);
            LOGGER.error("[DistortionPortal] Teleport failed: {}", e.getMessage(), e);
            return false;
        }
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
