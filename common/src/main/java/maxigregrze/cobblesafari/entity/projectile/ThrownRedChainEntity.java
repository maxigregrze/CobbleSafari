package maxigregrze.cobblesafari.entity.projectile;



import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import maxigregrze.cobblesafari.effect.RedShackledEffects;

import maxigregrze.cobblesafari.init.ModEffects;

import maxigregrze.cobblesafari.init.ModEntities;

import maxigregrze.cobblesafari.init.ModItems;

import net.minecraft.world.effect.MobEffectInstance;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.projectile.ThrowableItemProjectile;

import net.minecraft.world.item.Item;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.EntityHitResult;

import net.minecraft.world.phys.HitResult;



public class ThrownRedChainEntity extends ThrowableItemProjectile {



    public static final int MAX_LIFETIME_TICKS = 100;



    public ThrownRedChainEntity(EntityType<? extends ThrownRedChainEntity> type, Level level) {

        super(type, level);

    }



    public ThrownRedChainEntity(Level level, LivingEntity thrower) {

        super(ModEntities.THROWN_RED_CHAIN, thrower, level);

    }



    @Override

    protected Item getDefaultItem() {

        return ModItems.RED_CHAIN;

    }



    @Override

    protected double getDefaultGravity() {

        return super.getDefaultGravity() / 4.0;

    }



    @Override

    public void tick() {

        super.tick();

        setXRot(0.0f);



        if (!level().isClientSide() && isAlive() && tickCount >= MAX_LIFETIME_TICKS) {

            dropChainAndDiscard();

        }

    }



    @Override

    protected void onHitEntity(EntityHitResult result) {

        super.onHitEntity(result);

        if (level().isClientSide()) {

            return;

        }



        if (result.getEntity() instanceof LivingEntity living) {

            applyShackle(living);

            discard();

        }

    }



    @Override

    protected void onHit(HitResult result) {

        super.onHit(result);

        if (level().isClientSide()) {

            return;

        }

        if (result.getType() == HitResult.Type.BLOCK) {

            dropChainAndDiscard();

        }

    }



    private void applyShackle(LivingEntity living) {

        int duration = living instanceof PokemonEntity

                ? RedShackledEffects.DURATION_TICKS_POKEMON

                : RedShackledEffects.DURATION_TICKS_DEFAULT;

        living.addEffect(new MobEffectInstance(ModEffects.RED_SHACKLED.holder, duration, 0, false, true));

    }



    private void dropChainAndDiscard() {

        if (!isAlive()) {

            return;

        }

        if (!wasThrownFromCreative()) {

            spawnAtLocation(new ItemStack(ModItems.RED_CHAIN));

        }

        discard();

    }



    private boolean wasThrownFromCreative() {

        if (!(getOwner() instanceof Player player)) {

            return false;

        }

        return player.getAbilities().instabuild;

    }

}

