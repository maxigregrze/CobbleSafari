package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.ParticipantState;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Cloche de la Tour ({@code jump_1}, plan 199 § 10.2) : première entité d'attaque que les joueurs
 * peuvent frapper. Cassée par 3 coups de n'importe quelle source (main nue, arme, projectile, quels
 * que soient les dégâts) portés par un participant, ou par 1 baume (direct ou éclaboussure).
 * La chute, les sonneries et le soin du boss sont pilotés par {@code JumpBellsAttack}.
 */
public class AttackBellEntity extends AbstractAttackEntity {

    public static final int HITS_TO_BREAK = 3;
    private static final int LIFESPAN = 320; // filet anti-fuite (l'attaque dure 240 t)

    private static final EntityDataAccessor<Integer> DATA_HITS =
            SynchedEntityData.defineId(AttackBellEntity.class, EntityDataSerializers.INT);

    private boolean broken;

    public AttackBellEntity(EntityType<? extends AttackBellEntity> type, Level level) {
        super(type, level);
    }

    public static AttackBellEntity spawn(ServerLevel level, double x, double y, double z, int sessionId) {
        AttackBellEntity e = new AttackBellEntity(ModEntities.ATTACK_BELL, level);
        e.setSessionId(sessionId);
        e.setPos(x, y, z);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected int maxLifespan() {
        return LIFESPAN;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HITS, 0);
    }

    public int getHits() {
        return this.entityData.get(DATA_HITS);
    }

    public boolean isBroken() {
        return broken || isRemoved();
    }

    // --- Coups --------------------------------------------------------------------

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return false;
    }

    /** Un coup compte s'il vient d'un participant actif (directement ou via son projectile). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isBroken() || !(source.getEntity() instanceof ServerPlayer attacker)) {
            return false;
        }
        BossBattleSession s = session();
        ParticipantState st = s == null ? null : s.getParticipants().get(attacker.getUUID());
        if (st == null || st.discarded) {
            return false;
        }
        int hits = getHits() + 1;
        this.entityData.set(DATA_HITS, hits);
        level().playSound(null, getX(), getY() + 1.5, getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE,
                0.8F, 1.4F);
        if (hits >= HITS_TO_BREAK) {
            breakBell();
        }
        return true;
    }

    /** Baume (coup direct ou éclaboussure) : cassée d'un coup. */
    public void breakByBalm() {
        if (!level().isClientSide && !isBroken()) {
            breakBell();
        }
    }

    private void breakBell() {
        broken = true;
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GOLD_BLOCK.defaultBlockState()),
                    getX(), getY() + 1.2, getZ(), 60, 0.8, 0.8, 0.8, 0.2);
            sl.playSound(null, getX(), getY() + 1.0, getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE,
                    1.2F, 0.6F);
        }
        discard();
    }
}
