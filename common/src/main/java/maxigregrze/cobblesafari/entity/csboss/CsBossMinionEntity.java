package maxigregrze.cobblesafari.entity.csboss;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.csboss.CsBossDefinition;
import maxigregrze.cobblesafari.init.ModEffects;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
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
 * Boss minion: lightweight entity that <b>borrows the model of a Cobblemon species</b> (like
 * {@link CsBossEntity}), meant to be driven by an attack pattern (plan 104).
 * No vanilla AI, no gravity, immune — movement and removal are decided by
 * attack code. Hitbox derived from species × size.
 */
public class CsBossMinionEntity extends Mob {

    private static final EntityDataAccessor<String> DATA_SPECIE =
            SynchedEntityData.defineId(CsBossMinionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SIZE =
            SynchedEntityData.defineId(CsBossMinionEntity.class, EntityDataSerializers.INT);
    /** Incremented server-side to trigger a client attack animation. */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_SEQ =
            SynchedEntityData.defineId(CsBossMinionEntity.class, EntityDataSerializers.INT);
    /** White flash (overlay) driven by an attack pattern (plan 107 § 5.3). */
    private static final EntityDataAccessor<Boolean> DATA_FLASH =
            SynchedEntityData.defineId(CsBossMinionEntity.class, EntityDataSerializers.BOOLEAN);
    /** Render opacity (fade of {@code base_ghost_1}, plan 108). */
    private static final EntityDataAccessor<Float> DATA_ALPHA =
            SynchedEntityData.defineId(CsBossMinionEntity.class, EntityDataSerializers.FLOAT);
    /** Floating scale (≤ 0 ⇒ uses integer {@code size}). Enables precise resizing (plan 111). */
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(CsBossMinionEntity.class, EntityDataSerializers.FLOAT);

    private static final String KEY_SPECIE = "Specie";
    private static final String KEY_SIZE = "Size";
    private static final String KEY_SESSION = "SessionId";

    private int sessionId;
    @Nullable
    private EntityDimensions cachedDims;

    public CsBossMinionEntity(EntityType<? extends CsBossMinionEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D);
    }

    /** Spawns a minion at the given position. */
    public static CsBossMinionEntity spawn(ServerLevel level, double x, double y, double z,
                                           String specie, int size, int sessionId) {
        CsBossMinionEntity minion = new CsBossMinionEntity(ModEntities.CSBOSS_MINION, level);
        minion.setSpecie(specie);
        minion.setSize(size);
        minion.sessionId = sessionId;
        minion.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(minion);
        return minion;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPECIE, "");
        builder.define(DATA_SIZE, CsBossDefinition.DEFAULT_SIZE);
        builder.define(DATA_ATTACK_SEQ, 0);
        builder.define(DATA_FLASH, false);
        builder.define(DATA_ALPHA, 1.0F);
        builder.define(DATA_SCALE, 0.0F);
    }

    /** Effective scale: {@code DATA_SCALE} if &gt; 0, otherwise the integer {@code size}. */
    public float getRenderScale() {
        float fs = this.entityData.get(DATA_SCALE);
        return fs > 0.0F ? fs : getSize();
    }

    /**
     * Resizes the minion so its hitbox height targets {@code targetBlocks} blocks, accounting for
     * the species' natural hitbox × baseScale (plan 111, {@code base_ghost_1}).
     */
    public void resizeToHeight(double targetBlocks) {
        String specie = getSpecie();
        if (specie == null || specie.isBlank()) {
            return;
        }
        try {
            Pokemon mon = PokemonProperties.Companion.parse(specie).create(null);
            EntityDimensions box = mon.getForm().getHitbox();
            float natural = box.height() * mon.getForm().getBaseScale();
            if (natural > 0.001F) {
                this.entityData.set(DATA_SCALE, (float) (targetBlocks / natural));
            }
        } catch (Exception ignored) {
            // unresolved species: keep current scale
        }
    }

    public boolean isFlashing() {
        return this.entityData.get(DATA_FLASH);
    }

    public void setFlashing(boolean flashing) {
        this.entityData.set(DATA_FLASH, flashing);
    }

    public float getAlpha() {
        return this.entityData.get(DATA_ALPHA);
    }

    public void setAlpha(float alpha) {
        this.entityData.set(DATA_ALPHA, alpha);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SPECIE.equals(key) || DATA_SIZE.equals(key) || DATA_SCALE.equals(key)) {
            recomputeDimensions();
        }
    }

    private void recomputeDimensions() {
        String specie = getSpecie();
        if (specie == null || specie.isBlank()) {
            return;
        }
        try {
            Pokemon mon = PokemonProperties.Companion.parse(specie).create(null);
            EntityDimensions box = mon.getForm().getHitbox();
            float scale = mon.getForm().getBaseScale() * getRenderScale();
            this.cachedDims = box.scale(scale);
            refreshDimensions();
        } catch (Exception ignored) {
            // unresolved species: keep previous / default dimensions
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        return this.cachedDims != null ? this.cachedDims : super.getDefaultDimensions(pose);
    }

    @Override
    protected void registerGoals() {
        // No vanilla AI: movement is driven by the attack pattern.
    }

    public String getSpecie() {
        return this.entityData.get(DATA_SPECIE);
    }

    public void setSpecie(String specie) {
        this.entityData.set(DATA_SPECIE, specie == null ? "" : specie);
    }

    public int getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setSize(int size) {
        this.entityData.set(DATA_SIZE, Math.max(1, size));
    }

    public int getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getAttackSeq() {
        return this.entityData.get(DATA_ATTACK_SEQ);
    }

    public void triggerAttackAnimation() {
        this.entityData.set(DATA_ATTACK_SEQ, getAttackSeq() + 1);
    }

    /** Rotates the minion toward a target (body/head yaw), for rendering. */
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
     * Like {@link #faceTarget(Vec3)} but also snaps the previous-frame rotations so the minion is
     * already oriented on its very first rendered frame (no visible turn after spawning).
     */
    public void faceTargetInstant(@Nullable Vec3 targetPos) {
        faceTarget(targetPos);
        this.yRotO = this.getYRot();
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
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
    public boolean canBeAffected(MobEffectInstance effect) {
        // Les minions ne peuvent pas être enchaînés (red_shackled, plan 122 § 6).
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
        // no-op
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(KEY_SPECIE, getSpecie());
        tag.putInt(KEY_SIZE, getSize());
        tag.putInt(KEY_SESSION, this.sessionId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpecie(tag.getString(KEY_SPECIE));
        setSize(tag.contains(KEY_SIZE) ? tag.getInt(KEY_SIZE) : CsBossDefinition.DEFAULT_SIZE);
        this.sessionId = tag.getInt(KEY_SESSION);
    }
}
