package maxigregrze.cobblesafari.safari.hazard;

import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.entity.safari.SafariBallisticMeteorEntity;
import maxigregrze.cobblesafari.entity.safari.SafariShadowHazardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Safari biome hazard helpers: reuses boss attack visuals/effects outside
 * boss sessions; damages any player in the hazard area.
 */
public final class SafariHazardLib {

    private SafariHazardLib() {}

    public static void startFireShadow(ServerLevel level, Vec3 spawn, ServerPlayer target) {
        SafariShadowHazardEntity.spawnFire(level, spawn);
    }

    public static void startWaterShadow(ServerLevel level, Vec3 spawn, ServerPlayer target) {
        SafariShadowHazardEntity.spawnWater(level, spawn);
    }

    public static void launchDracoMeteor(ServerLevel level, Vec3 spawn, RandomSource rng) {
        SafariBallisticMeteorEntity.spawn(level, spawn, rng);
    }

    public static void damagePlayersInColumn(ServerLevel level, double cx, double baseY, double cz,
                                             double radius, double height, float damage, int fireTicks) {
        damagePlayersInColumn(level, cx, baseY, cz, radius, height,
                level.damageSources().inFire(), damage, fireTicks);
    }

    public static void damagePlayersInColumn(ServerLevel level, double cx, double baseY, double cz,
                                             double radius, double height, DamageSource source,
                                             float damage, int fireTicks) {
        double r2 = radius * radius;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !p.isAlive()) {
                continue;
            }
            double dx = p.getX() - cx;
            double dz = p.getZ() - cz;
            if (dx * dx + dz * dz > r2) {
                continue;
            }
            if (p.getY() + p.getBbHeight() < baseY || p.getY() > baseY + height) {
                continue;
            }
            p.hurt(source, damage);
            if (fireTicks > 0) {
                p.setRemainingFireTicks(Math.max(p.getRemainingFireTicks(), fireTicks));
            }
        }
    }

    public static boolean meteorSweepHit(ServerLevel level, Entity meteor,
                                         double fromY, double toY, float damage) {
        AABB box = meteor.getBoundingBox();
        AABB swept = box.minmax(box.move(0.0, fromY - toY, 0.0));
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !p.isAlive()) {
                continue;
            }
            if (p.getBoundingBox().intersects(swept)) {
                p.hurt(CsBossDamage.bullet(level), damage);
                return true;
            }
        }
        if (meteor instanceof SafariBallisticMeteorEntity ballistic && ballistic.hasTargetEntity()) {
            Entity target = level.getEntity(ballistic.getTargetEntityId());
            if (target instanceof LivingEntity living && living.isAlive()
                    && living.getBoundingBox().intersects(swept)) {
                living.hurt(CsBossDamage.bullet(level), damage);
                return true;
            }
        }
        return false;
    }
}
