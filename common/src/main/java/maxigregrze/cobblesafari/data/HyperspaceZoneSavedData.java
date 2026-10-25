package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.dungeon.hyperspace.HyperspaceZone;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The roll of every live {@code dungeon_paris} instance (action plan 196, § 3.6). */
public class HyperspaceZoneSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_hyperspace_zones";
    private static final String KEY_ZONES = "Zones";
    private static final String KEY_INSTANCE = "Instance";
    private static final String KEY_STAR = "Star";
    private static final String KEY_TYPES = "Types";
    private static final String KEY_SPECIES = "Species";

    private final Map<UUID, HyperspaceZone> zones = new HashMap<>();

    public HyperspaceZoneSavedData() {
        // Required by the SavedData factory; state is populated in load().
    }

    public static HyperspaceZoneSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HyperspaceZoneSavedData data = new HyperspaceZoneSavedData();
        ListTag list = tag.getList(KEY_ZONES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag zoneTag = list.getCompound(i);
            if (!zoneTag.hasUUID(KEY_INSTANCE)) {
                continue;
            }
            data.zones.put(zoneTag.getUUID(KEY_INSTANCE), new HyperspaceZone(
                    zoneTag.getInt(KEY_STAR),
                    readStrings(zoneTag.getList(KEY_TYPES, Tag.TAG_STRING)),
                    readStrings(zoneTag.getList(KEY_SPECIES, Tag.TAG_STRING))));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, HyperspaceZone> entry : zones.entrySet()) {
            CompoundTag zoneTag = new CompoundTag();
            zoneTag.putUUID(KEY_INSTANCE, entry.getKey());
            zoneTag.putInt(KEY_STAR, entry.getValue().star());
            zoneTag.put(KEY_TYPES, writeStrings(entry.getValue().types()));
            zoneTag.put(KEY_SPECIES, writeStrings(entry.getValue().targetSpecies()));
            list.add(zoneTag);
        }
        tag.put(KEY_ZONES, list);
        return tag;
    }

    public static HyperspaceZoneSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(HyperspaceZoneSavedData::new, HyperspaceZoneSavedData::load, null),
                DATA_NAME);
    }

    @Nullable
    public HyperspaceZone get(UUID instanceId) {
        return zones.get(instanceId);
    }

    public void put(UUID instanceId, HyperspaceZone zone) {
        zones.put(instanceId, zone);
        setDirty();
    }

    public void remove(UUID instanceId) {
        if (zones.remove(instanceId) != null) {
            setDirty();
        }
    }

    private static List<String> readStrings(ListTag list) {
        List<String> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    private static ListTag writeStrings(List<String> values) {
        ListTag list = new ListTag();
        for (String v : values) {
            list.add(StringTag.valueOf(v));
        }
        return list;
    }
}
