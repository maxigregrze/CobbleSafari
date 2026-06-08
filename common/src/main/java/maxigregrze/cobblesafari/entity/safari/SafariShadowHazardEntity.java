package maxigregrze.cobblesafari.entity.safari;

import maxigregrze.cobblesafari.csboss.CsBossDamage;
import maxigregrze.cobblesafari.csboss.attack.CsBossAttackLib;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.safari.hazard.SafariHazardLib;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible driver for a single fire/water column cycle at a fixed position (plan 115).
 * Particles only — no shadow entity.
 */
public class SafariShadowHazardEntity extends Entity {

    public enum Kind {
        FIRE,
        WATER
    }

    private static final int COLUMN_START = 80;
    private static final int COLUMN_END = 110;
    private static final int DISCARD_AT = 110;
    private static final int MAX_AGE = 120;
    private static final double COLUMN_RADIUS = 1.0;
    private static final double COLUMN_HEIGHT = 4.0;
    private static final float FIRE_DAMAGE = 1.0F;
    private static final int FIRE_TICKS = 60;
    private static final float WATER_DAMAGE = 8.0F;

    private Kind kind = Kind.FIRE;
    private int age;

    public SafariShadowHazardEntity(EntityType<? extends SafariShadowHazardEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static void spawnFire(ServerLevel level, Vec3 spawn) {
        SafariShadowHazardEntity driver = create(level, spawn, Kind.FIRE);
        level.addFreshEntity(driver);
        CsBossAttackLib.sound(level, spawn.x, spawn.y, spawn.z,
                "minecraft:block.fire.ambient", SoundSource.HOSTILE, 0.8F, 1.2F);
    }

    public static void spawnWater(ServerLevel level, Vec3 spawn) {
        SafariShadowHazardEntity driver = create(level, spawn, Kind.WATER);
        level.addFreshEntity(driver);
        CsBossAttackLib.sound(level, spawn.x, spawn.y, spawn.z,
                "minecraft:entity.generic.splash", SoundSource.HOSTILE, 0.8F, 1.2F);
    }

    private static SafariShadowHazardEntity create(ServerLevel level, Vec3 spawn, Kind kind) {
        SafariShadowHazardEntity driver = new SafariShadowHazardEntity(ModEntities.SAFARI_SHADOW_HAZARD, level);
        driver.kind = kind;
        driver.moveTo(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
        return driver;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (++age > MAX_AGE) {
            this.discard();
            return;
        }
        drive((ServerLevel) this.level());
        if (age >= DISCARD_AT) {
            this.discard();
        }
    }

    private void drive(ServerLevel level) {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        if (age < COLUMN_START) {
            var telegraph = kind == Kind.FIRE ? ParticleTypes.FLAME : ParticleTypes.BUBBLE_COLUMN_UP;
            CsBossAttackLib.risingTelegraph(level, x, y, z, telegraph);
        } else if (age >= COLUMN_START && age < COLUMN_END) {
            if (age == COLUMN_START) {
                String sound = kind == Kind.FIRE
                        ? "cobblemon:move.eruption.actor"
                        : "cobblemon:move.waterpulse.actor";
                CsBossAttackLib.sound(level, x, y, z, sound, SoundSource.HOSTILE, 1.3F, 1.0F);
            }
            if (kind == Kind.FIRE) {
                CsBossAttackLib.flameColumn(level, x, y, z, COLUMN_HEIGHT);
                if (age % 5 == 0) {
                    SafariHazardLib.damagePlayersInColumn(level, x, y, z,
                            COLUMN_RADIUS, COLUMN_HEIGHT, FIRE_DAMAGE, FIRE_TICKS);
                }
            } else {
                CsBossAttackLib.bubbleColumn(level, x, y, z, COLUMN_HEIGHT);
                if (age % 5 == 0) {
                    SafariHazardLib.damagePlayersInColumn(level, x, y, z,
                            COLUMN_RADIUS, COLUMN_HEIGHT, CsBossDamage.bullet(level), WATER_DAMAGE, 0);
                }
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // noSave entity
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // noSave entity
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
