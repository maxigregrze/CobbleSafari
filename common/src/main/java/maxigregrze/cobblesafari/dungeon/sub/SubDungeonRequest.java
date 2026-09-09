package maxigregrze.cobblesafari.dungeon.sub;

import maxigregrze.cobblesafari.data.TimerScope;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Everything a caller supplies to open a sub-dungeon. This record <b>is</b> the generic boundary of
 * the mechanism (action plan 183, Decision section 1): nothing in {@code dungeon/sub/} knows about
 * the mini-game or about Ultra Space, and a future second consumer only has to fill one of these.
 *
 * @param playerId        who is going in - the sole owner of the instance that results
 * @param parentScope     the visit this one nests inside, or {@code null}
 * @param parentInstanceId the parent instance's id, used for the orphan sweep and the fallback
 * @param devicePos       where to put them back: the wormhole block
 * @param parentDimension the dimension that block stands in
 * @param structurePool   what to generate, taken from the destination file - never from the config
 * @param dungeonId       which registered sub-dungeon hosts it, taken from the destination file
 * @param biome           biome to stamp on the footprint, or {@code null} - ignored when the
 *                        destination owns its dimension
 * @param typeId          the ride's {@code type}: interpreted, it chose {@code structurePool}
 * @param tier            the ride's {@code tier}: recorded, never interpreted
 * @param distance        the ride's {@code distance}: recorded, never interpreted
 */
public record SubDungeonRequest(
        UUID playerId,
        TimerScope parentScope,
        UUID parentInstanceId,
        BlockPos devicePos,
        ResourceKey<Level> parentDimension,
        String structurePool,
        String dungeonId,
        String biome,
        int typeId,
        int tier,
        int distance
) {
    /** @return true when this visit is nested in a dungeon run whose credit it will debit. */
    public boolean hasParent() {
        return parentScope != null;
    }
}
