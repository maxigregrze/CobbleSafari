package maxigregrze.cobblesafari.item.donut;

import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModComponents;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.power.GuaranteedShinyManager;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class DonutItem extends Item {

    private static FoodProperties cachedGoldenAppleFood;

    public DonutItem(Properties properties) {
        super(properties);
    }

    private static DonutFlavorComponent getFlavor(ItemStack stack) {
        return stack.get(ModComponents.DONUT_FLAVOR);
    }

    private static FoodProperties goldenAppleFood() {
        if (cachedGoldenAppleFood == null) {
            cachedGoldenAppleFood = new ItemStack(Items.GOLDEN_APPLE).get(DataComponents.FOOD);
        }
        return cachedGoldenAppleFood;
    }

    private static void applyGoldenAppleConsumption(ServerPlayer player, RandomSource random) {
        FoodProperties food = goldenAppleFood();
        if (food == null) {
            return;
        }
        player.getFoodData().eat(food);
        for (FoodProperties.PossibleEffect possible : food.effects()) {
            if (random.nextFloat() < possible.probability()) {
                player.addEffect(possible.effect());
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        DonutFlavorComponent comp = getFlavor(stack);
        if (comp == null) {
            return Component.translatable("item.cobblesafari.donut");
        }
        String key = "item.cobblesafari.donut." + comp.flavor().getSerializedName() + "." + comp.tier();
        Style style = nameStyleForTier(comp.tier());
        return Component.translatable(key).withStyle(style);
    }

    private static Style nameStyleForTier(int tier) {
        if (tier >= 4) {
            return Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
        }
        if (tier >= 2) {
            return Style.EMPTY.withColor(ChatFormatting.AQUA);
        }
        return Style.EMPTY.withColor(ChatFormatting.YELLOW);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DonutFlavorComponent comp = getFlavor(stack);
        if (comp == null) {
            tooltip.add(Component.translatable("tooltip.cobblesafari.donut.plain").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return DonutTooltipPayload.fromStack(stack).map(p -> p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getFlavor(stack) == null) {
            FoodProperties golden = goldenAppleFood();
            if (golden != null && player.canEat(golden.canAlwaysEat())) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        if (player.canEat(true)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (getFlavor(stack) == null) {
            FoodProperties golden = goldenAppleFood();
            if (golden != null) {
                return golden.eatDurationTicks();
            }
        }
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (player.getAbilities().instabuild) {
            return;
        }
        DonutFlavorComponent comp = getFlavor(stack);
        if (!DonutFlavorLogic.isUnrolledShell(comp) || comp.flavor() == DonutMainFlavor.MIX) {
            return;
        }
        stack.set(ModComponents.DONUT_FLAVOR, DonutFlavorLogic.generateDonut(comp.flavor(), comp.tier()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        DonutFlavorComponent comp = getFlavor(stack);
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (comp != null) {
                int ticks = Math.max(1, comp.calories()) * 20;
                ModPowerEffects.removeAllDonutPowerEffects(player);
                GuaranteedShinyManager.clearByPrefix(player, "sparkling:");
                for (DonutBonus b : comp.bonuses()) {
                    Holder<MobEffect> holder = ModPowerEffects.resolveBonus(b);
                    if (holder != null) {
                        player.addEffect(new MobEffectInstance(holder, ticks, 0, false, true, true));
                        if ("sparkling".equals(b.powerId()) && b.level() == 3) {
                            Integer variant = b.type() == PowerVariantRegistry.INDEX_ALL ? null : b.type();
                            GuaranteedShinyManager.requestForEffect(player,
                                    "sparkling:" + PowerVariantRegistry.suffix(b.type()) + "_3",
                                    holder, variant, ticks,
                                    SpawnBoostConfig.data.effectSettings.sparklingGuaranteeWindowFraction);
                        }
                    }
                }
            } else {
                applyGoldenAppleConsumption(player, player.getRandom());
            }
        }
        if (comp == null) {
            if (!level.isClientSide() && entity instanceof Player p && !p.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return stack;
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
