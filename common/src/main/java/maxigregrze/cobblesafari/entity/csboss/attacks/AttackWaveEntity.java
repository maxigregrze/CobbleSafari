package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Vague d'eau projectile (plan 110 § base_water_2) : se déplace en ligne droite (modèle
 * {@code vertical_wave}), inflige 8 dégâts et repousse fortement les participants au contact
 * (« la vague ne traverse pas le joueur »). Trois vagues forment un arc envoyé par le boss.
 */
public class AttackWaveEntity extends AbstractAttackEntity {

    private static final String KEY_VX = "Vx";
    private static final String KEY_VY = "Vy";
    private static final String KEY_VZ = "Vz";
    private static final float DAMAGE = 8.0F;
    private static final double KNOCKBACK = 1.2;

    private Vec3 velocity = Vec3.ZERO;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public AttackWaveEntity(EntityType<? extends AttackWaveEntity> type, Level level) {
        super(type, level);
    }

    public static AttackWaveEntity spawn(ServerLevel level, double x, double y, double z,
                                         int sessionId, Vec3 velocity, float yawDeg) {
        AttackWaveEntity e = new AttackWaveEntity(ModEntities.ATTACK_WAVE, level);
        e.setSessionId(sessionId);
        e.velocity = velocity;
        e.moveTo(x, y, z, yawDeg, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        BossBattleSession session = session();
        if (session == null) {
            return;
        }
        for (ServerPlayer p : session.aliveParticipants(level)) {
            if (alreadyHit.contains(p.getUUID()) || !p.getBoundingBox().intersects(getBoundingBox())) {
                continue;
            }
            alreadyHit.add(p.getUUID());
            p.hurt(CsBossDamage.bullet(level), DAMAGE);
            // Repousse le joueur dans le sens de déplacement de la vague (il ne la traverse pas).
            p.knockback(KNOCKBACK, -velocity.x, -velocity.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.velocity = new Vec3(tag.getDouble(KEY_VX), tag.getDouble(KEY_VY), tag.getDouble(KEY_VZ));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble(KEY_VX, velocity.x);
        tag.putDouble(KEY_VY, velocity.y);
        tag.putDouble(KEY_VZ, velocity.z);
    }

    @Override
    protected int maxLifespan() {
        return 50;
    }
}
