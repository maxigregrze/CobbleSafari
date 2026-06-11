package maxigregrze.cobblesafari.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class RedShackledMobEffect extends MobEffect {

    public RedShackledMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xCC0000);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        return true;
    }
}
