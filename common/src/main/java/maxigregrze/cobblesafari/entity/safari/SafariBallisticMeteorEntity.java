package maxigregrze.cobblesafari.entity.safari;

import maxigregrze.cobblesafari.csboss.attack.CsBossAttackLib;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.safari.hazard.SafariHazardLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draco meteor launched upward from a Safari crater block; places {@code draco_meteorite} on impact.
 */
public class SafariBallisticMeteorEntity extends Entity {

    private static final int TELEGRAPH_TICKS = 80;
    private static final int MAX_AGE = 280;
    private static final float METEOR_DAMAGE = 18.0F;
    private static final double GRAVITY = 0.08;
    private static final double LAUNCH_SPEED = 1.0;

    private static final EntityDataAccessor<Float> DATA_SPIN =
            SynchedEntityData.defineId(SafariBallisticMeteorEntity.class, EntityDataSerializers.FLOAT);

    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private float spin;
    private int age;
    private boolean launched;
    private boolean damaged;
    private boolean damageOnly;
    private boolean falling;
    private int targetEntityId = -1;

    public SafariBallisticMeteorEntity(EntityType<? extends SafariBallisticMeteorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Crater behaviour: rising telegraph, then a ballistic rock thrown upward that places a block on impact. */
    public static void spawn(ServerLevel level, Vec3 spawn, RandomSource rng) {
        configureAndSpawn(level, spawn, rng, false, null, true, false);
    }

    /** Rock trap (base): a rock thrown upward immediately, without the telegraph delay; places a block on impact. */
    public static void spawnInstant(ServerLevel level, Vec3 spawn, RandomSource rng) {
        configureAndSpawn(level, spawn, rng, false, null, false, false);
    }

    /** Rock trap (hard): a meteorite dropped straight from the sky onto the target, damage only (no block placed). */
    public static void spawnFalling(ServerLevel level, Vec3 spawnAbove, LivingEntity target, RandomSource rng) {
        configureAndSpawn(level, spawnAbove, rng, true, target, false, true);
    }

    private static void configureAndSpawn(ServerLevel level, Vec3 spawn, RandomSource rng,
                                          boolean damageOnly, LivingEntity target,
                                          boolean telegraph, boolean falling) {
        SafariBallisticMeteorEntity meteor = new SafariBallisticMeteorEntity(ModEntities.SAFARI_BALLISTIC_METEOR, level);
        meteor.spawnX = spawn.x;
        meteor.spawnY = spawn.y;
        meteor.spawnZ = spawn.z;
        meteor.damageOnly = damageOnly;
        meteor.falling = falling;
        meteor.launched = !telegraph;
        if (target != null) {
            meteor.targetEntityId = target.getId();
        }
        meteor.moveTo(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);

        if (falling) {
            double driftX = Math.toRadians((rng.nextDouble() * 2.0 - 1.0) * 3.0);
            double driftZ = Math.toRadians((rng.nextDouble() * 2.0 - 1.0) * 3.0);
            meteor.velocityY = -LAUNCH_SPEED;
            meteor.velocityX = LAUNCH_SPEED * Math.tan(driftX);
            meteor.velocityZ = LAUNCH_SPEED * Math.tan(driftZ);
        } else {
            double pitchX = Math.toRadians((rng.nextDouble() * 2.0 - 1.0) * 5.0);
            double pitchZ = Math.toRadians((rng.nextDouble() * 2.0 - 1.0) * 5.0);
            meteor.velocityY = LAUNCH_SPEED;
            meteor.velocityX = LAUNCH_SPEED * Math.tan(pitchX);
            meteor.velocityZ = LAUNCH_SPEED * Math.tan(pitchZ);
        }

        level.addFreshEntity(meteor);
        CsBossAttackLib.sound(level, spawn.x, spawn.y, spawn.z,
                "minecraft:block.fire.ambient", SoundSource.HOSTILE, 0.8F, 1.2F);
    }

    public float getSpin() {
        return this.entityData.get(DATA_SPIN);
    }

    public boolean hasTargetEntity() {
        return targetEntityId >= 0;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (getSpin() > 0.0F) {
                emitTrailParticles();
            }
            return;
        }

        if (++age > MAX_AGE) {
            this.discard();
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        if (!launched) {
            this.setPos(spawnX, spawnY, spawnZ);
            CsBossAttackLib.risingTelegraph(serverLevel, spawnX, spawnY, spawnZ, ParticleTypes.FLAME);
            if (age >= TELEGRAPH_TICKS) {
                launched = true;
                CsBossAttackLib.sound(serverLevel, spawnX, spawnY, spawnZ,
                        "cobblemon:move.dragonclaw.actor", SoundSource.HOSTILE, 1.3F, 0.8F);
            }
            return;
        }

        double prevY = this.getY();
        double nextX = this.getX() + velocityX;
        double nextY = this.getY() + velocityY;
        double nextZ = this.getZ() + velocityZ;
        velocityY -= GRAVITY;
        spin += 12.0F;
        this.entityData.set(DATA_SPIN, spin);
        this.setPos(nextX, nextY, nextZ);

        if (!damaged) {
            damaged = SafariHazardLib.meteorSweepHit(serverLevel, this, prevY, nextY, METEOR_DAMAGE);
        }

        if (velocityY < 0.0 && hitsGround(serverLevel, nextX, nextY, nextZ)) {
            impact(serverLevel, BlockPos.containing(nextX, nextY, nextZ));
        }
    }

    private void emitTrailParticles() {
        for (int i = 0; i < 4; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 1.2;
            double oz = (this.random.nextDouble() - 0.5) * 1.2;
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX() + ox, this.getY() + 0.5 + this.random.nextDouble(), this.getZ() + oz,
                    0.0, 0.05, 0.0);
        }
    }

    private boolean hitsGround(ServerLevel level, double x, double y, double z) {
        BlockPos below = BlockPos.containing(x, y - 0.15, z);
        BlockState state = level.getBlockState(below);
        return !state.canBeReplaced() && state.isSolidRender(level, below);
    }

    private void impact(ServerLevel level, BlockPos pos) {
        if (!damageOnly) {
            BlockPos place = pos;
            while (place.getY() < level.getMaxBuildHeight() && !level.getBlockState(place).canBeReplaced()) {
                place = place.above();
            }
            if (level.getBlockState(place).canBeReplaced()) {
                level.setBlockAndUpdate(place, ModBlocks.DRACO_METEORITE.defaultBlockState());
            }
        }
        CsBossAttackLib.sound(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                "cobblemon:impact.dragon", SoundSource.HOSTILE, 1.5F, 1.0F);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 0.8F);
        this.discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SPIN, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // noSave entity
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // noSave entity
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
