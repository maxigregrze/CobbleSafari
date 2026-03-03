package maxigregrze.cobblesafari.item;

import maxigregrze.cobblesafari.init.ModEffects;
import maxigregrze.cobblesafari.init.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.Set;

public class IncenseItem extends Item {

    private static final Set<Holder<MobEffect>> SHINY_GROUP = Set.of(
            ModEffects.SHINY_BOOST.holder,
            ModEffects.GREAT_SHINY_BOOST.holder,
            ModEffects.ULTRA_SHINY_BOOST.holder
    );

    private static final Set<Holder<MobEffect>> PERFUME_GROUP = Set.of(
            ModEffects.UNCOMMON_BOOST.holder,
            ModEffects.RARE_BOOST.holder,
            ModEffects.ULTRA_RARE_BOOST.holder
    );

    private final Holder<MobEffect> effect;
    private final int duration;
    private final int amplifier;

    public IncenseItem(Properties properties, Holder<MobEffect> effect, int duration) {
        this(properties, effect, duration, 0);
    }

    public IncenseItem(Properties properties, Holder<MobEffect> effect, int duration, int amplifier) {
        super(properties);
        this.effect = effect;
        this.duration = duration;
        this.amplifier = amplifier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) {
            return stack;
        }

        if (!level.isClientSide) {
            removeIncompatibleEffects(player);
            applyEffect(player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SPRAY_SOUND, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return stack;
    }

    private void removeIncompatibleEffects(Player player) {
        Set<Holder<MobEffect>> incompatibleGroup = getIncompatibleGroup();
        for (Holder<MobEffect> incompatible : incompatibleGroup) {
            if (!incompatible.equals(this.effect) && player.hasEffect(incompatible)) {
                player.removeEffect(incompatible);
            }
        }
    }

    private Set<Holder<MobEffect>> getIncompatibleGroup() {
        if (SHINY_GROUP.contains(this.effect)) {
            return SHINY_GROUP;
        }
        if (PERFUME_GROUP.contains(this.effect)) {
            return PERFUME_GROUP;
        }
        return Set.of();
    }

    private void applyEffect(Player player) {
        MobEffectInstance existing = player.getEffect(this.effect);
        int finalDuration = this.duration;

        if (existing != null) {
            finalDuration = existing.getDuration() + this.duration;
        }

        player.addEffect(new MobEffectInstance(this.effect, finalDuration, this.amplifier, false, true));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
