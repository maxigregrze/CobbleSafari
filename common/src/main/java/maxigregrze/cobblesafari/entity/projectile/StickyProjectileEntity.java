package maxigregrze.cobblesafari.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * A harmless projectile that sticks into the first block or entity it touches, then vanishes after
 * {@link #stuckLifetimeTicks()} (action plan 192, section 3). Shared by the Stellar Canon shell and
 * the Origami Carrier paper plane.
 * <p>The flight is server-authoritative: the client moves along the synced velocity, the server
 * corrects positions. Once stuck in an entity, both sides re-derive the position every tick from the
 * synced host id and host-relative offset, so it follows without lag.</p>
 * <p>Extends {@code ThrowableProjectile} only because {@code Projectile}'s constructor is package-private:
 * {@link #tick()} replaces its physics entirely.</p>
 * <p>Rotation uses this class's own convention ({@code yRot = atan2(-dx, dz)}, {@code xRot} positive
 * when diving), matched by {@code BakedModelProjectileRenderer}.</p>
 */
public abstract class StickyProjectileEntity extends ThrowableProjectile {

    public static final byte STATE_FLYING = 0;
    public static final byte STATE_STUCK_BLOCK = 1;
    public static final byte STATE_STUCK_ENTITY = 2;

    private static final int MAX_FLIGHT_TICKS = 1200;
    private static final int OWNER_GRACE_TICKS = 5;
    /** Ticks a reloaded entity-stuck projectile may wait for its host to load before giving up. */
    private static final int HOST_RESOLVE_GRACE_TICKS = 40;

    private static final EntityDataAccessor<Byte> DATA_STATE =
            SynchedEntityData.defineId(StickyProjectileEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_HOST_ID =
            SynchedEntityData.defineId(StickyProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_HOST_OFFSET =
            SynchedEntityData.defineId(StickyProjectileEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> DATA_HOST_YAW_OFFSET =
            SynchedEntityData.defineId(StickyProjectileEntity.class, EntityDataSerializers.FLOAT);

    private int flightTicks;
    private int stuckTicks;
    private int hostMissingTicks;
    private BlockPos stuckBlock;
    private UUID hostUuid;

    protected StickyProjectileEntity(EntityType<? extends StickyProjectileEntity> type, Level level) {
        super(type, level);
    }

    /** Vanilla-style constructor helper: placed at the shooter's eyes, owned by it. */
    protected void initFromShooter(LivingEntity shooter) {
        setOwner(shooter);
        setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
    }

    /** Velocity for the next tick of free flight, given the current one. */
    protected abstract Vec3 nextFlightMotion(Vec3 motion);

    /** How long the projectile stays once stuck, in ticks. */
    protected abstract int stuckLifetimeTicks();

    /** Per-tick hook while stuck (both sides), e.g. for particles. */
    protected void stuckTick() {
        // no-op by default
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STATE, STATE_FLYING);
        builder.define(DATA_HOST_ID, -1);
        builder.define(DATA_HOST_OFFSET, new Vector3f());
        builder.define(DATA_HOST_YAW_OFFSET, 0.0f);
    }

    public byte state() {
        return entityData.get(DATA_STATE);
    }

    @Override
    public void tick() {
        baseTick();
        switch (state()) {
            case STATE_STUCK_BLOCK -> tickStuckInBlock();
            case STATE_STUCK_ENTITY -> tickStuckInEntity();
            default -> tickFlying();
        }
    }

    private void tickFlying() {
        if (!level().isClientSide()) {
            flightTicks++;
            if (flightTicks > MAX_FLIGHT_TICKS || getY() < level().getMinBuildHeight()) {
                discard();
                return;
            }
            if (isInWater()) {
                splashAndDiscard();
                return;
            }
            setDeltaMovement(nextFlightMotion(getDeltaMovement()));
        }
        Vec3 motion = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            if (!level().isClientSide()) {
                onStick(hit, motion);
            } else if (hit instanceof BlockHitResult) {
                // Stop locally against the wall while the stuck state is on its way.
                setPos(hit.getLocation());
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        faceMotion(motion);
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
    }

    private void onStick(HitResult hit, Vec3 motion) {
        Vec3 backOff = motion.lengthSqr() > 1.0e-6 ? motion.normalize().scale(0.05) : Vec3.ZERO;
        setPos(hit.getLocation().subtract(backOff));
        setDeltaMovement(Vec3.ZERO);
        stuckTicks = 0;
        if (hit instanceof EntityHitResult entityHit) {
            Entity host = entityHit.getEntity();
            float hostYaw = hostYaw(host);
            Vec3 local = position().subtract(host.position()).yRot(hostYaw * Mth.DEG_TO_RAD);
            entityData.set(DATA_HOST_OFFSET, local.toVector3f());
            entityData.set(DATA_HOST_YAW_OFFSET, getYRot() - hostYaw);
            entityData.set(DATA_HOST_ID, host.getId());
            hostUuid = host.getUUID();
            entityData.set(DATA_STATE, STATE_STUCK_ENTITY);
        } else if (hit instanceof BlockHitResult blockHit) {
            stuckBlock = blockHit.getBlockPos();
            entityData.set(DATA_STATE, STATE_STUCK_BLOCK);
        }
    }

    private void tickStuckInBlock() {
        setDeltaMovement(Vec3.ZERO);
        stuckTick();
        if (level().isClientSide()) {
            return;
        }
        if (stuckBlock == null
                || level().getBlockState(stuckBlock).getCollisionShape(level(), stuckBlock).isEmpty()) {
            discard();
            return;
        }
        countDownStuck();
    }

    private void tickStuckInEntity() {
        setDeltaMovement(Vec3.ZERO);
        Entity host = resolveHost();
        if (host == null || !host.isAlive()) {
            if (!level().isClientSide() && (host != null || ++hostMissingTicks > HOST_RESOLVE_GRACE_TICKS)) {
                discard();
            }
            return;
        }
        float hostYaw = hostYaw(host);
        Vec3 local = new Vec3(entityData.get(DATA_HOST_OFFSET));
        Vec3 world = host.position().add(local.yRot(-hostYaw * Mth.DEG_TO_RAD));
        setPos(world);
        setYRot(hostYaw + entityData.get(DATA_HOST_YAW_OFFSET));
        yRotO = getYRot();
        xRotO = getXRot();
        stuckTick();
        if (!level().isClientSide()) {
            countDownStuck();
        }
    }

    private Entity resolveHost() {
        int id = entityData.get(DATA_HOST_ID);
        Entity host = id >= 0 ? level().getEntity(id) : null;
        if (host == null && hostUuid != null && level() instanceof ServerLevel serverLevel) {
            host = serverLevel.getEntity(hostUuid);
            if (host != null) {
                entityData.set(DATA_HOST_ID, host.getId());
            }
        }
        return host;
    }

    private void countDownStuck() {
        stuckTicks++;
        if (stuckTicks >= stuckLifetimeTicks()) {
            discard();
        }
    }

    private void splashAndDiscard() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 8, 0.2, 0.1, 0.2, 0.0);
        }
        discard();
    }

    private static float hostYaw(Entity host) {
        return host instanceof LivingEntity living ? living.yBodyRot : host.getYRot();
    }

    /** Points the projectile along {@code motion}, keeping last tick's values for interpolation. */
    protected void faceMotion(Vec3 motion) {
        yRotO = getYRot();
        xRotO = getXRot();
        double horizontal = motion.horizontalDistance();
        if (motion.lengthSqr() < 1.0e-7) {
            return;
        }
        setYRot((float) (Mth.atan2(-motion.x, motion.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(-motion.y, horizontal) * Mth.RAD_TO_DEG));
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // The shot starts inside the shooter: ignore it for the first ticks.
        return super.canHitEntity(target) && (flightTicks > OWNER_GRACE_TICKS || target != getOwner());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Host entity ids do not survive a reload: the UUID is resolved again on the next ticks.
        tag.putByte("StickyState", state());
        tag.putInt("FlightTicks", flightTicks);
        tag.putInt("StuckTicks", stuckTicks);
        if (stuckBlock != null) {
            tag.put("StuckBlock", NbtUtils.writeBlockPos(stuckBlock));
        }
        if (hostUuid != null) {
            tag.putUUID("Host", hostUuid);
            Vector3f offset = entityData.get(DATA_HOST_OFFSET);
            tag.putFloat("HostOffsetX", offset.x());
            tag.putFloat("HostOffsetY", offset.y());
            tag.putFloat("HostOffsetZ", offset.z());
            tag.putFloat("HostYawOffset", entityData.get(DATA_HOST_YAW_OFFSET));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        flightTicks = tag.getInt("FlightTicks");
        stuckTicks = tag.getInt("StuckTicks");
        stuckBlock = NbtUtils.readBlockPos(tag, "StuckBlock").orElse(null);
        if (tag.hasUUID("Host")) {
            hostUuid = tag.getUUID("Host");
            entityData.set(DATA_HOST_OFFSET, new Vector3f(
                    tag.getFloat("HostOffsetX"), tag.getFloat("HostOffsetY"), tag.getFloat("HostOffsetZ")));
            entityData.set(DATA_HOST_YAW_OFFSET, tag.getFloat("HostYawOffset"));
        }
        byte saved = tag.getByte("StickyState");
        if ((saved == STATE_STUCK_ENTITY && hostUuid == null) || (saved == STATE_STUCK_BLOCK && stuckBlock == null)) {
            saved = STATE_FLYING;
        }
        entityData.set(DATA_STATE, saved);
    }
}
