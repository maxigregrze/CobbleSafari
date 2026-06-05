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
 * Librairie partagée des patterns d'attaque CSBoss (plan 107 § 3). Regroupe les helpers communs
 * (directions, ciblage, poursuite, dégâts de zone limités aux participants, particules) afin
 * d'éviter le copier‑coller entre les attaques. Toute la détection de collision reste limitée au
 * set des participants vivants — jamais de scan monde.
 */
public final class CsBossAttackLib {

    /** Vitesse de poursuite des entités d'attaque (> sprint joueur ≈ 0.28 b/t : injoignable à la course). */
    public static final double CHASE_SPEED = 0.45;

    /** 8 directions de boussole (unitaires), ordre N, NE, E, SE, S, SO, O, NO. */
    public static final Vec3[] EIGHT_DIRS = buildEightDirs();

    /** Taux de variation du nombre d'occurrences d'une attaque (plan 109). */
    public static final double OCCURRENCE_VARIATION = 0.25;

    private CsBossAttackLib() {}

    /**
     * Nombre d'occurrences randomisé autour de {@code nominal} avec ±25 % de variation (borné ≥ 1).
     * Ex. {@code nominal = 4 → [3, 5]}.
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

    // --- Ciblage -------------------------------------------------------------

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

    // --- Poursuite -----------------------------------------------------------

    /**
     * Déplace {@code e} vers ({@code tx},{@code tz}) en bornant le pas horizontal à {@code maxStep},
     * et fixe son Y à {@code ty} (suit le sol du joueur).
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

    // --- Dégâts de zone (participants uniquement) ----------------------------

    /**
     * Inflige des dégâts + feu aux participants vivants dans un cylindre vertical centré sur
     * ({@code cx},{@code cz}) au sol {@code baseY}. Équivalent « bloc de feu ».
     */
    public static void damagePlayersInColumn(ServerLevel level, BossBattleSession session,
                                             double cx, double baseY, double cz,
                                             double radius, double height, float damage, int fireTicks) {
        damagePlayersInColumn(level, session, cx, baseY, cz, radius, height,
                level.damageSources().inFire(), damage, fireTicks);
    }

    /**
     * Variante avec source de dégâts explicite et feu optionnel ({@code fireTicks = 0} ⇒ pas
     * d'embrasement). Utilisée par {@code base_water_1} (dégâts sans feu).
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
     * Explosion sans dégât de bloc : particules + son, et dégâts (dégressifs avec la distance)
     * aux participants vivants dans le rayon. Aucun bloc n'est cassé.
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

    // --- Particules ----------------------------------------------------------

    /** Colonne de particules montantes générique. */
    public static void particleColumn(ServerLevel level, double x, double baseY, double z, double height,
                                      net.minecraft.core.particles.ParticleOptions particle) {
        for (double dy = 0; dy < height; dy += 0.5) {
            level.sendParticles(particle, x, baseY + dy, z, 4, 0.25, 0.1, 0.25, 0.08);
        }
    }

    /** Émet une bouffée de flammes montantes (style colonne {@code bubble_column_up}). */
    public static void flameColumn(ServerLevel level, double x, double baseY, double z, double height) {
        particleColumn(level, x, baseY, z, height, ParticleTypes.FLAME);
        level.sendParticles(ParticleTypes.LAVA, x, baseY + 0.2, z, 1, 0.2, 0.0, 0.2, 0.0);
    }

    /** Émet une colonne de bulles montantes ({@code base_water_1}). */
    public static void bubbleColumn(ServerLevel level, double x, double baseY, double z, double height) {
        particleColumn(level, x, baseY, z, height, ParticleTypes.BUBBLE_COLUMN_UP);
        level.sendParticles(ParticleTypes.SPLASH, x, baseY + 0.2, z, 6, 0.25, 0.1, 0.25, 0.1);
    }

    /** Bouffée de particules blanches autour d'une position (flash de télégraphe). */
    public static void whiteFlash(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 12, 0.4, 0.4, 0.4, 0.02);
    }

    /** Poussière météorite (#ad857d) tombant du ciel. */
    public static final net.minecraft.core.particles.DustParticleOptions METEOR_DUST =
            new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.678f, 0.522f, 0.490f), 1.5f);
    /** Poussière dracométéore (#c3476b). */
    public static final net.minecraft.core.particles.DustParticleOptions DRACO_DUST =
            new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.765f, 0.278f, 0.420f), 1.5f);

    /**
     * Télégraphe météorite : un peu de poussière colorée tombant du ciel, 2 blocs au‑dessus de
     * l'ombre (rend l'attaque visible même en vue première personne). À émettre chaque tick.
     */
    public static void meteorTelegraph(ServerLevel level, double x, double y, double z,
                                       net.minecraft.core.particles.DustParticleOptions dust) {
        // Étalé sur ~1 bloc en horizontal (count=0 ⇒ vitesse vers le bas) pour tomber dans le champ de vision.
        double ox = level.getRandom().nextDouble() - 0.5;
        double oz = level.getRandom().nextDouble() - 0.5;
        level.sendParticles(dust, x + ox, y + 2.0, z + oz, 0, 0.0, -1.0, 0.0, 0.25);
    }

    /**
     * Petite quantité de particules montantes depuis l'ombre, réparties jusqu'à ~2 blocs de haut
     * (télégraphe colonne feu/eau).
     */
    public static void risingTelegraph(ServerLevel level, double x, double y, double z,
                                       net.minecraft.core.particles.ParticleOptions particle) {
        double h = level.getRandom().nextDouble() * 2.0;
        level.sendParticles(particle, x, y + 0.2 + h, z, 1, 0.1, 0.05, 0.1, 0.01);
    }

    /**
     * Dégât « balayé » d'une météorite tombante : couvre tout le segment vertical parcouru ce tick
     * ({@code fromY} → {@code toY}), pas seulement la position d'arrivée — sinon un pas de chute plus
     * grand que la hitbox saute par‑dessus le joueur (cause des ratés). Inflige une fois et renvoie
     * {@code true} si un participant a été touché.
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

    // --- Effets de statut ----------------------------------------------------

    /**
     * Applique un effet à tous les participants vivants (plan 113). À rappeler chaque tick avec une
     * petite durée pour qu'il « dure le temps de l'attaque », ou une fois avec la durée voulue.
     */
    public static void applyEffectToAll(ServerLevel level, BossBattleSession session,
                                        net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                        int durationTicks, int amplifier) {
        for (ServerPlayer p : session.aliveParticipants(level)) {
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, durationTicks, amplifier));
        }
    }

    // --- Sons ----------------------------------------------------------------

    /**
     * Joue un son par identifiant ({@code "namespace:path"}, vanilla ou Cobblemon) à une position.
     * Utilise un {@code SoundEvent} direct : le client résout le son par sa location même s'il n'est
     * pas dans le registre côté serveur.
     */
    public static void sound(ServerLevel level, double x, double y, double z,
                             String id, SoundSource source, float volume, float pitch) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(ResourceLocation.parse(id));
        level.playSound(null, x, y, z, event, source, volume, pitch);
    }
}
