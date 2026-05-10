package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.api.spawning.position.calculators.SpawnablePositionCalculator;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.power.PowerSpawnPredicates;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TypedEncounterBoostInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public TypedEncounterBoostInfluence(ServerPlayer player) {
        this.player = player;
    }

    private static double getEffectDistanceSq() {
        return Math.pow(SpawnBoostConfig.data.effectSettings.effectDistanceBlocks, 2);
    }

    @Override
    public float affectWeight(@NotNull SpawnDetail detail, @NotNull SpawnablePosition spawnablePosition, float weight) {
        if (!(detail instanceof PokemonSpawnDetail psd)) {
            return weight;
        }
        if (player.blockPosition().distSqr(spawnablePosition.getPosition()) > getEffectDistanceSq()) {
            return weight;
        }

        EncounterSel sel = findEncounter(psd);
        if (sel == null) {
            return weight;
        }

        float bonus = switch (sel.level()) {
            case 1 -> SpawnBoostConfig.data.effectSettings.encounterPowerLevel1SpawnBonus;
            case 2 -> SpawnBoostConfig.data.effectSettings.encounterPowerLevel2SpawnBonus;
            case 3 -> SpawnBoostConfig.data.effectSettings.encounterPowerLevel3SpawnBonus;
            default -> 0.0f;
        };
        return weight * (1.0f + bonus);
    }

    private EncounterSel findEncounter(PokemonSpawnDetail detail) {
        for (int lv = 3; lv >= 1; lv--) {
            for (int vi = 0; vi < PowerVariantRegistry.ELEMENTAL_COUNT; vi++) {
                if (!player.hasEffect(ModPowerEffects.encounter(vi, lv))) {
                    continue;
                }
                if (PowerSpawnPredicates.detailMatchesVariant(detail, vi)) {
                    return new EncounterSel(lv);
                }
            }
            if (player.hasEffect(ModPowerEffects.encounter(PowerVariantRegistry.INDEX_ALL, lv))
                    && PowerSpawnPredicates.detailMatchesVariant(detail, PowerVariantRegistry.INDEX_ALL)) {
                return new EncounterSel(lv);
            }
        }
        return null;
    }

    private record EncounterSel(int level) {}

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
