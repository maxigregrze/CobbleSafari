package maxigregrze.cobblesafari.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * One-shot event carrying an id (e.g. a Warp Ride destination). The JSON may restrict it with an
 * optional {@code id}; without one, any id matches. One criterion per id gives a vanilla X/N progress bar.
 */
public class IdEventTrigger extends SimpleCriterionTrigger<IdEventTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, String id) {
        this.trigger(player, instance -> instance.matches(id));
    }

    public record Instance(Optional<ContextAwarePredicate> player, Optional<String> id)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.optionalFieldOf("id").forGetter(Instance::id)
        ).apply(i, Instance::new));

        public boolean matches(String actualId) {
            return id.isEmpty() || id.get().equals(actualId);
        }
    }
}
