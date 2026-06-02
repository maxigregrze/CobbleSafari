package maxigregrze.cobblesafari.entity.csboss;

import maxigregrze.cobblesafari.csboss.CsBossDamage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Projectile « bullet-hell » ultra-léger (plan 100 § 12.3) : pas d'AI ni de physique,
 * traverse les murs, durée de vie fixe, détection limitée au Set de participants.
 */
public class CsBossBulletEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_VX =
            SynchedEntityData.defineId(CsBossBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_VY =
            SynchedEntityData.defineId(CsBossBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_VZ =
            SynchedEntityData.defineId(CsBossBulletEntity.class, EntityDataSerializers.FLOAT);

    private static final String KEY_AGE = "Age";
    private static final String KEY_SESSION = "SessionId";

    private static final int MAX_AGE = 60;
    private static final float DEFAULT_DAMAGE = 16.0F; // 8 cœurs

    private int age;
    private int sessionId;
    private float damage = DEFAULT_DAMAGE;
    private final Set<UUID> participants = new HashSet<>();

    public CsBossBulletEntity(EntityType<? extends CsBossBulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void configure(int sessionId, Set<UUID> participants, Vec3 velocity, float damage) {
        this.sessionId = sessionId;
        this.participants.clear();
        this.participants.addAll(participants);
        this.damage = damage;
        this.entityData.set(DATA_VX, (float) velocity.x);
        this.entityData.set(DATA_VY, (float) velocity.y);
        this.entityData.set(DATA_VZ, (float) velocity.z);
    }

    public int getSessionId() {
        return this.sessionId;
    }

    private Vec3 velocity() {
        return new Vec3(this.entityData.get(DATA_VX), this.entityData.get(DATA_VY), this.entityData.get(DATA_VZ));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VX, 0.0F);
        builder.define(DATA_VY, 0.0F);
        builder.define(DATA_VZ, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = velocity();
        this.setPos(this.getX() + v.x, this.getY() + v.y, this.getZ() + v.z);

        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.HEART,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), 0, 0, 0);
            return;
        }

        if (++this.age >= MAX_AGE) {
            this.discard();
            return;
        }

        AABB box = this.getBoundingBox();
        for (UUID uuid : this.participants) {
            if (this.level().getPlayerByUUID(uuid) instanceof ServerPlayer p
                    && p.isAlive() && p.getBoundingBox().intersects(box)) {
                p.hurt(CsBossDamage.bullet(this.level()), this.damage);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt(KEY_AGE);
        this.sessionId = tag.getInt(KEY_SESSION);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(KEY_AGE, this.age);
        tag.putInt(KEY_SESSION, this.sessionId);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
