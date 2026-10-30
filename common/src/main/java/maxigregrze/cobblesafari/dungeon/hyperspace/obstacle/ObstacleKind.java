package maxigregrze.cobblesafari.dungeon.hyperspace.obstacle;

import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.objectives.TaskType;
import org.jetbrains.annotations.Nullable;

/**
 * The Z-A obstacles a Pokémon move clears (action plan 196, § 8.1), plus the shard cluster, a
 * resource node cleared the same way (§ 8.8): no task, and its own, longer respawn timer.
 */
public enum ObstacleKind {
    BOULDER("boulder", TaskType.TASK_DESTROY_BOULDER, false),
    BRAMBLES("brambles", TaskType.TASK_DESTROY_BRAMBLES, false),
    SLUDGE("sludge", TaskType.TASK_DESTROY_SLUDGE, false),
    SHARDCLUSTER("shardcluster", null, true);

    private final String id;
    @Nullable
    private final TaskType task;
    private final boolean resource;

    ObstacleKind(String id, @Nullable TaskType task, boolean resource) {
        this.id = id;
        this.task = task;
        this.resource = resource;
    }

    public String id() {
        return id;
    }

    /** The "destroy" task this obstacle feeds, or {@code null} when none does. */
    @Nullable
    public TaskType task() {
        return task;
    }

    /**
     * Seconds before a cleared obstacle of this kind comes back: {@code obstacleResourceRespawnSeconds}
     * for a resource node, {@code obstacleRespawnSeconds} otherwise.
     */
    public int respawnSeconds() {
        return resource ? MiscConfig.getObstacleResourceRespawnSeconds() : MiscConfig.getObstacleRespawnSeconds();
    }

    /** Registry path of the matching block, e.g. {@code obstacle_boulder}. */
    public String blockName() {
        return "obstacle_" + id;
    }
}
