package maxigregrze.cobblesafari.dungeon.hyperspace.obstacle;

import maxigregrze.cobblesafari.objectives.TaskType;

/** The three Z-A obstacles a Pokémon move clears (action plan 196, § 8.1). */
public enum ObstacleKind {
    BOULDER("boulder", TaskType.TASK_DESTROY_BOULDER),
    BRAMBLES("brambles", TaskType.TASK_DESTROY_BRAMBLES),
    SLUDGE("sludge", TaskType.TASK_DESTROY_SLUDGE);

    private final String id;
    private final TaskType task;

    ObstacleKind(String id, TaskType task) {
        this.id = id;
        this.task = task;
    }

    public String id() {
        return id;
    }

    public TaskType task() {
        return task;
    }

    /** Registry path of the matching block, e.g. {@code obstacle_boulder}. */
    public String blockName() {
        return "obstacle_" + id;
    }
}
