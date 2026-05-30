package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import maxigregrze.cobblesafari.config.SpawnBoostConfig;
import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

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
}
