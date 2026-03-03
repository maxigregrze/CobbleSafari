package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.api.spawning.position.calculators.SpawnablePositionCalculator;
import com.cobblemon.mod.common.pokemon.Species;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class TagBoostInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public TagBoostInfluence(ServerPlayer player) {
        this.player = player;
    }

    private static double getEffectDistanceSq() {
        return Math.pow(SpawnBoostConfig.data.effectSettings.effectDistanceBlocks, 2);
    }

    @Override
    public float affectWeight(@NotNull SpawnDetail detail, @NotNull SpawnablePosition spawnablePosition, float weight) {
        return weight;
    }
    /*
    @Override
    public float affectWeight(@NotNull SpawnDetail detail, @NotNull SpawnablePosition spawnablePosition, float weight) {
        if (!(detail instanceof PokemonSpawnDetail pkm)) return weight;
        if (player.blockPosition().distSqr(spawnablePosition.getPosition()) > getEffectDistanceSq()) return weight;

        Set<String> labels = resolveLabels(pkm);
        if (labels.isEmpty()) return weight;

        float multiplier = 1.0f;

        if (player.hasEffect(ModEffects.ULTRA_BEAST_BOOST.holder) && labels.contains("ultra_beast")) {
            int amp = player.getEffect(ModEffects.ULTRA_BEAST_BOOST.holder).getAmplifier();
            multiplier *= SpawnBoostConfig.data.effectSettings.ultraBeastBoostMultiplier * (amp + 1);
        }
        if (player.hasEffect(ModEffects.PARADOX_BOOST.holder) && labels.contains("paradox")) {
            int amp = player.getEffect(ModEffects.PARADOX_BOOST.holder).getAmplifier();
            multiplier *= SpawnBoostConfig.data.effectSettings.paradoxBoostMultiplier * (amp + 1);
        }
        if (player.hasEffect(ModEffects.LEGENDARY_BOOST.holder) && labels.contains("legendary")) {
            int amp = player.getEffect(ModEffects.LEGENDARY_BOOST.holder).getAmplifier();
            multiplier *= SpawnBoostConfig.data.effectSettings.legendaryBoostMultiplier * (amp + 1);
        }
        if (player.hasEffect(ModEffects.MYTHICAL_BOOST.holder) && labels.contains("mythical")) {
            int amp = player.getEffect(ModEffects.MYTHICAL_BOOST.holder).getAmplifier();
            multiplier *= SpawnBoostConfig.data.effectSettings.mythicalBoostMultiplier * (amp + 1);
        }

        return weight * multiplier;
    }
    */

    /*
    private Set<String> resolveLabels(PokemonSpawnDetail detail) {
        try {
            if (detail.getPokemon() != null) {
                String speciesId = detail.getPokemon().getSpecies();
                if (speciesId != null) {
                    ResourceLocation identifier = speciesId.indexOf(':') >= 0 
                            ? ResourceLocation.parse(speciesId) 
                            : ResourceLocation.fromNamespaceAndPath("cobblemon", speciesId);
                    Species species = PokemonSpecies.INSTANCE.getByIdentifier(identifier);
                    if (species != null) {
                        return species.getLabels();
                    }
                }
            }
        } catch (Exception ignored) {}
        return Set.of();
    }
    */

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public void affectAction(@NotNull SpawnAction<?> action) {}

    @Override
    public void affectSpawn(@NotNull SpawnAction<?> action, @NotNull Entity entity) {}

    @Override
    public void affectBucketWeights(@NotNull Map<SpawnBucket, Float> bucketWeights) {}

    @Override
    public boolean affectSpawnable(@NotNull SpawnDetail detail, @NotNull SpawnablePosition spawnablePosition) {
        return true;
    }

    @Override
    public boolean isAllowedPosition(@NotNull ServerLevel world, @NotNull BlockPos pos, @NotNull SpawnablePositionCalculator<?, ?> calculator) {
        return true;
    }
}
