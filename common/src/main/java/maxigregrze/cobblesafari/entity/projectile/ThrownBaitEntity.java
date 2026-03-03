package maxigregrze.cobblesafari.entity.projectile;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.safari.SafariPokemonState;
import maxigregrze.cobblesafari.safari.SafariStateManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownBaitEntity extends ThrowableItemProjectile {

    public ThrownBaitEntity(EntityType<? extends ThrownBaitEntity> type, Level level) {
        super(type, level);
    }

    public ThrownBaitEntity(Level level, LivingEntity thrower) {
        super(ModEntities.THROWN_BAIT, thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BAIT;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide()) return;

        if (result.getEntity() instanceof PokemonEntity pokemon && pokemon.getPokemon().isWild()) {
            if (!SafariStateManager.isInSafariDimension(pokemon)) {
                if (getOwner() instanceof ServerPlayer player) {
                    player.sendSystemMessage(Component.translatable(
                            "cobblesafari.safari.item_outside_safari",
                            Component.translatable("item.cobblesafari.bait")
                    ));
                }
                return;
            }

            SafariPokemonState state = SafariStateManager.getState(pokemon.getUUID());
            if (state != null && state.isFleeing()) {
                SafariStateManager.triggerImmediateDespawn(pokemon);
                return;
            }

            SafariStateManager.applyBait(pokemon);

            level().playSound(null, pokemon.getX(), pokemon.getY(), pokemon.getZ(),
                    SoundEvents.ITEM_PICKUP, pokemon.getSoundSource(), 0.8f, 0.8f);

            SafariStateManager.tryFlee(pokemon);
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

    private void spawnImpactParticles() {
        if (level() instanceof ServerLevel serverLevel) {
            ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(getDefaultItem()));
            serverLevel.sendParticles(particle, getX(), getY(), getZ(), 12, 0.15, 0.15, 0.15, 0.25);
        }
    }
}
