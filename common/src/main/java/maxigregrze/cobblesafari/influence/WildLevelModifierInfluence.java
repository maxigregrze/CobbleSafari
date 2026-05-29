package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.api.spawning.position.calculators.SpawnablePositionCalculator;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class WildLevelModifierInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public WildLevelModifierInfluence(ServerPlayer player) {
        this.player = player;
    }

    private static double getEffectDistanceSq() {
        return Math.pow(SpawnBoostConfig.data.effectSettings.effectDistanceBlocks, 2);
    }

    @Override
    public void affectSpawn(@NotNull SpawnAction<?> action, @NotNull Entity entity) {
        if (!(entity instanceof PokemonEntity pe)) {
            return;
        }
        if (!pe.getPokemon().isWild()) {
            return;
        }
        if (player.blockPosition().distSqr(pe.blockPosition()) > getEffectDistanceSq()) {
            return;
        }

        int humongoDelta = activeHumongoDelta();
        int teensyLoss = activeTeensyLoss();
        if (humongoDelta == 0 && teensyLoss == 0) {
            return;
        }

        int level = pe.getPokemon().getLevel();
        int next = level + humongoDelta - teensyLoss;
        int max = Cobblemon.INSTANCE.getConfig().getMaxPokemonLevel();
        next = Math.min(max, Math.max(1, next));
        pe.getPokemon().setLevel(next);
    }

    private int activeHumongoDelta() {
        for (int lv = 3; lv >= 1; lv--) {
            if (player.hasEffect(ModPowerEffects.humongo(lv))) {
                return switch (lv) {
                    case 1 -> SpawnBoostConfig.data.effectSettings.humongoPowerLevel1LevelDelta;
                    case 2 -> SpawnBoostConfig.data.effectSettings.humongoPowerLevel2LevelDelta;
                    case 3 -> SpawnBoostConfig.data.effectSettings.humongoPowerLevel3LevelDelta;
                    default -> 0;
                };
            }
        }
        return 0;
    }

    private int activeTeensyLoss() {
        for (int lv = 3; lv >= 1; lv--) {
            if (player.hasEffect(ModPowerEffects.teensy(lv))) {
                return switch (lv) {
                    case 1 -> SpawnBoostConfig.data.effectSettings.teensyPowerLevel1LevelLoss;
                    case 2 -> SpawnBoostConfig.data.effectSettings.teensyPowerLevel2LevelLoss;
                    case 3 -> SpawnBoostConfig.data.effectSettings.teensyPowerLevel3LevelLoss;
                    default -> 0;
                };
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
