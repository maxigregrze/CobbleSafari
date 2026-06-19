package maxigregrze.cobblesafari.objectives;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Parsed datapack definition of the objective pool for one dimension.
 * Immutable; produced by {@link DimensionalObjectivesDataLoader}.
 */
public final class DimensionalObjectivesDefinition {

    private final ResourceLocation dimensionId;
    private final boolean instanciated;
    private final boolean enableFinalCompletionAuspiciousReward;
    private final String auspiciousPokeballRewardDisplayName;
    @Nullable
    private final ResourceLocation fallbackCompletionReward;
    private final boolean enableFinalCompletionReward;
    @Nullable
    private final ResourceLocation finalCompletionReward;
    private final List<TaskPoolEntry> tasks;

    public DimensionalObjectivesDefinition(
            ResourceLocation dimensionId,
            boolean instanciated,
            boolean enableFinalCompletionAuspiciousReward,
            String auspiciousPokeballRewardDisplayName,
            @Nullable ResourceLocation fallbackCompletionReward,
            boolean enableFinalCompletionReward,
            @Nullable ResourceLocation finalCompletionReward,
            List<TaskPoolEntry> tasks) {
        this.dimensionId = dimensionId;
        this.instanciated = instanciated;
        this.enableFinalCompletionAuspiciousReward = enableFinalCompletionAuspiciousReward;
        this.auspiciousPokeballRewardDisplayName = auspiciousPokeballRewardDisplayName;
        this.fallbackCompletionReward = fallbackCompletionReward;
        this.enableFinalCompletionReward = enableFinalCompletionReward;
        this.finalCompletionReward = finalCompletionReward;
        this.tasks = List.copyOf(tasks);
    }

    public ResourceLocation dimensionId() {
        return dimensionId;
    }

    public boolean isInstanciated() {
        return instanciated;
    }

    public boolean enableFinalCompletionAuspiciousReward() {
        return enableFinalCompletionAuspiciousReward;
    }

    public String auspiciousPokeballRewardDisplayName() {
        return auspiciousPokeballRewardDisplayName;
    }

    @Nullable
    public ResourceLocation fallbackCompletionReward() {
        return fallbackCompletionReward;
    }

    public boolean enableFinalCompletionReward() {
        return enableFinalCompletionReward;
    }

    @Nullable
    public ResourceLocation finalCompletionReward() {
        return finalCompletionReward;
    }

    public List<TaskPoolEntry> tasks() {
        return tasks;
    }

    /**
     * One rollable entry in the pool: a task type with its weight and the bounds used to roll
     * its variables.
     */
    public static final class TaskPoolEntry {
        private final TaskType type;
        private final int weight;
        private final int countMin;
        private final int countMax;
        private final List<String> allowedSpecies; // may contain the "random" sentinel
        private final List<Integer> allowedTypes; // variant indices 0..17
        @Nullable
        private final ResourceLocation taskReward;

        public TaskPoolEntry(TaskType type, int weight, int countMin, int countMax,
                             List<String> allowedSpecies, List<Integer> allowedTypes,
                             @Nullable ResourceLocation taskReward) {
            this.type = type;
            this.weight = weight;
            this.countMin = countMin;
            this.countMax = countMax;
            this.allowedSpecies = List.copyOf(allowedSpecies);
            this.allowedTypes = List.copyOf(allowedTypes);
            this.taskReward = taskReward;
        }

        public TaskType type() {
            return type;
        }

        public int weight() {
            return weight;
        }

        public int countMin() {
            return countMin;
        }

        public int countMax() {
            return countMax;
        }

        public List<String> allowedSpecies() {
            return allowedSpecies;
        }

        public List<Integer> allowedTypes() {
            return allowedTypes;
        }

        @Nullable
        public ResourceLocation taskReward() {
            return taskReward;
        }
    }
}
