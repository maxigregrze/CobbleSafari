package maxigregrze.cobblesafari.effect;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class LuckPowerEffect extends MobEffect {

    public LuckPowerEffect(int color, double luckBonus, String modifierPath) {
        super(MobEffectCategory.BENEFICIAL, color);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, modifierPath);
        addAttributeModifier(Attributes.LUCK, id, luckBonus, AttributeModifier.Operation.ADD_VALUE);
    }
}
