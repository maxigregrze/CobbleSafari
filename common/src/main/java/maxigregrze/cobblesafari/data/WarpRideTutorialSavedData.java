package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Players who have read the Ultra Warp Ride tutorial to the end (action plan 200, section 5.5).
 * Server-side, so the slideshow is shown once per player and per world, whatever client they use.
 */
public class WarpRideTutorialSavedData extends SavedData {

    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_warp_ride_tutorial";
    private static final String KEY_SEEN = "Seen";

    private final Set<UUID> seen = new HashSet<>();

    public WarpRideTutorialSavedData() {
        // Required by the SavedData factory; state is populated in load().
    }

    public static WarpRideTutorialSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WarpRideTutorialSavedData data = new WarpRideTutorialSavedData();
        ListTag list = tag.getList(KEY_SEEN, Tag.TAG_INT_ARRAY);
        for (Tag entry : list) {
            data.seen.add(NbtUtils.loadUUID(entry));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (UUID id : seen) {
            list.add(NbtUtils.createUUID(id));
        }
        tag.put(KEY_SEEN, list);
        return tag;
    }

    public static WarpRideTutorialSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(WarpRideTutorialSavedData::new, WarpRideTutorialSavedData::load, null),
                DATA_NAME);
    }

    public boolean hasSeen(UUID player) {
        return seen.contains(player);
    }

    public void markSeen(UUID player) {
        if (seen.add(player)) {
            setDirty();
        }
    }

    /** @return true if the player had seen it, i.e. something changed */
    public boolean reset(UUID player) {
        boolean removed = seen.remove(player);
        if (removed) {
            setDirty();
        }
        return removed;
    }
}
