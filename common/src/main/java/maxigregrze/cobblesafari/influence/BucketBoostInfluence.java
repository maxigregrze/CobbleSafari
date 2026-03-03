package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.api.spawning.position.calculators.SpawnablePositionCalculator;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class BucketBoostInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public BucketBoostInfluence(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void affectBucketWeights(@NotNull Map<SpawnBucket, Float> bucketWeights) {
        float uncommonMultiplier = 1.0f;
        float rareMultiplier = 1.0f;
        float ultraRareMultiplier = 1.0f;

        if (player.hasEffect(ModEffects.UNCOMMON_BOOST.holder)) {
            int amp = player.getEffect(ModEffects.UNCOMMON_BOOST.holder).getAmplifier();
            uncommonMultiplier = SpawnBoostConfig.data.effectSettings.uncommonBoostMultiplier * (amp + 1);
        }
        if (player.hasEffect(ModEffects.RARE_BOOST.holder)) {
            int amp = player.getEffect(ModEffects.RARE_BOOST.holder).getAmplifier();
            rareMultiplier = SpawnBoostConfig.data.effectSettings.rareBoostMultiplier * (amp + 1);
        }
        if (player.hasEffect(ModEffects.ULTRA_RARE_BOOST.holder)) {
            int amp = player.getEffect(ModEffects.ULTRA_RARE_BOOST.holder).getAmplifier();
            ultraRareMultiplier = SpawnBoostConfig.data.effectSettings.ultraRareBoostMultiplier * (amp + 1);
        }

        for (Map.Entry<SpawnBucket, Float> entry : bucketWeights.entrySet()) {
            SpawnBucket bucket = entry.getKey();
            String bucketName = bucket.getName().toLowerCase();

            if (bucketName.contains("uncommon") && uncommonMultiplier > 1.0f) {
                entry.setValue(entry.getValue() * uncommonMultiplier);
            } else if (bucketName.contains("ultra-rare") || bucketName.contains("ultrarare")) {
                if (ultraRareMultiplier > 1.0f) {
                    entry.setValue(entry.getValue() * ultraRareMultiplier);
                }
            } else if (bucketName.contains("rare") && rareMultiplier > 1.0f) {
                entry.setValue(entry.getValue() * rareMultiplier);
            }
        }
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public float affectWeight(@NotNull SpawnDetail detail, @NotNull SpawnablePosition spawnablePosition, float weight) {
        return weight;
    }

    @Override
    public void affectAction(@NotNull SpawnAction<?> action) {}

    @Override
    public void affectSpawn(@NotNull SpawnAction<?> action, @NotNull Entity entity) {}

    @Override
    public boolean affectSpawnable(@NotNull SpawnDetail detail, @NotNull SpawnablePosition spawnablePosition) {
        return true;
    }

    @Override
    public boolean isAllowedPosition(@NotNull ServerLevel world, @NotNull BlockPos pos, @NotNull SpawnablePositionCalculator<?, ?> calculator) {
        return true;
    }
}
