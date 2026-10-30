package maxigregrze.cobblesafari.entity.projectile;

import maxigregrze.cobblesafari.config.CsBossSettings;
import maxigregrze.cobblesafari.csboss.BossBattleManager;
import maxigregrze.cobblesafari.entity.csboss.CsBossEntity;
import maxigregrze.cobblesafari.entity.csboss.CsBossMinionEntity;
import maxigregrze.cobblesafari.entity.csboss.attacks.AttackBellEntity;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ThrownBalmEntity extends ThrowableItemProjectile {

    private static final int TRAIL_COLOR = 0xFBE57A;

    public ThrownBalmEntity(EntityType<? extends ThrownBalmEntity> type, Level level) {
        super(type, level);
    }

    public ThrownBalmEntity(Level level, LivingEntity thrower) {
        super(ModEntities.THROWN_BALM, thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BALM;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.2;
            double oy = (random.nextDouble() - 0.5) * 0.2;
            double oz = (random.nextDouble() - 0.5) * 0.2;
            level().addParticle(
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, TRAIL_COLOR),
                    getX() + ox, getY() + oy, getZ() + oz,
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide()) {
            return;
        }
        Entity target = result.getEntity();
        if (target instanceof CsBossEntity boss) {
            BossBattleManager.onBalmHit(boss); // coup direct : pas d'éclaboussure
        } else if (target instanceof AttackBellEntity bell) {
            bell.breakByBalm(); // coup direct : pas d'éclaboussure
        } else if (target instanceof CsBossMinionEntity minion && minion.isBalmProxy()) {
            BossBattleManager.onBalmProxyHit(minion); // relais du boss caché
        } else {
            splash(result.getLocation());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide()) {
            splash(result.getLocation());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            spawnImpactParticles();
            discard();
        }
    }

    /**
     * Éclaboussure (plan 199 § 9) : le baume « détone » au point d'impact et applique son effet à
     * <b>une seule</b> cible, la plus proche, parmi le boss et les cloches touchés par le rayon.
     */
    private void splash(Vec3 at) {
        double r = CsBossSettings.get().getBalmSplashRadius();
        if (r <= 0.0 || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.levelEvent(LevelEvent.PARTICLES_SPELL_POTION_SPLASH, BlockPos.containing(at), TRAIL_COLOR);
        // Boîte de recherche large : la hitbox du boss peut déborder bien au-delà du rayon.
        AABB search = new AABB(at, at).inflate(r + 16.0);
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : serverLevel.getEntities(this, search, e -> (e instanceof CsBossEntity b && !b.isVanished())
                || e instanceof AttackBellEntity
                || (e instanceof CsBossMinionEntity m && m.isBalmProxy()))) {
            double d = distanceToBox(at, e.getBoundingBox());
            if (d <= r && d < bestDist) {
                best = e;
                bestDist = d;
            }
        }
        if (best instanceof CsBossEntity boss) {
            BossBattleManager.onBalmHit(boss);
        } else if (best instanceof AttackBellEntity bell) {
            bell.breakByBalm();
        } else if (best instanceof CsBossMinionEntity minion) {
            BossBattleManager.onBalmProxyHit(minion);
        }
    }

    private static double distanceToBox(Vec3 p, AABB box) {
        double dx = Math.max(Math.max(box.minX - p.x, 0.0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0.0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0.0), p.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void spawnImpactParticles() {
        if (level() instanceof ServerLevel serverLevel) {
            ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, getItem());
            serverLevel.sendParticles(particle, getX(), getY(), getZ(), 12, 0.15, 0.15, 0.15, 0.25);
        }
    }
}
