package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

public class BucketBoostInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public BucketBoostInfluence(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void affectBucketWeights(@NotNull Map<String, Float> bucketWeights) {
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

        // Cobblemon 1.8 deleted SpawnBucket: the map key *is* the bucket name.
        for (Map.Entry<String, Float> entry : bucketWeights.entrySet()) {
            String bucketName = entry.getKey().toLowerCase(Locale.ROOT);

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
}
