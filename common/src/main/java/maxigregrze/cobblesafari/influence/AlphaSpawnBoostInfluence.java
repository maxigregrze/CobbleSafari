package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.compat.CobblemonFishingInterop;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModPowerEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The Alpha Power, with two distinct levers.
 *
 * <p><b>A — boss bucket.</b> Cobblemon 1.8 spawns wild alphas through the {@code boss} bucket,
 * whose only members are the 396 alpha herd files. Multiplying that bucket's weight is the
 * faithful way to make alphas more common: the player gets real herds, leader and followers.
 *
 * <p><b>B — promotion.</b> On top of that, a small chance for any other spawn to be promoted to
 * an alpha outright, which is exactly what the {@code cobblemon:alpha_chance} bait effect of the
 * Hopo Berry does.
 *
 * <p><b>Why {@code computeIfPresent} matters.</b> Only the world spawner declares a {@code boss}
 * bucket; the fishing, Poke Snack and activated-habitat weight maps do not. They do however share
 * {@code WORLD_SPAWN_POOL}, and {@code Spawner.chooseBucket} intersects the (mutated) weight map
 * with the pool's buckets — so <em>adding</em> a {@code boss} key on those spawners would survive
 * the intersection and let alpha herds spawn off a fishing bobber. Never create the key; only
 * scale it when the spawner already has it.
 */
public class AlphaSpawnBoostInfluence implements SpawningInfluence {

    /** Cobblemon's alpha-herd bucket. Declared in {@code worldBuckets} only. */
    private static final String BOSS_BUCKET = "boss";

    private final ServerPlayer player;

    public AlphaSpawnBoostInfluence(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void affectBucketWeights(@NotNull Map<String, Float> bucketWeights) {
        int level = activeAlphaLevel();
        if (level == 0) {
            return;
        }
        float multiplier = bossBucketMultiplier(level);
        if (multiplier <= 1.0f) {
            return;
        }
        bucketWeights.computeIfPresent(BOSS_BUCKET, (bucket, weight) -> weight * multiplier);
    }

    @Override
    public void affectSpawn(@NotNull SpawnAction<?> action, @NotNull Entity entity) {
        if (!(entity instanceof PokemonEntity pokemonEntity) || pokemonEntity.getPokemon().isAlpha()) {
            return;
        }
        int level = activeAlphaLevel();
        if (level == 0) {
            return;
        }
        if (player.getRandom().nextFloat() >= promotionChance(level)) {
            return;
        }
        // Sets isAlpha, rebuilds the moveset from the "cobblemon:alpha" builder, remakes the brain
        // so the alpha sensors attach, and pushes IS_ALPHA to the client. Level matching is then
        // handled continuously by AlphaLevelMatchingSensor; no manual call needed.
        CobblemonFishingInterop.alterAlphaAttempt(pokemonEntity);
    }

    private static float bossBucketMultiplier(int level) {
        return switch (level) {
            case 1 -> SpawnBoostConfig.data.effectSettings.alphaPowerLevel1BossBucketMultiplier;
            case 2 -> SpawnBoostConfig.data.effectSettings.alphaPowerLevel2BossBucketMultiplier;
            case 3 -> SpawnBoostConfig.data.effectSettings.alphaPowerLevel3BossBucketMultiplier;
            default -> 1.0f;
        };
    }

    private static float promotionChance(int level) {
        return switch (level) {
            case 1 -> SpawnBoostConfig.data.effectSettings.alphaPowerLevel1PromotionChance;
            case 2 -> SpawnBoostConfig.data.effectSettings.alphaPowerLevel2PromotionChance;
            case 3 -> SpawnBoostConfig.data.effectSettings.alphaPowerLevel3PromotionChance;
            default -> 0.0f;
        };
    }

    private int activeAlphaLevel() {
        for (int lv = 3; lv >= 1; lv--) {
            if (player.hasEffect(ModPowerEffects.alpha(lv))) {
                return lv;
            }
        }
        return 0;
    }
}
