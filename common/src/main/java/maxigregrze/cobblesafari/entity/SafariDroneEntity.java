package maxigregrze.cobblesafari.entity;

import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.safari.SafariDroneExitHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The Safari exit drone (action plan 198, part G): it comes down like the Safari balloon, only
 * three times faster, then hovers 2 blocks above the ground, its screen turned towards the nearest
 * player. Right-clicking it, or standing still under it, asks the player whether they want to
 * leave the Safari ({@link SafariDroneExitHandler}).
 */
public class SafariDroneEntity extends Mob {

    /** Three times the balloon's descent (decision 12.2). */
    private static final double DESCENT_SPEED = 0.12D;
    /** Height of the drone's origin above the ground it hovers over. */
    public static final double HOVER_HEIGHT = 2.0D;
    /** A player this close gets the drone's screen turned towards them (decision 12.3). */
    private static final double FACE_PLAYER_RANGE = 6.0D;
    /** Degrees per tick the drone turns at, so it swivels rather than snaps. */
    private static final float TURN_SPEED = 10.0F;

    public SafariDroneEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    /** As tough as a player in full netherite, with 5 health points to compensate (decision 12.3). */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // No AI goals - the drone only comes down and hovers
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) {
            return;
        }

        double targetY = this.hoverY();
        if (this.getY() - targetY > DESCENT_SPEED) {
            this.setDeltaMovement(0.0D, -DESCENT_SPEED, 0.0D);
        } else {
            // Close enough, or below it because the ground rose under it: settle exactly on it.
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide && this.getY() != targetY) {
                this.setPos(this.getX(), targetY, this.getZ());
            }
        }

        if (!this.level().isClientSide) {
            this.faceNearestPlayer();
            if (this.tickCount > MiscConfig.getBalloonLifetimeTicks()) {
                this.discard();
            }
        }
    }

    /** Turns the screen (the model's front) towards the nearest player in range; the client lerps it. */
    private void faceNearestPlayer() {
        Player nearest = this.level().getNearestPlayer(this, FACE_PLAYER_RANGE);
        if (nearest == null) {
            return;
        }
        double dx = nearest.getX() - this.getX();
        double dz = nearest.getZ() - this.getZ();
        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(this.getYRot(), targetYaw, TURN_SPEED);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    /** The ground under the drone's column (leaves and fluids included), plus the hover height. */
    private double hoverY() {
        return this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, this.getBlockX(), this.getBlockZ()) + HOVER_HEIGHT;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !this.isAlive()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            SafariDroneExitHandler.requestExit(serverPlayer);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Hovers in place: never pushed, never pushes
    }

    @Override
    protected boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** Players walk under it to use it: it must not block them. */
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // No fall damage
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }
}
