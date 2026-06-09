package maxigregrze.cobblesafari.objectives;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A player's set of (always 3) rolled tasks for one dimension or dungeon instance (plan 118 §4.2).
 * The assignment key is {@code dimensionId} (non-instanced) or {@code dimensionId@instanceId}.
 */
public final class ObjectiveAssignment {

    public static final char INSTANCE_SEP = '@';

    private final String dimensionId;
    @Nullable
    private final UUID instanceId;
    private final List<ObjectiveTask> tasks;
    private boolean finalRewardGiven;

    public ObjectiveAssignment(String dimensionId, @Nullable UUID instanceId, List<ObjectiveTask> tasks) {
        this.dimensionId = dimensionId;
        this.instanceId = instanceId;
        this.tasks = new ArrayList<>(tasks);
    }

    public static String keyOf(String dimensionId, @Nullable UUID instanceId) {
        return instanceId == null ? dimensionId : dimensionId + INSTANCE_SEP + instanceId;
    }

    public String key() {
        return keyOf(dimensionId, instanceId);
    }

    public String dimensionId() {
        return dimensionId;
    }

    @Nullable
    public UUID instanceId() {
        return instanceId;
    }

    public List<ObjectiveTask> tasks() {
        return tasks;
    }

    public boolean isFinalRewardGiven() {
        return finalRewardGiven;
    }

    public void markFinalRewardGiven() {
        this.finalRewardGiven = true;
    }

    public boolean allComplete() {
        if (tasks.isEmpty()) {
            return false;
        }
        for (ObjectiveTask t : tasks) {
            if (!t.isComplete()) {
                return false;
            }
        }
        return true;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimensionId);
        if (instanceId != null) {
            tag.putUUID("Instance", instanceId);
        }
        tag.putBoolean("FinalRewardGiven", finalRewardGiven);
        ListTag list = new ListTag();
        for (ObjectiveTask task : tasks) {
            list.add(task.toNbt());
        }
        tag.put("Tasks", list);
        return tag;
    }

    public static ObjectiveAssignment fromNbt(CompoundTag tag) {
        String dimensionId = tag.getString("Dimension");
        UUID instanceId = tag.hasUUID("Instance") ? tag.getUUID("Instance") : null;
        List<ObjectiveTask> tasks = new ArrayList<>();
        ListTag list = tag.getList("Tasks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            tasks.add(ObjectiveTask.fromNbt(list.getCompound(i)));
        }
        ObjectiveAssignment a = new ObjectiveAssignment(dimensionId, instanceId, tasks);
        a.finalRewardGiven = tag.getBoolean("FinalRewardGiven");
        return a;
    }
}
