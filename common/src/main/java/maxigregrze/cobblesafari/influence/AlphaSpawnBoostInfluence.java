package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.api.spawning.position.calculators.SpawnablePositionCalculator;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.compat.CobblemonFishingInterop;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AlphaSpawnBoostInfluence implements SpawningInfluence {

    private static final float BASE_ALPHA_ROLL_CHANCE = 0.01f;

    private final ServerPlayer player;

    public AlphaSpawnBoostInfluence(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void affectSpawn(@NotNull SpawnAction<?> action, @NotNull Entity entity) {
        if (!(entity instanceof PokemonEntity pe)) {
            return;
        }
        int level = activeAlphaLevel();
        if (level == 0) {
            return;
        }
        float mult = switch (level) {
            case 1 -> SpawnBoostConfig.data.effectSettings.alphaPowerLevel1BoostMultiplier;
            case 2 -> SpawnBoostConfig.data.effectSettings.alphaPowerLevel2BoostMultiplier;
            case 3 -> SpawnBoostConfig.data.effectSettings.alphaPowerLevel3BoostMultiplier;
            default -> 1.0f;
        };
        float chance = Math.min(1.0f, BASE_ALPHA_ROLL_CHANCE * mult);
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        CobblemonFishingInterop.alterAlphaAttempt(pe);
    }

    private int activeAlphaLevel() {
        for (int lv = 3; lv >= 1; lv--) {
            if (player.hasEffect(ModPowerEffects.alpha(lv))) {
                return lv;
            }
        }
        return 0;
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
    public void affectAction(@NotNull SpawnAction<?> action) {
        // No-op: this influence does not modify spawn actions.
    }

    @Override
    public void affectBucketWeights(@NotNull Map<SpawnBucket, Float> bucketWeights) {
        // No-op: this influence does not alter bucket weights.
    }

    @Override
    public boolean affectSpawnable(@NotNull SpawnDetail detail, @NotNull SpawnablePosition spawnablePosition) {
        return true;
    }

    @Override
    public boolean isAllowedPosition(@NotNull ServerLevel world, @NotNull BlockPos pos, @NotNull SpawnablePositionCalculator<?, ?> calculator) {
        return true;
    }
}
