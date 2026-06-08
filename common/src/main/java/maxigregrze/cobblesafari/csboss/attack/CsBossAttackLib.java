package maxigregrze.cobblesafari.csboss.attack;

import maxigregrze.cobblesafari.csboss.BossBattleSession;
import maxigregrze.cobblesafari.csboss.CsBossDamage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shared library for CSBoss attack patterns (plan 107 § 3). Groups common helpers
 * (directions, targeting, pursuit, area damage limited to participants, particles) to
 * avoid copy-paste between attacks. All collision detection remains limited to the
 * set of living participants — never a world scan.
 */
public final class CsBossAttackLib {

    /** Chase speed for attack entities (> player sprint ≈ 0.28 b/t : uncatchable while running). */
    public static final double CHASE_SPEED = 0.45;

    /** 8 compass directions (unit vectors), order N, NE, E, SE, S, SW, W, NW. */
    public static final Vec3[] EIGHT_DIRS = buildEightDirs();

    /** Variation rate for attack occurrence counts (plan 109). */
    public static final double OCCURRENCE_VARIATION = 0.25;

    private CsBossAttackLib() {}

    /**
     * Randomized occurrence count around {@code nominal} with ±25% variation (bounded ≥ 1).
     * E.g. {@code nominal = 4 → [3, 5]}.
     */
    public static int varyOccurrences(int nominal, RandomSource rng) {
        int min = Math.max(1, (int) Math.round(nominal * (1.0 - OCCURRENCE_VARIATION)));
        int max = Math.max(min, (int) Math.round(nominal * (1.0 + OCCURRENCE_VARIATION)));
        return min + rng.nextInt(max - min + 1);
    }

