package maxigregrze.cobblesafari.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DungeonInstance {

    private final UUID instanceId;
    private final String dungeonId;
    private final ResourceKey<Level> dimension;
    private final BlockPos structurePos;
    private final BlockPos exitPortalPos;
    private final long creationTime;
    private final Set<UUID> players;

    public DungeonInstance(String dungeonId, ResourceKey<Level> dimension, BlockPos structurePos, BlockPos exitPortalPos) {
        this.instanceId = UUID.randomUUID();
        this.dungeonId = dungeonId;
        this.dimension = dimension;
        this.structurePos = structurePos;
        this.exitPortalPos = exitPortalPos;
        this.creationTime = System.currentTimeMillis();
        this.players = new HashSet<>();
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getStructurePos() {
        return structurePos;
    }

    public BlockPos getExitPortalPos() {
        return exitPortalPos;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public void addPlayer(UUID playerId) {
        players.add(playerId);
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }

    public boolean hasPlayers() {
        return !players.isEmpty();
    }
}
