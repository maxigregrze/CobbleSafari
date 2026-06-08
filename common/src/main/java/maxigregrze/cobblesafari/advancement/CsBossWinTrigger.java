package maxigregrze.cobblesafari.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Advancement trigger for surviving a CSBoss fight to completion (final phase victory).
 * Optional JSON conditions: {@code dimension} (resource location) and {@code boss} (root boss id).
 */
public class CsBossWinTrigger extends SimpleCriterionTrigger<CsBossWinTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, ResourceKey<Level> dimension, String rootBossId) {
        this.trigger(player, instance -> instance.matches(dimension, rootBossId));
    }

    public record Instance(
            Optional<ContextAwarePredicate> player,
            Optional<ResourceLocation> dimension,
            Optional<String> boss
    ) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(Instance::dimension),
                Codec.STRING.optionalFieldOf("boss").forGetter(Instance::boss)
        ).apply(i, Instance::new));

        public boolean matches(ResourceKey<Level> actualDimension, String actualRootBossId) {
            if (dimension.isPresent() && !dimension.get().equals(actualDimension.location())) {
                return false;
            }
            if (boss.isPresent() && !boss.get().equals(actualRootBossId)) {
                return false;
            }
            return true;
        }
    }
}
