package maxigregrze.cobblesafari.entity.csboss;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.csboss.CsBossDefinition;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.BlockPos;
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
 * Entité Boss custom (plan 100 § 5). Modèle emprunté à une espèce Cobblemon (rendu client),
 * immunisée à tous les dégâts ; sa « vie » est le compte à rebours géré par la session.
 */
public class CsBossEntity extends Mob {

    private static final EntityDataAccessor<String> DATA_SPECIE =
            SynchedEntityData.defineId(CsBossEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SIZE =
            SynchedEntityData.defineId(CsBossEntity.class, EntityDataSerializers.INT);
    /** Incrémenté côté serveur à chaque attaque pour déclencher l'animation d'attaque côté client. */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_SEQ =
            SynchedEntityData.defineId(CsBossEntity.class, EntityDataSerializers.INT);
    /** Phase d'animation d'entité : 0 = entrée, 1 = actif, 2 = mort. */
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(CsBossEntity.class, EntityDataSerializers.INT);
    /** Progression 0..1 de la phase courante (entrée ⇒ échelle ; mort ⇒ fondu). */
    private static final EntityDataAccessor<Float> DATA_ANIM =
            SynchedEntityData.defineId(CsBossEntity.class, EntityDataSerializers.FLOAT);

    public static final int PHASE_ENTERING = 0;
    public static final int PHASE_ACTIVE = 1;
    public static final int PHASE_DYING = 2;

    /** Hauteur (blocs) de chute pendant l'entrée. */
    public static final double ENTRANCE_HEIGHT = 5.0;
    /** Décalage Y de la position cible : 0 ⇒ le boss est « dans » le bloc trigger, pas posé dessus. */
    public static final double STAND_Y_OFFSET = 0.0;

    private static final String KEY_SPECIE = "Specie";
    private static final String KEY_SIZE = "Size";
    private static final String KEY_SESSION = "SessionId";
    private static final String KEY_STATIC = "IsStatic";

    private int sessionId;
    private boolean staticBoss = true;
    /** Hitbox dérivée de l'espèce (null ⇒ dimensions par défaut du type). */
    @Nullable
    private EntityDimensions cachedDims;

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

    /** Fait apparaître un boss au centre, au-dessus du trigger. */
    public static CsBossEntity spawnAbove(ServerLevel level, BlockPos triggerPos, CsBossDefinition def, int sessionId) {
        CsBossEntity boss = new CsBossEntity(ModEntities.CSBOSS, level);
        boss.setSpecie(def.specie());
        boss.setSize(def.size());
        boss.staticBoss = def.isStatic();
        boss.sessionId = sessionId;
        boss.setPhase(PHASE_ENTERING);
        boss.setAnim(0.0F);
        // Entrée : apparaît ENTRANCE_HEIGHT blocs au-dessus de la position cible, à l'échelle 0.
        double standY = triggerPos.getY() + STAND_Y_OFFSET;
        boss.moveTo(triggerPos.getX() + 0.5, standY + ENTRANCE_HEIGHT, triggerPos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(boss);
        return boss;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPECIE, "");
        builder.define(DATA_SIZE, CsBossDefinition.DEFAULT_SIZE);
        builder.define(DATA_ATTACK_SEQ, 0);
        builder.define(DATA_PHASE, PHASE_ACTIVE);
        builder.define(DATA_ANIM, 1.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SPECIE.equals(key) || DATA_SIZE.equals(key)) {
            recomputeDimensions();
        }
    }

    /** Recalcule la hitbox depuis l'espèce Cobblemon (hitbox du form × baseScale × effectiveScale × size). */
    private void recomputeDimensions() {
        String specie = getSpecie();
        if (specie == null || specie.isBlank()) {
            return;
        }
        try {
            Pokemon mon = PokemonProperties.Companion.parse(specie).create(null);
            EntityDimensions box = mon.getForm().getHitbox();
            float scale = mon.getForm().getBaseScale() * getSize();
            this.cachedDims = box.scale(scale);
            refreshDimensions();
        } catch (Exception ignored) {
            // espèce non résolue : on garde les dimensions précédentes / par défaut
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        return this.cachedDims != null ? this.cachedDims : super.getDefaultDimensions(pose);
    }

    @Override
    protected void registerGoals() {
        // Aucune AI vanilla : le mouvement est piloté par BossBattleManager.
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

    public boolean isStaticBoss() {
        return this.staticBoss;
    }

    public void setStaticBoss(boolean staticBoss) {
        this.staticBoss = staticBoss;
    }

    public int getAttackSeq() {
        return this.entityData.get(DATA_ATTACK_SEQ);
    }

    /** Serveur : signale au client de jouer une animation d'attaque. */
    public void triggerAttackAnimation() {
        this.entityData.set(DATA_ATTACK_SEQ, getAttackSeq() + 1);
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
    }

    /** Progression 0..1 de la phase courante (échelle d'entrée / fondu de mort). */
    public float getAnim() {
        return this.entityData.get(DATA_ANIM);
    }

    public void setAnim(float anim) {
        this.entityData.set(DATA_ANIM, Mth.clamp(anim, 0.0F, 1.0F));
    }

    /** Oriente le boss vers une cible (yaw du corps/tête), pour le rendu et les animations. */
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
     * Suit le participant le plus proche à très faible vitesse, sans jamais sortir du rayon d'arène.
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

    // --- Immunité / inertie totale ------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // no-op : pas de collision pushback
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
        tag.putInt(KEY_SIZE, getSize());
        tag.putInt(KEY_SESSION, this.sessionId);
        tag.putBoolean(KEY_STATIC, this.staticBoss);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpecie(tag.getString(KEY_SPECIE));
        setSize(tag.contains(KEY_SIZE) ? tag.getInt(KEY_SIZE) : CsBossDefinition.DEFAULT_SIZE);
        this.sessionId = tag.getInt(KEY_SESSION);
        this.staticBoss = !tag.contains(KEY_STATIC) || tag.getBoolean(KEY_STATIC);
    }
}
