package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Lame d'Air ({@code base_flying_2}, plan 199 § 10.4) : lame plate et tournoyante qui file en ligne
 * droite à travers les murs, comme les balles, et touche une fois chaque joueur. L'orientation de
 * rendu suit la vitesse (synchronisée en lacet).
 */
public class AttackAirSlashEntity extends AbstractAttackEntity {

    private static final int LIFESPAN = 60;
    private static final double HALF_WIDTH = 0.8;
    private static final double HEIGHT = 0.5;

    private static final EntityDataAccessor<Float> DATA_HEADING =
            SynchedEntityData.defineId(AttackAirSlashEntity.class, EntityDataSerializers.FLOAT);

    private Vec3 velocity = Vec3.ZERO;
    private float damage;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public AttackAirSlashEntity(EntityType<? extends AttackAirSlashEntity> type, Level level) {
        super(type, level);
    }

    public static AttackAirSlashEntity spawn(ServerLevel level, Vec3 pos, int sessionId, Vec3 velocity, float damage) {
        AttackAirSlashEntity e = new AttackAirSlashEntity(ModEntities.ATTACK_AIR_SLASH, level);
        e.setSessionId(sessionId);
        e.velocity = velocity;
        e.damage = damage;
        e.entityData.set(DATA_HEADING, (float) Math.toDegrees(Math.atan2(velocity.z, velocity.x)));
        e.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    public float getHeading() {
        return this.entityData.get(DATA_HEADING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HEADING, 0.0F);
    }

    @Override
    protected int maxLifespan() {
        return LIFESPAN;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        BossBattleSession session = session();
        if (session == null) {
            return;
        }
        AABB box = new AABB(getX() - HALF_WIDTH, getY(), getZ() - HALF_WIDTH,
                getX() + HALF_WIDTH, getY() + HEIGHT, getZ() + HALF_WIDTH);
        for (ServerPlayer p : session.aliveParticipants(level)) {
            if (!alreadyHit.contains(p.getUUID()) && p.getBoundingBox().intersects(box)) {
                alreadyHit.add(p.getUUID());
                CsBossDamage.hurt(p, session, damage);
            }
        }
    }

    @Override
    protected void clientTick() {
        if (this.random.nextInt(2) == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    getX(), getY() + 0.2, getZ(), 0.0, 0.0, 0.0);
        }
    }
}
