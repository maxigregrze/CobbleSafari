package maxigregrze.cobblesafari.block.misc;

import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class DistortionPortalBlockEntity extends BlockEntity {
    private static final double TRACKING_RADIUS = 16.0;
    private static final float IDLE_SPIN_DEGREES = 0.5f;
    private static final float TRACKING_SMOOTHING = 0.25f;

    private static final float SPIN_Z_DEGREES_PER_TICK = 1.5f;

    private float currentYaw;
    private float previousYaw;
    private float targetYaw;
    private float currentSpinZ;
    private float previousSpinZ;

    public DistortionPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISTORTION_PORTAL, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DistortionPortalBlockEntity blockEntity) {
        if (!state.is(ModBlocks.DISTORTION_PORTAL)) {
            return;
        }
        if (level.isClientSide()) {
            clientTick(level, pos, state, blockEntity);
        } else {
            serverTick(level, pos, state);
        }
    }

    private static void serverTick(Level level, BlockPos pos, BlockState state) {
        ServerLevel serverLevel = (ServerLevel) level;
        AABB aabb = new AABB(pos).expandTowards(0.0D, 0.5D, 0.0D).inflate(0.01D);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, aabb)) {
            DistortionPortalBlock.handlePlayerInPortalVolume(serverLevel, pos, state, player);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, DistortionPortalBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            return;
        }
        if (!state.is(ModBlocks.DISTORTION_PORTAL)) {
            return;
        }

        blockEntity.previousYaw = blockEntity.currentYaw;
        blockEntity.previousSpinZ = blockEntity.currentSpinZ;
        blockEntity.currentSpinZ += SPIN_Z_DEGREES_PER_TICK;
        Player nearestPlayer = level.getNearestPlayer(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                TRACKING_RADIUS,
                false
        );

        if (nearestPlayer != null) {
            double dx = nearestPlayer.getX() - (pos.getX() + 0.5);
            double dz = nearestPlayer.getZ() - (pos.getZ() + 0.5);
            blockEntity.targetYaw = 90.0f - (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI));
        } else {
            blockEntity.targetYaw = Mth.wrapDegrees(blockEntity.targetYaw + IDLE_SPIN_DEGREES);
        }

        float yawDelta = Mth.wrapDegrees(blockEntity.targetYaw - blockEntity.currentYaw);
        blockEntity.currentYaw = Mth.wrapDegrees(blockEntity.currentYaw + yawDelta * TRACKING_SMOOTHING);
    }

    public float getCurrentYaw() {
        return currentYaw;
    }

    public float getPreviousYaw() {
        return previousYaw;
    }

    public float getCurrentSpinZ() {
        return currentSpinZ;
    }

    public float getPreviousSpinZ() {
        return previousSpinZ;
    }
}
