package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.api.spawning.position.calculators.SpawnablePositionCalculator;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RepelInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public RepelInfluence(ServerPlayer player) {
        this.player = player;
    }

    private static double getEffectDistanceSq() {
        return Math.pow(SpawnBoostConfig.data.effectSettings.effectDistanceBlocks, 2);
    }

    @Override
    public boolean affectSpawnable(@NotNull SpawnDetail detail, @NotNull SpawnablePosition position) {
        if (!player.hasEffect(ModEffects.REPEL.holder)) return true;
        if (player.blockPosition().distSqr(position.getPosition()) > getEffectDistanceSq()) return true;

        return !(detail instanceof PokemonSpawnDetail);
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
    public void affectBucketWeights(@NotNull Map<SpawnBucket, Float> bucketWeights) {}

    @Override
    public boolean isAllowedPosition(@NotNull ServerLevel world, @NotNull BlockPos pos, @NotNull SpawnablePositionCalculator<?, ?> calculator) {
        return true;
    }
}
