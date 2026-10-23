package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.block.base.HorizontalModelBlock;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible mount that lets a player sit on a chair or a bench, after the Sit mod by bl4ckscor3.
 *
 * <p>One seat per block cell. The entity is never saved: a reload simply leaves the player
 * standing, and no orphan seat can survive its block. It removes itself as soon as it has no
 * passenger or the block under it stops being a seat.</p>
 */
public class SeatEntity extends Entity {

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    /**
     * Sits {@code player} on the seat block at {@code pos}, {@code heightPx} above its floor, facing
     * away from the backrest ({@link HorizontalModelBlock#seatFront}). Needs an empty main hand, so a
     * block can still be placed against a chair.
     */
    public static InteractionResult trySit(Level level, BlockPos pos, BlockState state, Player player, double heightPx) {
        if (!player.getMainHandItem().isEmpty() || player.isSecondaryUseActive()
                || player.isPassenger() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!(state.getBlock() instanceof HorizontalModelBlock block)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        SeatEntity seat = new SeatEntity(ModEntities.SEAT, level);
        Direction front = block.seatFront(state);
        seat.moveTo(pos.getX() + 0.5, pos.getY() + heightPx / 16.0, pos.getZ() + 0.5, front.toYRot(), 0.0F);
        level.addFreshEntity(seat);
        player.startRiding(seat);
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty() || !isSeat(level().getBlockState(blockPosition())))) {
            discard();
        }
    }

    private static boolean isSeat(BlockState state) {
        return state.getBlock() instanceof HorizontalModelBlock block && block.isSeat();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockPos seatPos = blockPosition();
        BlockState state = level().getBlockState(seatPos);
        if (state.getBlock() instanceof HorizontalModelBlock block && block.isSeat()) {
            BlockPos front = seatPos.relative(block.seatFront(state));
            Vec3 spot = safeSpot(passenger, front);
            if (spot != null) {
                return spot;
            }
        }
        Vec3 above = safeSpot(passenger, seatPos.above());
        return above != null ? above : super.getDismountLocationForPassenger(passenger);
    }

    @Nullable
    private Vec3 safeSpot(LivingEntity passenger, BlockPos pos) {
        return DismountHelper.findSafeDismountLocation(passenger.getType(), level(), pos, true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synced state: the seat is defined by where it stands.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Never saved: the entity type is registered with noSave().
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Never saved: the entity type is registered with noSave().
    }
}
