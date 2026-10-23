package maxigregrze.cobblesafari.entity.projectile;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Harlequin Bomb (action plan 192, section 5). The firework explosion is rolled when the item is set
 * on the entity - i.e. at throw time - and travels in the synced item stack, so the client already
 * knows it when the impact event arrives.
 */
public class BlacephalonBombEntity extends ThrowableItemProjectile {

    private static final byte EVENT_FIREWORK = 17;
    /** Firework rocket damage for a single star: 5 + 2 × 1. */
    private static final float BASE_DAMAGE = 7.0f;
    private static final double RADIUS = 5.0;

    public BlacephalonBombEntity(EntityType<? extends BlacephalonBombEntity> type, Level level) {
        super(type, level);
    }

    public BlacephalonBombEntity(Level level, LivingEntity thrower) {
        super(ModEntities.BLACEPHALON_BOMB, thrower, level);
    }

    public BlacephalonBombEntity(Level level, double x, double y, double z) {
        super(ModEntities.BLACEPHALON_BOMB, x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BLACEPHALON_BOMB;
    }

    @Override
    public void setItem(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        if (!copy.has(DataComponents.FIREWORK_EXPLOSION)) {
            copy.set(DataComponents.FIREWORK_EXPLOSION, randomExplosion(random));
        }
        super.setItem(copy);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide()) {
            return;
        }
        level().broadcastEntityEvent(this, EVENT_FIREWORK);
        dealExplosionDamage();
        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_FIREWORK) {
            FireworkExplosion explosion = getItem().get(DataComponents.FIREWORK_EXPLOSION);
            if (explosion != null) {
                level().createFireworks(getX(), getY(), getZ(), 0.0, 0.0, 0.0, List.of(explosion));
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** {@code FireworkRocketEntity#dealExplosionDamage} for one star: line of sight, sqrt falloff. */
    private void dealExplosionDamage() {
        Vec3 center = position();
        DamageSource source = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.FIREWORKS), this, getOwner());
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RADIUS))) {
            if (distanceToSqr(target) > RADIUS * RADIUS || !hasLineOfSight(center, target)) {
                continue;
            }
            float damage = BASE_DAMAGE * (float) Math.sqrt((RADIUS - distanceTo(target)) / RADIUS);
            target.hurt(source, damage);
        }
    }

    private boolean hasLineOfSight(Vec3 from, LivingEntity target) {
        for (int i = 0; i < 2; i++) {
            Vec3 to = new Vec3(target.getX(), target.getY(0.5 * i), target.getZ());
            if (level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                    .getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    private static FireworkExplosion randomExplosion(RandomSource random) {
        FireworkExplosion.Shape[] shapes = FireworkExplosion.Shape.values();
        return new FireworkExplosion(
                shapes[random.nextInt(shapes.length)],
                randomColors(random, 1 + random.nextInt(3)),
                randomColors(random, random.nextInt(3)),
                random.nextBoolean(),
                random.nextBoolean());
    }

    private static IntList randomColors(RandomSource random, int count) {
        DyeColor[] dyes = DyeColor.values();
        IntList colors = new IntArrayList(count);
        for (int i = 0; i < count; i++) {
            colors.add(dyes[random.nextInt(dyes.length)].getFireworkColor());
        }
        return colors;
    }
}
