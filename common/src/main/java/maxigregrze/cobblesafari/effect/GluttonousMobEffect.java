package maxigregrze.cobblesafari.effect;

import maxigregrze.cobblesafari.item.ultraloot.GluttonArmorLogic;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Gluttonous (action plan 192, section 11.3): drains one saturation point - or one food point once
 * saturation is empty - every 5 s × level, and keeps the food-dependent Resistance / Regeneration /
 * Strength tier.
 */
public class GluttonousMobEffect extends MobEffect {

    private static final long DRAIN_TICKS_PER_LEVEL = 100L;
    private static final long BONUS_INTERVAL = 20L;

    public GluttonousMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xB0562A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide() || !(entity instanceof Player player)) {
            return true;
        }
        long time = entity.level().getGameTime();
        if (time % (DRAIN_TICKS_PER_LEVEL * (amplifier + 1)) == 0) {
            GluttonArmorLogic.drainOnePoint(player);
        }
        if (time % BONUS_INTERVAL == 0) {
            GluttonArmorLogic.applyBonuses(player);
        }
        return true;
    }
}
