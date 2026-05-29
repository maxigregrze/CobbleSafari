package maxigregrze.cobblesafari.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Generic count-based advancement trigger. The JSON sets the threshold via the {@code count} field;
 * game code calls {@link #trigger(ServerPlayer, int)} with the current running total.
 */
public class CountTrigger extends SimpleCriterionTrigger<CountTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, int currentCount) {
        this.trigger(player, instance -> instance.matches(currentCount));
    }

    public record Instance(Optional<ContextAwarePredicate> player, int requiredCount)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Instance::requiredCount)
        ).apply(i, Instance::new));

        public boolean matches(int currentCount) {
            return currentCount >= requiredCount;
        }
    }
}
