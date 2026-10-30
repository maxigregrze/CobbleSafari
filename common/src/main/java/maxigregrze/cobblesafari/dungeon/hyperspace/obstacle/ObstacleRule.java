package maxigregrze.cobblesafari.dungeon.hyperspace.obstacle;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Which moves clear an obstacle (action plan 196, § 8.2): any non-status move of one of
 * {@code types}, or any move listed in {@code moves} whatever its type - or, with {@code anyMove},
 * every non-status move (§ 8.8).
 *
 * @param types   Showdown type ids
 * @param moves   Showdown move ids
 * @param anyMove accept every physical or special move
 * @param message lang key shown when the Pokémon knows no compatible move
 */
public record ObstacleRule(ObstacleKind kind, Set<String> types, Set<String> moves, boolean anyMove,
                           String message, @Nullable ResourceLocation lootTable) {

    public ObstacleRule {
        types = Set.copyOf(types);
        moves = Set.copyOf(moves);
    }

    public boolean accepts(String typeId, String moveId) {
        return anyMove || types.contains(typeId) || moves.contains(moveId);
    }
}
