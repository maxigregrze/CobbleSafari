package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.effect.RedShackledEffects;
import maxigregrze.cobblesafari.init.ModEffects;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Projectile « chaîne rouge » ({@code distortion_5}) : un disque plat (texture
 * {@code attack_redchain.png}, rendu parallèle au sol et tournant comme le projectile de l'objet
 * Chaîne Rouge) qui glisse très lentement en ligne droite et applique l'effet <i>Enchaîné Rouge</i>
 * à chaque participant traversé (une seule fois par joueur, il continue sa course).
 */
public class AttackRedChainEntity extends AbstractAttackEntity {

    private static final String KEY_VX = "Vx";
    private static final String KEY_VY = "Vy";
    private static final String KEY_VZ = "Vz";

    private Vec3 velocity = Vec3.ZERO;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public AttackRedChainEntity(EntityType<? extends AttackRedChainEntity> type, Level level) {
        super(type, level);
    }

    public static AttackRedChainEntity spawn(ServerLevel level, double x, double y, double z,
                                             int sessionId, Vec3 velocity, float yawDeg) {
        AttackRedChainEntity e = new AttackRedChainEntity(ModEntities.ATTACK_RED_CHAIN, level);
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
            p.addEffect(new MobEffectInstance(ModEffects.RED_SHACKLED.holder,
                    RedShackledEffects.DURATION_TICKS_DEFAULT, 0, false, true));
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
        return 140;
    }
}
