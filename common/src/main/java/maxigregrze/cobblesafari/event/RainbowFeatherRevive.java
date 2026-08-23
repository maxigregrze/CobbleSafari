package maxigregrze.cobblesafari.event;

import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Server-side behaviour of the Rainbow Feather, driven by
 * {@link maxigregrze.cobblesafari.mixin.LivingEntityRainbowFeatherMixin}.
 *
 * <p>The feather is a boosted Totem of Undying. Minecraft 1.21.1 has no {@code death_protection}
 * data component - that arrives in 1.21.2 - and {@code LivingEntity#checkTotemDeathProtection} is
 * hardcoded on {@link net.minecraft.world.item.Items#TOTEM_OF_UNDYING}, so the whole revival has
 * to be reimplemented here.</p>
 */
public final class RainbowFeatherRevive {

    /** 5:00, the duration shared by the whole Rainbow Feather overlay. */
    private static final int FIVE_MINUTES = 6000;

    private RainbowFeatherRevive() {}

    /**
     * Consumes a Rainbow Feather held in either hand and revives the entity.
     *
     * @return {@code true} when a feather was consumed and death must be cancelled.
     */
    public static boolean tryConsume(LivingEntity entity, DamageSource source) {
        // Same exemption as the vanilla totem: the void and /kill still kill.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        boolean consumed = false;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = entity.getItemInHand(hand);
            if (held.is(ModItems.RAINBOW_FEATHER)) {
                held.shrink(1);
                consumed = true;
                break;
            }
        }
        if (!consumed) {
            return false;
        }

        if (entity instanceof ServerPlayer player) {
            player.awardStat(Stats.ITEM_USED.get(ModItems.RAINBOW_FEATHER));
            entity.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        }

        entity.setHealth(1.0F);
        entity.removeAllEffects();

        // The vanilla totem's three effects first...
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

        // ...then the feather's overlay. addEffect keeps the stronger instance, so the three above
        // survive only where they beat their counterpart, which is Regeneration II at 0:45.
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, FIVE_MINUTES, 3));
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIVE_MINUTES, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, FIVE_MINUTES, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, FIVE_MINUTES, 4));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, FIVE_MINUTES, 2));
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, FIVE_MINUTES, 2));

        ModNetworking.sendRainbowFeatherUsed(entity);
        return true;
    }
}
