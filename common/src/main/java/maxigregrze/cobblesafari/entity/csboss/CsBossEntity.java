package maxigregrze.cobblesafari.entity.csboss;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.csboss.BossBattleManager;
import maxigregrze.cobblesafari.csboss.CsBossDefinition;
import maxigregrze.cobblesafari.init.ModEffects;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Custom boss entity. Borrows the model from a Cobblemon species (client render),
 * immune to all damage; its "health" is the countdown managed by the session.
 */
public class CsBossEntity extends Mob {

    private static final EntityDataAccessor<String> DATA_SPECIE =
            SynchedEntityData.defineId(CsBossEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(CsBossEntity.class, EntityDataSerializers.FLOAT);
    /** Incremented server-side on each attack to trigger the client attack animation. */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_SEQ =
            SynchedEntityData.defineId(CsBossEntity.class, EntityDataSerializers.INT);
    /** Entity animation phase: 0 = entrance, 1 = active, 2 = death. */
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(CsBossEntity.class, EntityDataSerializers.INT);
    /** Progress 0..1 for the current phase (entrance ⇒ scale; death ⇒ fade). */
    private static final EntityDataAccessor<Float> DATA_ANIM =
            SynchedEntityData.defineId(CsBossEntity.class, EntityDataSerializers.FLOAT);

    public static final int PHASE_ENTERING = 0;
    public static final int PHASE_ACTIVE = 1;
    public static final int PHASE_DYING = 2;

    /** Fall height (blocks) during entrance — also the portal / death-rise height. */
    public static final double ENTRANCE_HEIGHT = 12.0;
    /** Y offset of the target position: 0 ⇒ the boss is "inside" the trigger block, not standing on top. */
    public static final double STAND_Y_OFFSET = 0.0;
    /** Minimum scale reached at the end of the departure (death) animation. Shared with the renderer and the hitbox. */
    public static final float DEATH_MIN_SCALE = 0.05f;

    private static final String KEY_SPECIE = "Specie";
    private static final String KEY_SIZE = "Size";
    private static final String KEY_SESSION = "SessionId";
    private static final String KEY_STATIC = "IsStatic";

    private int sessionId;
    private boolean staticBoss = true;
    /**
     * Base hitbox = species hitbox × baseScale × size, <i>before</i> the entrance/death animation
     * scale (applied in {@link #getDefaultDimensions}). Null ⇒ default type dimensions.
     */
    @Nullable
    private EntityDimensions baseDims;

    public CsBossEntity(EntityType<? extends CsBossEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.04D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D);
    }

    /** Spawns a boss at the center, above the trigger. */
    public static CsBossEntity spawnAbove(ServerLevel level, BlockPos triggerPos, CsBossDefinition def, int sessionId) {
        CsBossEntity boss = new CsBossEntity(ModEntities.CSBOSS, level);
        boss.setSpecie(def.specie());
        boss.setSize((float) def.size());
        boss.staticBoss = def.isStatic();
        boss.sessionId = sessionId;
        boss.setPhase(PHASE_ENTERING);
        boss.setAnim(0.0F);
        // Entrance: spawns ENTRANCE_HEIGHT blocks above the target position, at scale 0.
        double standY = triggerPos.getY() + STAND_Y_OFFSET;
        boss.moveTo(triggerPos.getX() + 0.5, standY + def.portalDistance(), triggerPos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(boss);
        return boss;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPECIE, "");
        builder.define(DATA_SIZE, (float) CsBossDefinition.DEFAULT_SIZE);
        builder.define(DATA_ATTACK_SEQ, 0);
        builder.define(DATA_PHASE, PHASE_ACTIVE);
        builder.define(DATA_ANIM, 1.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SPECIE.equals(key) || DATA_SIZE.equals(key)) {
            recomputeDimensions();
        } else if (DATA_PHASE.equals(key) || DATA_ANIM.equals(key)) {
            // Entrance/death animation changes the visible scale: keep the hitbox in step with it.
            refreshDimensions();
        }
    }

    /** Recomputes the hitbox from the Cobblemon species (form hitbox × baseScale × effectiveScale × size). */
    private void recomputeDimensions() {
        String specie = getSpecie();
        if (specie == null || specie.isBlank()) {
            return;
        }
        try {
            Pokemon mon = PokemonProperties.Companion.parse(specie).create(null);
            EntityDimensions box = mon.getForm().getHitbox();
            float scale = mon.getForm().getBaseScale() * getSize();
            this.baseDims = box.scale(scale);
            refreshDimensions();
        } catch (Exception ignored) {
            // unresolved species: keep previous / default dimensions
        }
    }

