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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The overworld maps shown in the sky of every live {@code dungeon_paris} instance, and the world
 * spawn map they fall back on (action plan 197, section 5.3).
 */
public class HyperspaceSkyMapSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_hyperspace_skymaps";
    private static final String KEY_INSTANCES = "Instances";
    private static final String KEY_INSTANCE = "InstanceId";
    private static final String KEY_SOURCE = "Source";
    private static final String KEY_COLORS = "Colors";
    private static final String KEY_SPAWN = "Spawn";

    /** Where an instance's map comes from. */
    public enum Source {
        /** Sampled around the instance's own portal. */
        PORTAL,
        /** The world spawn map, or the fixed image while there is none. */
        SPAWN
    }

    /** {@code colors} is set for {@link Source#PORTAL} only. */
    public record Entry(Source source, @Nullable byte[] colors) {}

    private final Map<UUID, Entry> instances = new HashMap<>();
    private byte[] spawnColors;

    public HyperspaceSkyMapSavedData() {
        // Required by the SavedData factory; state is populated in load().
    }

    public static HyperspaceSkyMapSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HyperspaceSkyMapSavedData data = new HyperspaceSkyMapSavedData();
        ListTag list = tag.getList(KEY_INSTANCES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            if (!entryTag.hasUUID(KEY_INSTANCE)) {
                continue;
            }
            Source source = Source.SPAWN;
            byte[] colors = null;
            if (Source.PORTAL.name().equals(entryTag.getString(KEY_SOURCE)) && entryTag.contains(KEY_COLORS, Tag.TAG_BYTE_ARRAY)) {
                source = Source.PORTAL;
                colors = entryTag.getByteArray(KEY_COLORS);
            }
            data.instances.put(entryTag.getUUID(KEY_INSTANCE), new Entry(source, colors));
        }
        if (tag.contains(KEY_SPAWN, Tag.TAG_BYTE_ARRAY)) {
            data.spawnColors = tag.getByteArray(KEY_SPAWN);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Entry> entry : instances.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(KEY_INSTANCE, entry.getKey());
            entryTag.putString(KEY_SOURCE, entry.getValue().source().name());
            if (entry.getValue().colors() != null) {
                entryTag.putByteArray(KEY_COLORS, entry.getValue().colors());
            }
            list.add(entryTag);
        }
        tag.put(KEY_INSTANCES, list);
        if (spawnColors != null) {
            tag.putByteArray(KEY_SPAWN, spawnColors);
        }
        return tag;
    }

    public static HyperspaceSkyMapSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(HyperspaceSkyMapSavedData::new, HyperspaceSkyMapSavedData::load, null),
                DATA_NAME);
    }

    @Nullable
    public Entry get(UUID instanceId) {
        return instances.get(instanceId);
    }

    public void put(UUID instanceId, Entry entry) {
        instances.put(instanceId, entry);
        setDirty();
    }

    public void remove(UUID instanceId) {
        if (instances.remove(instanceId) != null) {
            setDirty();
        }
    }

    @Nullable
    public byte[] getSpawnColors() {
        return spawnColors;
    }

    public void setSpawnColors(byte[] colors) {
        spawnColors = colors;
        setDirty();
    }
}
