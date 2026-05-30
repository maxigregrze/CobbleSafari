package maxigregrze.cobblesafari.influence;

import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.power.GuaranteedShinyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class GuaranteedShinyInfluence implements SpawningInfluence {

    private final ServerPlayer player;

    public GuaranteedShinyInfluence(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void affectSpawn(@NotNull SpawnAction<?> action, @NotNull Entity entity) {
        if (entity instanceof PokemonEntity pe) {
            GuaranteedShinyManager.tryConsume(player, pe);
        }
    }
}
