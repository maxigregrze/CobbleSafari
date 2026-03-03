package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.effect.BasicStatusEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import java.util.function.Supplier;

public enum ModEffects {
    SHINY_BOOST("shiny_boost", () -> new BasicStatusEffect(MobEffectCategory.BENEFICIAL, 0xFFD700)),
    GREAT_SHINY_BOOST("great_shiny_boost", () -> new BasicStatusEffect(MobEffectCategory.BENEFICIAL, 0xFFA500)),
    ULTRA_SHINY_BOOST("ultra_shiny_boost", () -> new BasicStatusEffect(MobEffectCategory.BENEFICIAL, 0xFF4500)),
    UNCOMMON_BOOST("uncommon_boost", () -> new BasicStatusEffect(MobEffectCategory.BENEFICIAL, 0x00FF00)),
    RARE_BOOST("rare_boost", () -> new BasicStatusEffect(MobEffectCategory.BENEFICIAL, 0x0000FF)),
    ULTRA_RARE_BOOST("ultra_rare_boost", () -> new BasicStatusEffect(MobEffectCategory.BENEFICIAL, 0x9400D3)),
    REPEL("repel", () -> new BasicStatusEffect(MobEffectCategory.NEUTRAL, 0x808080)),
    ULTRA_BEAST_BOOST("ultra_beast_boost", () -> new BasicStatusEffect(MobEffectCategory.BENEFICIAL, 0x4B0082)),
    PARADOX_BOOST("paradox_boost", () -> new BasicStatusEffect(MobEffectCategory.BENEFICIAL, 0xFF1493)),
    LEGENDARY_BOOST("legendary_boost", () -> new BasicStatusEffect(MobEffectCategory.BENEFICIAL, 0xFFD700)),
    MYTHICAL_BOOST("mythical_boost", () -> new BasicStatusEffect(MobEffectCategory.BENEFICIAL, 0xE6E6FA));

    public final Holder<MobEffect> holder;
    public final MobEffect effect;

    ModEffects(String id, Supplier<MobEffect> factory) {
        this.effect = Registry.register(
                BuiltInRegistries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, id),
                factory.get()
        );
        this.holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this.effect);
    }

    public static void register() {
        CobbleSafari.LOGGER.info("CobbleSafari >> Registering spawn boost effects...");
        values();
    }
}
