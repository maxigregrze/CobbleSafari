package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Anneau de choc propagatif au sol (plan 124) : rayon croissant, bande radiale de ½ bloc,
 * collision limitée aux participants (esquive par saut).
 *
 * <p>Util générique : l'appelant fournit directement la couleur, les dégâts et les effets
 * (poison / poussée) — aucun « type » figé, n'importe qui peut créer une variante.
 */
public class AttackShockwaveEntity extends AbstractAttackEntity {

    private static final String KEY_COLOR = "Color";

    public static final double WAVE_SPEED = 0.5;
    public static final double THICKNESS = 0.5;
    /** Écart vertical max entre le joueur et le sol de l'onde pour être touché (exclut les blocs surélevés). */
    public static final double GROUND_Y_TOLERANCE = 0.5;
    /** Force de poussée appliquée quand {@code applyKnockback} est vrai. */
    public static final double KNOCKBACK_STRENGTH = 1.0;

    private static final int POISON_DURATION = 100; // 5 s
    private static final int POISON_AMPLIFIER = 1;
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    /** Couleur synchronisée (rendu côté client). */
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(AttackShockwaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(AttackShockwaveEntity.class, EntityDataSerializers.FLOAT);

    // Paramètres de collision (serveur uniquement, fixés au spawn).
    private float damage;
    private boolean applyPoison;
    private boolean applyKnockback;

    private float radius;
    private float prevRadius;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public AttackShockwaveEntity(EntityType<? extends AttackShockwaveEntity> type, Level level) {
        super(type, level);
    }

    /**
     * Crée une onde de choc. Les variantes (eau / poison / acier / normal / personnalisée) sont
     * définies par les paramètres transmis ici, pas par un enum.
     *
     * @param colorRgb       couleur de l'anneau (rendu)
     * @param damage         dégâts au contact
     * @param applyPoison    applique l'effet Poison au contact
     * @param applyKnockback applique une poussée radiale vers l'extérieur au contact
     */
    public static AttackShockwaveEntity spawn(ServerLevel level, double x, double floorY, double z,
                                              int sessionId, int colorRgb, float damage,
                                              boolean applyPoison, boolean applyKnockback) {
        AttackShockwaveEntity e = new AttackShockwaveEntity(ModEntities.ATTACK_SHOCKWAVE, level);
        e.setSessionId(sessionId);
        e.setColorRgb(colorRgb);
        e.damage = damage;
        e.applyPoison = applyPoison;
        e.applyKnockback = applyKnockback;
        e.radius = 0.0F;
        e.prevRadius = 0.0F;
        e.moveTo(x, floorY, z, 0.0F, 0.0F);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, DEFAULT_COLOR);
        builder.define(DATA_RADIUS, 0.0F);
    }

    public int getColorRgb() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColorRgb(int colorRgb) {
        this.entityData.set(DATA_COLOR, colorRgb);
    }

    public float getRadius() {
        return this.radius;
    }

    public float getPrevRadius() {
        return this.prevRadius;
    }

    public float getRadius(float partialTicks) {
        return this.prevRadius + (this.radius - this.prevRadius) * partialTicks;
    }

    @Override
    public void tick() {
        this.prevRadius = this.radius;
        if (this.level().isClientSide) {
            this.radius = this.entityData.get(DATA_RADIUS);
        }
        super.tick();
    }

    @Override
    protected void serverTick(ServerLevel level) {
        this.radius += (float) WAVE_SPEED;
        this.entityData.set(DATA_RADIUS, this.radius);

        BossBattleSession session = session();
        if (session == null) {
            return;
        }

        if (this.radius >= session.getPlayerRadius()) {
            this.discard();
            return;
        }

        double cx = getX();
        double cz = getZ();
        double inner = this.radius - THICKNESS;

        for (ServerPlayer p : session.aliveParticipants(level)) {
            // Touché seulement si le joueur est au sol ET au même niveau que l'onde (pas sur un bloc surélevé).
            if (alreadyHit.contains(p.getUUID()) || !p.onGround()
                    || Math.abs(p.getY() - getY()) > GROUND_Y_TOLERANCE) {
                continue;
            }
            double dx = p.getX() - cx;
            double dz = p.getZ() - cz;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < inner || d > this.radius) {
                continue;
            }
            alreadyHit.add(p.getUUID());
            p.hurt(CsBossDamage.bullet(level), this.damage);
            if (this.applyKnockback) {
                p.knockback(KNOCKBACK_STRENGTH, cx - p.getX(), cz - p.getZ());
            }
            if (this.applyPoison) {
                p.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION, POISON_AMPLIFIER));
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(this.radius);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(KEY_COLOR)) {
            setColorRgb(tag.getInt(KEY_COLOR));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(KEY_COLOR, getColorRgb());
    }

    @Override
    protected int maxLifespan() {
        return 80;
    }
}
