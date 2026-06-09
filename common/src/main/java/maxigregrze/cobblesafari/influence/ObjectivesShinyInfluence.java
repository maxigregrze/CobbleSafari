package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.objectives.ObjectivesManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Forces one guaranteed shiny spawn for a {@code task_catch_shiny} objective, but only while the
 * player is inside the objective's dimension/instance (plan 118 §8.3). Bound to the assignment, so
 * it pauses when the player leaves and is discarded when the assignment is removed.
 */
public class ObjectivesShinyInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public ObjectivesShinyInfluence(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void affectSpawn(@NotNull SpawnAction<?> action, @NotNull Entity entity) {
        if (entity instanceof PokemonEntity pe) {
            ObjectivesManager.tryConsumeShinyGuarantee(player, pe);
        }
    }
}
