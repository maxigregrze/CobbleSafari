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
 * "Shadow ball" projectile ({@code base_ghost_4}): a flat 2×2 ball (texture
 * {@code attack_shadowball.png}, camera-facing and slowly spinning on its forward axis) that moves
 * in a straight line and deals 8 damage on contact (once per player; it passes through).
 */
public class AttackShadowballEntity extends AbstractAttackEntity {

    private static final String KEY_VX = "Vx";
    private static final String KEY_VY = "Vy";
    private static final String KEY_VZ = "Vz";
    private static final float DAMAGE = 14.0F; // slow ball, individually dodgeable

    private Vec3 velocity = Vec3.ZERO;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public AttackShadowballEntity(EntityType<? extends AttackShadowballEntity> type, Level level) {
        super(type, level);
    }

    public static AttackShadowballEntity spawn(ServerLevel level, double x, double y, double z,
                                               int sessionId, Vec3 velocity) {
        AttackShadowballEntity e = new AttackShadowballEntity(ModEntities.ATTACK_SHADOWBALL, level);
        e.setSessionId(sessionId);
        e.velocity = velocity;
        e.moveTo(x, y, z, 0.0F, 0.0F);
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
        return 60;
    }
}
