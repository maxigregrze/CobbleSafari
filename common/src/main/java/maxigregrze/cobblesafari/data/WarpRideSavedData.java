package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The persistent half of the rider-body protection. {@code Invulnerable} and {@code NoGravity} live
 * in the player's own NBT, so a session ending abnormally would leave a player permanently
 * invulnerable across restarts; this store holds the flags they had before the ride, for login to
 * restore. A {@link SavedData} rather than per-player NBT: loader-neutral, and written by the same
 * autosave that would have persisted the dangling flag.
 */
public class WarpRideSavedData extends SavedData {

    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_warp_ride_bodies";
    private static final String KEY_RIDERS = "Riders";
    private static final String KEY_UUID = "Uuid";
    private static final String KEY_PRIOR_INVULNERABLE = "PriorInvulnerable";
    private static final String KEY_PRIOR_NO_GRAVITY = "PriorNoGravity";

    /** The flags a player had before the ride started. */
    public record PriorBody(boolean invulnerable, boolean noGravity) {}

    private final Map<UUID, PriorBody> riding = new HashMap<>();

    public WarpRideSavedData() {
        // Required by the SavedData factory; state is populated in load().
    }

    public static WarpRideSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WarpRideSavedData data = new WarpRideSavedData();
        if (tag.contains(KEY_RIDERS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(KEY_RIDERS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (!entry.hasUUID(KEY_UUID)) {
                    continue;
                }
                data.riding.put(entry.getUUID(KEY_UUID), new PriorBody(
                        entry.getBoolean(KEY_PRIOR_INVULNERABLE),
                        entry.getBoolean(KEY_PRIOR_NO_GRAVITY)));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PriorBody> entry : riding.entrySet()) {
            CompoundTag riderTag = new CompoundTag();
            riderTag.putUUID(KEY_UUID, entry.getKey());
            riderTag.putBoolean(KEY_PRIOR_INVULNERABLE, entry.getValue().invulnerable());
            riderTag.putBoolean(KEY_PRIOR_NO_GRAVITY, entry.getValue().noGravity());
            list.add(riderTag);
        }
        tag.put(KEY_RIDERS, list);
        return tag;
    }

    public static WarpRideSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(WarpRideSavedData::new, WarpRideSavedData::load, null),
                DATA_NAME);
    }

    public void markRiding(UUID player, boolean priorInvulnerable, boolean priorNoGravity) {
        riding.put(player, new PriorBody(priorInvulnerable, priorNoGravity));
        setDirty();
    }

    /** @return the prior flags, or null if this player was not marked */
    public PriorBody consume(UUID player) {
        PriorBody prior = riding.remove(player);
        if (prior != null) {
            setDirty();
        }
        return prior;
    }

    public boolean isMarked(UUID player) {
        return riding.containsKey(player);
    }
}
