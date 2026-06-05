package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.BossBattleManager;
import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Base des entités d'attaque CSBoss (plan 107) : entités ultra‑légères sans physique ni AI,
 * immunisées, no‑gravity, traversant les blocs, pilotées par le code d'attaque ou par
 * auto‑réplication interne. Un TTL de sécurité ({@link #maxLifespan()}) évite toute fuite si la
 * session disparaît avant le nettoyage normal.
 */
public abstract class AbstractAttackEntity extends Entity {

    protected static final String KEY_SESSION = "SessionId";
    protected static final String KEY_AGE = "Age";

    protected int sessionId;
    protected int age;

    protected AbstractAttackEntity(EntityType<? extends AbstractAttackEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Durée de vie maximale en ticks (filet anti‑fuite). */
    protected abstract int maxLifespan();

    public int getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    /** Session courante (ou {@code null} si terminée) — pour l'auto‑réplication. */
    @Nullable
    protected BossBattleSession session() {
        return BossBattleManager.getSession(this.sessionId);
    }

    /** Boss de la session (ou {@code null}) — pour les entités attachées au repère tournant du boss. */
    @Nullable
    protected CsBossEntity boss(ServerLevel level) {
        BossBattleSession s = session();
        return s != null && level.getEntity(s.getBossUuid()) instanceof CsBossEntity b ? b : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientTick();
            return;
        }
        if (++this.age >= maxLifespan()) {
            this.discard();
            return;
        }
        serverTick((ServerLevel) this.level());
    }

    /** Logique serveur spécifique (auto‑réplication). Par défaut : rien. */
    protected void serverTick(ServerLevel level) {
    }

    /** Particules / effets client. Par défaut : rien. */
    protected void clientTick() {
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // Les sous-classes ajoutent leurs accesseurs en surchargeant + super().
    }

    // --- Immunité / inertie ------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
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
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.sessionId = tag.getInt(KEY_SESSION);
        this.age = tag.getInt(KEY_AGE);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(KEY_SESSION, this.sessionId);
        tag.putInt(KEY_AGE, this.age);
    }
}
