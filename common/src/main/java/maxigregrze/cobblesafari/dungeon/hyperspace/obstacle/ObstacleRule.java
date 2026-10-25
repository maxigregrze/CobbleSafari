package maxigregrze.cobblesafari.dungeon.hyperspace.obstacle;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Which moves clear an obstacle (action plan 196, § 8.2): any non-status move of one of
 * {@code types}, or any move listed in {@code moves} whatever its type.
 *
 * @param types   Showdown type ids
 * @param moves   Showdown move ids
 * @param message lang key shown when the Pokémon knows no compatible move
 */
public record ObstacleRule(ObstacleKind kind, Set<String> types, Set<String> moves, String message,
                           @Nullable ResourceLocation lootTable) {

    public ObstacleRule {
        types = Set.copyOf(types);
        moves = Set.copyOf(moves);
    }

    public boolean accepts(String typeId, String moveId) {
        return types.contains(typeId) || moves.contains(moveId);
    }
}
