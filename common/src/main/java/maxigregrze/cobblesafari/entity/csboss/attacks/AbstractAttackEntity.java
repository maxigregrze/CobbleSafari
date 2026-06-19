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
 * Base for CSBoss attack entities: ultra-light entities with no physics or AI,
 * immune, no-gravity, passing through blocks, driven by attack code or by
 * internal self-replication. A safety TTL ({@link #maxLifespan()}) prevents leaks if the
 * session disappears before normal cleanup.
 */
public abstract class AbstractAttackEntity extends Entity {

    protected static final String KEY_SESSION = "SessionId";
    protected static final String KEY_AGE = "Age";

    protected int sessionId;
    protected int age;
    private double originX;
    private double originZ;
    private double maxTravel = -1.0;

    protected AbstractAttackEntity(EntityType<? extends AbstractAttackEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Maximum lifespan in ticks (anti-leak safety net). */
    protected abstract int maxLifespan();

    public int getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    /** Caps horizontal travel from the position at call time; {@code discard()} when exceeded. */
    public void setMaxTravel(double maxTravel) {
        this.originX = getX();
        this.originZ = getZ();
        this.maxTravel = maxTravel;
    }

    /** Current session (or {@code null} if ended) — for self-replication. */
    @Nullable
    protected BossBattleSession session() {
        return BossBattleManager.getSession(this.sessionId);
    }

    /** Session boss (or {@code null}) — for entities attached to the boss's rotating frame. */
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
        if (this.maxTravel > 0.0) {
            double dx = getX() - this.originX;
            double dz = getZ() - this.originZ;
            if (dx * dx + dz * dz > this.maxTravel * this.maxTravel) {
                this.discard();
                return;
            }
        }
        serverTick((ServerLevel) this.level());
    }

    /** Specific server logic (self-replication). Default: nothing. */
    protected void serverTick(ServerLevel level) {
    }

    /** Client particles / effects. Default: nothing. */
    protected void clientTick() {
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // Subclasses add their accessors by overriding + super().
    }

    // --- Immunity / inertia ------------------------------------------------------

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
