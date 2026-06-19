package maxigregrze.cobblesafari.entity.csboss.attacks;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.csboss.attack.CsBossAttackLib;
import maxigregrze.cobblesafari.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Arcing cube projectile: parabolic flight, single-player collision,
 * places ephemeral blocks on impact.
 */
public class AttackPileProjectileEntity extends AbstractAttackEntity {

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(AttackPileProjectileEntity.class, EntityDataSerializers.INT);

    private Vec3 origin = Vec3.ZERO;
    private Vec3 target = Vec3.ZERO;
    private int flightTicks = 30;
    private double arcHeight = 6.0;
    private int extent = 1;
    private float damage = 8.0F;
    private int flightAge;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public AttackPileProjectileEntity(EntityType<? extends AttackPileProjectileEntity> type, Level level) {
        super(type, level);
    }

    public static AttackPileProjectileEntity spawn(ServerLevel level, Vec3 origin, Vec3 target, int sessionId,
                                                   PileKind kind, int flightTicks, double arcHeight,
                                                   int extent, float damage) {
        AttackPileProjectileEntity e = new AttackPileProjectileEntity(ModEntities.ATTACK_PILE_PROJECTILE, level);
        e.setSessionId(sessionId);
        e.setKind(kind);
        e.origin = origin;
        e.target = target;
        e.flightTicks = Math.max(1, flightTicks);
        e.arcHeight = arcHeight;
        e.extent = extent;
        e.damage = damage;
        e.flightAge = 0;
        e.alreadyHit.clear();
        e.setPos(origin.x, origin.y, origin.z);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, PileKind.SLUDGE.ordinal());
    }

    public PileKind getKind() {
        return PileKind.fromOrdinal(this.entityData.get(DATA_KIND));
    }

    public void setKind(PileKind kind) {
        this.entityData.set(DATA_KIND, kind.ordinal());
    }

    @Override
    protected int maxLifespan() {
        return 200;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        flightAge++;
        double f = Mth.clamp(flightAge / (double) flightTicks, 0.0, 1.0);
        double x = lerp(origin.x, target.x, f);
        double y = lerp(origin.y, target.y, f) + CsBossAttackLib.hopOffset(flightAge, flightTicks, arcHeight);
        double z = lerp(origin.z, target.z, f);
        this.setPos(x, y, z);

        BossBattleSession session = session();
        if (session != null) {
            for (ServerPlayer p : session.aliveParticipants(level)) {
                if (alreadyHit.contains(p.getUUID())) {
                    continue;
                }
                if (p.getBoundingBox().intersects(this.getBoundingBox())) {
                    alreadyHit.add(p.getUUID());
                    applyHitEffects(level, p);
                }
            }
        }

        if (flightAge >= flightTicks) {
            impact(level);
            this.discard();
        }
    }

    private void applyHitEffects(ServerLevel level, ServerPlayer player) {
        PileKind kind = getKind();
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        if (kind.appliesPoison()) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        }
        if (kind.appliesDamage()) {
            player.hurt(CsBossDamage.bullet(level), damage);
        }
    }

    private void impact(ServerLevel level) {
        PileKind kind = getKind();
        BlockPos center = BlockPos.containing(target);
        int half = extent / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (level.getBlockState(pos).canBeReplaced()) {
                    level.setBlockAndUpdate(pos, kind.ephemeralBlock().defaultBlockState());
                }
            }
        }
        CsBossAttackLib.sound(level, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                kind == PileKind.SLUDGE ? "cobblemon:impact.poison" : "cobblemon:impact.ground",
                SoundSource.HOSTILE, 1.2F, 1.0F);
    }

    @Override
    protected void clientTick() {
        PileKind kind = getKind();
        for (int i = 0; i < 3; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 0.4;
            double oz = (this.random.nextDouble() - 0.5) * 0.4;
            this.level().addParticle(kind.trailDust(),
                    this.getX() + ox, this.getY() + 0.25, this.getZ() + oz,
                    0.0, 0.02, 0.0);
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

}
