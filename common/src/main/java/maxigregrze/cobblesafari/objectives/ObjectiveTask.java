package maxigregrze.cobblesafari.objectives;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A single rolled objective task and its live progress.
 */
public final class ObjectiveTask {

    private final String taskId;
    private final int targetCount;
    private final String speciesId; // "" if unused
    private final int typeIndex; // -1 if unused, else 0..17
    private final String taskRewardTable; // "" if none

    private int progress;
    private boolean complete;
    private boolean taskRewardGiven;
    private final LinkedHashSet<String> seenSpecies = new LinkedHashSet<>(); // *_unique only

    // Guaranteed-spawn state of task_catch_shiny / _alpha / _alphashiny (NBT keys kept from the shiny-only era)
    private boolean spawnGuaranteePending;
    private long spawnGuaranteeTriggerGameTime;

    public ObjectiveTask(String taskId, int targetCount, String speciesId, int typeIndex,
                         String taskRewardTable) {
        this.taskId = taskId;
        this.targetCount = Math.max(1, targetCount);
        this.speciesId = speciesId == null ? "" : speciesId;
        this.typeIndex = typeIndex;
        this.taskRewardTable = taskRewardTable == null ? "" : taskRewardTable;
    }

    public String taskId() {
        return taskId;
    }

    public TaskType type() {
        return TaskType.byId(taskId);
    }

    public int targetCount() {
        return targetCount;
    }

    public String speciesId() {
        return speciesId;
    }

    public int typeIndex() {
        return typeIndex;
    }

    public String taskRewardTable() {
        return taskRewardTable;
    }

    public int progress() {
        return progress;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isTaskRewardGiven() {
        return taskRewardGiven;
    }

    public void markTaskRewardGiven() {
        this.taskRewardGiven = true;
    }

    public boolean isSpawnGuaranteePending() {
        return spawnGuaranteePending;
    }

    public long spawnGuaranteeTriggerGameTime() {
        return spawnGuaranteeTriggerGameTime;
    }

    public void armSpawnGuarantee(long triggerGameTime) {
        this.spawnGuaranteePending = true;
        this.spawnGuaranteeTriggerGameTime = triggerGameTime;
    }

    public void consumeSpawnGuarantee() {
        this.spawnGuaranteePending = false;
    }

    /**
     * Adds {@code amount} to the progress (clamped to the target). Returns {@code true} if this
     * call transitioned the task to complete.
     */
    public boolean addProgress(int amount) {
        if (complete) {
            return false;
        }
        progress = Math.min(targetCount, progress + amount);
        if (progress >= targetCount) {
            complete = true;
            return true;
        }
        return false;
    }

    /** Marks a binary task complete. Returns {@code true} on the completing transition. */
    public boolean markBinaryComplete() {
        return addProgress(targetCount);
    }

    /**
     * Records a distinct species for {@code *_unique} tasks. Returns {@code true} if the species
     * was new (and thus progressed the task) and the task became complete on this call.
     */
    public boolean recordUniqueSpecies(String species) {
        if (complete || species == null || species.isEmpty()) {
            return false;
        }
        if (seenSpecies.add(species)) {
            return addProgress(1);
        }
        return false;
    }

    public boolean hasSeenSpecies(String species) {
        return seenSpecies.contains(species);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("TaskId", taskId);
        tag.putInt("Target", targetCount);
        tag.putString("Species", speciesId);
        tag.putInt("TypeIndex", typeIndex);
        tag.putString("Reward", taskRewardTable);
        tag.putInt("Progress", progress);
        tag.putBoolean("Complete", complete);
        tag.putBoolean("RewardGiven", taskRewardGiven);
        tag.putBoolean("ShinyPending", spawnGuaranteePending);
        tag.putLong("ShinyTrigger", spawnGuaranteeTriggerGameTime);
        if (!seenSpecies.isEmpty()) {
            ListTag list = new ListTag();
            for (String s : seenSpecies) {
                list.add(StringTag.valueOf(s));
            }
            tag.put("Seen", list);
        }
        return tag;
    }

    public static ObjectiveTask fromNbt(CompoundTag tag) {
        ObjectiveTask task = new ObjectiveTask(
                tag.getString("TaskId"),
                tag.getInt("Target"),
                tag.getString("Species"),
                tag.contains("TypeIndex") ? tag.getInt("TypeIndex") : -1,
                tag.getString("Reward"));
        task.progress = tag.getInt("Progress");
        task.complete = tag.getBoolean("Complete");
        task.taskRewardGiven = tag.getBoolean("RewardGiven");
        task.spawnGuaranteePending = tag.getBoolean("ShinyPending");
        task.spawnGuaranteeTriggerGameTime = tag.getLong("ShinyTrigger");
        if (tag.contains("Seen", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Seen", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                task.seenSpecies.add(list.getString(i));
            }
        }
        return task;
    }
}
