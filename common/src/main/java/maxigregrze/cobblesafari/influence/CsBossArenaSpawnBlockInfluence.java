package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import maxigregrze.cobblesafari.csboss.BossBattleManager;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Blocks wild Pokémon spawns in the vicinity of an active CSBoss arena
 * (horizontal square of max(playerRadius, blockRadius), any height). No-op
 * while no fight is running.
 */
public class CsBossArenaSpawnBlockInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public CsBossArenaSpawnBlockInfluence(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public boolean affectSpawnable(@NotNull SpawnDetail detail, @NotNull SpawnablePosition position) {
        if (!SpawnDetails.isPokemonSpawn(detail)) {
            return true;
        }
        return !BossBattleManager.isNearActiveArena(player.level().dimension(), position.getPosition());
    }
}