    private static Vec3[] buildEightDirs() {
        Vec3[] dirs = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0);
            dirs[i] = new Vec3(Math.sin(a), 0, -Math.cos(a)); // i=0 → N (-Z)
        }
        return dirs;
    }

    // --- Targeting -------------------------------------------------------------

    @Nullable
    public static ServerPlayer nearestAlive(BossBattleSession session, ServerLevel level, Vec3 from) {
        ServerPlayer best = null;
        double bestSq = Double.MAX_VALUE;
        for (ServerPlayer p : session.aliveParticipants(level)) {
            double d = p.position().distanceToSqr(from);
            if (d < bestSq) {
                bestSq = d;
                best = p;
            }
        }
        return best;
    }

    @Nullable
    public static ServerPlayer randomAlive(BossBattleSession session, ServerLevel level, RandomSource rng) {
        List<ServerPlayer> alive = session.aliveParticipants(level);
        return alive.isEmpty() ? null : alive.get(rng.nextInt(alive.size()));
    }

    // --- Pursuit -----------------------------------------------------------

    /**
     * Moves {@code e} toward ({@code tx},{@code tz}) while bounding horizontal step to {@code maxStep},
     * and fixes its Y to {@code ty} (follows player ground level).
     */
    public static void chase(Entity e, double tx, double ty, double tz, double maxStep) {
        double dx = tx - e.getX();
        double dz = tz - e.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        double nx;
        double nz;
        if (dist <= maxStep || dist < 1.0E-6) {
            nx = tx;
            nz = tz;
        } else {
            nx = e.getX() + dx / dist * maxStep;
            nz = e.getZ() + dz / dist * maxStep;
        }
        e.setPos(nx, ty, nz);
    }

    // --- Area damage (participants only) ----------------------------

    /**
     * Deals damage + fire to living participants in a vertical cylinder centered on
     * ({@code cx},{@code cz}) at ground {@code baseY}. Equivalent to a "fire block".
     */
    public static void damagePlayersInColumn(ServerLevel level, BossBattleSession session,
                                             double cx, double baseY, double cz,
                                             double radius, double height, float damage, int fireTicks) {
        damagePlayersInColumn(level, session, cx, baseY, cz, radius, height,
                level.damageSources().inFire(), damage, fireTicks);
    }

    /**
     * Variant with explicit damage source and optional fire ({@code fireTicks = 0} ⇒ no
     * ignition). Used by {@code base_water_1} (damage without fire).
     */
    public static void damagePlayersInColumn(ServerLevel level, BossBattleSession session,
                                             double cx, double baseY, double cz,
                                             double radius, double height,
                                             net.minecraft.world.damagesource.DamageSource source,
                                             float damage, int fireTicks) {
        double r2 = radius * radius;
        for (ServerPlayer p : session.aliveParticipants(level)) {
            double dx = p.getX() - cx;
            double dz = p.getZ() - cz;
            if (dx * dx + dz * dz > r2) {
                continue;
            }
            if (p.getY() + p.getBbHeight() < baseY || p.getY() > baseY + height) {
                continue;
            }
            p.hurt(source, damage);
            if (fireTicks > 0 && p.getRemainingFireTicks() < fireTicks) {
                p.setRemainingFireTicks(fireTicks);
            }
        }
    }

    /**
     * Explosion without block damage: particles + sound, and damage (falling off with distance)
     * to living participants within radius. No blocks are broken.
     */
    public static void nonBlockExplosion(ServerLevel level, BossBattleSession session,
                                         Vec3 center, double radius, float maxDamage) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 1.0F);
        double r2 = radius * radius;
        for (ServerPlayer p : session.aliveParticipants(level)) {
            double d2 = p.position().distanceToSqr(center);
            if (d2 > r2) {
                continue;
            }
            float factor = (float) (1.0 - Math.sqrt(d2) / radius);
            float dmg = Math.max(1.0F, maxDamage * factor);
            p.hurt(CsBossDamage.bullet(level), dmg);
        }
    }

    // --- Particles ----------------------------------------------------------

    /** Generic rising particle column. */
    public static void particleColumn(ServerLevel level, double x, double baseY, double z, double height,
                                      net.minecraft.core.particles.ParticleOptions particle) {
        for (double dy = 0; dy < height; dy += 0.5) {
            level.sendParticles(particle, x, baseY + dy, z, 4, 0.25, 0.1, 0.25, 0.08);
        }
    }

    /** Emits a burst of rising flames (style {@code bubble_column_up} column). */
    public static void flameColumn(ServerLevel level, double x, double baseY, double z, double height) {
        particleColumn(level, x, baseY, z, height, ParticleTypes.FLAME);
        level.sendParticles(ParticleTypes.LAVA, x, baseY + 0.2, z, 1, 0.2, 0.0, 0.2, 0.0);
    }

    /** Emits a rising bubble column ({@code base_water_1}). */
    public static void bubbleColumn(ServerLevel level, double x, double baseY, double z, double height) {
        particleColumn(level, x, baseY, z, height, ParticleTypes.BUBBLE_COLUMN_UP);
        level.sendParticles(ParticleTypes.SPLASH, x, baseY + 0.2, z, 6, 0.25, 0.1, 0.25, 0.1);
    }

    /** Burst of white particles around a position (telegraph flash). */
    public static void whiteFlash(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 12, 0.4, 0.4, 0.4, 0.02);
    }

    /** Meteor dust (#ad857d) falling from the sky. */
    public static final net.minecraft.core.particles.DustParticleOptions METEOR_DUST =
            new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.678f, 0.522f, 0.490f), 1.5f);
    /** Dracometeor dust (#c3476b). */
    public static final net.minecraft.core.particles.DustParticleOptions DRACO_DUST =
            new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.765f, 0.278f, 0.420f), 1.5f);

    /**
     * Meteor telegraph: a little colored dust falling from the sky, 2 blocks above
     * the shadow (makes the attack visible even in first-person view). Emit each tick.
     */
    public static void meteorTelegraph(ServerLevel level, double x, double y, double z,
                                       net.minecraft.core.particles.DustParticleOptions dust) {
        // Spread over ~1 block horizontally (count=0 ⇒ downward velocity) to fall into the field of view.
        double ox = level.getRandom().nextDouble() - 0.5;
        double oz = level.getRandom().nextDouble() - 0.5;
        level.sendParticles(dust, x + ox, y + 2.0, z + oz, 0, 0.0, -1.0, 0.0, 0.25);
    }

    /**
     * Small amount of rising particles from the shadow, spread up to ~2 blocks high
     * (fire/water column telegraph).
     */
    public static void risingTelegraph(ServerLevel level, double x, double y, double z,
                                       net.minecraft.core.particles.ParticleOptions particle) {
        double h = level.getRandom().nextDouble() * 2.0;
        level.sendParticles(particle, x, y + 0.2 + h, z, 1, 0.1, 0.05, 0.1, 0.01);
    }

    /**
     * "Swept" damage from a falling meteorite: covers the entire vertical segment traveled this tick
     * ({@code fromY} → {@code toY}), not just the landing position — otherwise a fall step larger
     * than the hitbox skips over the player (causes misses). Hits once and returns
     * {@code true} if a participant was struck.
     */
    public static boolean meteorSweepHit(ServerLevel level, BossBattleSession session,
                                         net.minecraft.world.entity.Entity meteor,
                                         double fromY, double toY, float damage) {
        net.minecraft.world.phys.AABB box = meteor.getBoundingBox();
        net.minecraft.world.phys.AABB swept = box.minmax(box.move(0.0, fromY - toY, 0.0));
        for (ServerPlayer p : session.aliveParticipants(level)) {
            if (p.getBoundingBox().intersects(swept)) {
                p.hurt(CsBossDamage.bullet(level), damage);
                return true;
            }
        }
        return false;
    }

    // --- Status effects ----------------------------------------------------

    /**
     * Applies an effect to all living participants (plan 113). Re-apply each tick with a
     * short duration so it "lasts for the attack", or once with the desired duration.
     */
    public static void applyEffectToAll(ServerLevel level, BossBattleSession session,
                                        net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                        int durationTicks, int amplifier) {
        for (ServerPlayer p : session.aliveParticipants(level)) {
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, durationTicks, amplifier));
        }
    }

    // --- Sounds ----------------------------------------------------------------

    /**
     * Plays a sound by id ({@code "namespace:path"}, vanilla or Cobblemon) at a position.
     * Uses a direct {@code SoundEvent}: the client resolves the sound by location even if it is
     * not in the server-side registry.
     */
    public static void sound(ServerLevel level, double x, double y, double z,
                             String id, SoundSource source, float volume, float pitch) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(ResourceLocation.parse(id));
        level.playSound(null, x, y, z, event, source, volume, pitch);
    }
}
