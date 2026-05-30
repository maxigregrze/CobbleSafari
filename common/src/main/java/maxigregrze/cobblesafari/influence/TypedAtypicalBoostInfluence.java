package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import maxigregrze.cobblesafari.power.PowerSpawnPredicates;
import maxigregrze.cobblesafari.power.PowerVariantRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;

public class TypedAtypicalBoostInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public TypedAtypicalBoostInfluence(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public float affectWeight(@NotNull SpawnDetail detail, @NotNull SpawnablePosition spawnablePosition, float weight) {
        if (!(detail instanceof PokemonSpawnDetail psd)) {
            return weight;
        }

        AtypicalSel sel = findAtypical(psd);
        if (sel == null) {
            return weight;
        }

        String bucketName = detail.getBucket().getName().toLowerCase();
        boolean ultra = bucketName.contains("ultra-rare") || bucketName.contains("ultrarare");
        boolean rareOnly = bucketName.contains("rare") && !ultra;
        boolean uncommon = bucketName.contains("uncommon");

        int level = sel.level();
        if (level == 1 && !uncommon) {
            return weight;
        }
        if (level == 2 && !rareOnly) {
            return weight;
        }
        if (level == 3 && !ultra) {
            return weight;
        }

        float mult = switch (level) {
            case 1 -> SpawnBoostConfig.data.effectSettings.uncommonBoostMultiplier;
            case 2 -> SpawnBoostConfig.data.effectSettings.rareBoostMultiplier;
            case 3 -> SpawnBoostConfig.data.effectSettings.ultraRareBoostMultiplier;
            default -> 1.0f;
        };

        MobEffectInstance inst = player.getEffect(ModPowerEffects.atypical(sel.variantIndex(), level));
        int amp = inst != null ? inst.getAmplifier() : 0;
        return weight * mult * (amp + 1);
    }

    private AtypicalSel findAtypical(PokemonSpawnDetail detail) {
        for (int level = 3; level >= 1; level--) {
            for (int vi = 0; vi < PowerVariantRegistry.VARIANT_COUNT; vi++) {
                if (!player.hasEffect(ModPowerEffects.atypical(vi, level))) {
                    continue;
                }
                if (!PowerSpawnPredicates.detailMatchesVariant(detail, vi)) {
                    continue;
                }
                return new AtypicalSel(vi, level);
            }
        }
        return null;
    }

    private record AtypicalSel(int variantIndex, int level) {}
}