    /**
     * Scale multiplier for the current phase, shared by the renderer and the hitbox so the collision
     * box always tracks the visible model: grows 0→1 on entrance, shrinks to {@link #DEATH_MIN_SCALE}
     * on death, and is 1 while active.
     */
    public float animScale() {
        return switch (getPhase()) {
            case PHASE_ENTERING -> getAnim();
            case PHASE_DYING -> Mth.lerp(getAnim(), 1.0f, DEATH_MIN_SCALE);
            default -> 1.0f;
        };
    }

    @Override
    protected EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        if (this.baseDims == null) {
            return super.getDefaultDimensions(pose);
        }
        // Match the visible model as it grows in / shrinks out; floor at DEATH_MIN_SCALE so the box
        // never collapses to a degenerate zero-size AABB at the very first entrance tick.
        return this.baseDims.scale(Math.max(animScale(), DEATH_MIN_SCALE));
    }

    @Override
    protected void registerGoals() {
        // No vanilla AI: movement is driven by BossBattleManager.
    }

    public String getSpecie() {
        return this.entityData.get(DATA_SPECIE);
    }

    public void setSpecie(String specie) {
        this.entityData.set(DATA_SPECIE, specie == null ? "" : specie);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setSize(float size) {
        this.entityData.set(DATA_SIZE, Math.max(1.0f, size));
    }

    public int getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isStaticBoss() {
        return this.staticBoss;
    }

    public void setStaticBoss(boolean staticBoss) {
        this.staticBoss = staticBoss;
    }

    public int getAttackSeq() {
        return this.entityData.get(DATA_ATTACK_SEQ);
    }

    /** Server: signals the client to play an attack animation. */
    public void triggerAttackAnimation() {
        this.entityData.set(DATA_ATTACK_SEQ, getAttackSeq() + 1);
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
    }

    /** Progress 0..1 for the current phase (entrance scale / death fade). */
    public float getAnim() {
        return this.entityData.get(DATA_ANIM);
    }

    public void setAnim(float anim) {
        this.entityData.set(DATA_ANIM, Mth.clamp(anim, 0.0F, 1.0F));
    }

    /** Rotates the boss toward a target (body/head yaw), for rendering and animations. */
    public void faceTarget(@Nullable Vec3 targetPos) {
        if (targetPos == null) {
            return;
        }
        double dx = targetPos.x - this.getX();
        double dz = targetPos.z - this.getZ();
        if (Math.abs(dx) < 1.0E-4 && Math.abs(dz) < 1.0E-4) {
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
    }

    /**
     * Follows the nearest participant at very low speed, never leaving the arena radius.
     */
    public void driveTowards(@Nullable Vec3 targetPos, Vec3 arenaCenter, int radius) {
        if (this.staticBoss || targetPos == null) {
            this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
            return;
        }
        Vec3 to = targetPos.subtract(this.position());
        double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
        Vec3 step = horiz < 0.1 ? Vec3.ZERO
                : to.normalize().scale(this.getAttributeValue(Attributes.MOVEMENT_SPEED));
        Vec3 next = this.position().add(step.x, 0, step.z);
        double dx = next.x - arenaCenter.x;
        double dz = next.z - arenaCenter.z;
        if (Math.sqrt(dx * dx + dz * dz) > radius) {
            step = Vec3.ZERO;
        }
        this.setDeltaMovement(step.x, this.getDeltaMovement().y, step.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide
                && this.sessionId != 0
                && BossBattleManager.getSession(this.sessionId) == null) {
            this.discard();
        }
    }

    // --- Immunity / total inertia ------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        // The boss cannot be shackled (red_shackled).
        if (effect.getEffect().equals(ModEffects.RED_SHACKLED.holder)) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // no-op: no collision pushback
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // no-op
    }

    @Override
    protected boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(KEY_SPECIE, getSpecie());
        tag.putFloat(KEY_SIZE, getSize());
        tag.putInt(KEY_SESSION, this.sessionId);
        tag.putBoolean(KEY_STATIC, this.staticBoss);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpecie(tag.getString(KEY_SPECIE));
        setSize(tag.contains(KEY_SIZE) ? tag.getFloat(KEY_SIZE) : CsBossDefinition.DEFAULT_SIZE);
        this.sessionId = tag.getInt(KEY_SESSION);
        this.staticBoss = !tag.contains(KEY_STATIC) || tag.getBoolean(KEY_STATIC);
    }
}
