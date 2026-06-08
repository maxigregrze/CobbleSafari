package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Snapshots of in-progress boss fights (plan 100 § 7), to cancel any fight on reboot.
 * Only the minimum needed for cleanup is persisted.
 */
public class CsBossSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_csboss";
    private static final String KEY_NEXT_ID = "NextSessionId";
    private static final String KEY_SNAPSHOTS = "Snapshots";

    private int nextSessionId = 1;
    private final List<Snapshot> snapshots = new ArrayList<>();

    public CsBossSavedData() {
        // state populated in load()
    }

    public static CsBossSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CsBossSavedData data = new CsBossSavedData();
        if (tag.contains(KEY_NEXT_ID)) {
            data.nextSessionId = Math.max(1, tag.getInt(KEY_NEXT_ID));
        }
        if (tag.contains(KEY_SNAPSHOTS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(KEY_SNAPSHOTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                Snapshot s = Snapshot.fromNbt(list.getCompound(i));
                if (s != null) {
                    data.snapshots.add(s);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(KEY_NEXT_ID, nextSessionId);
        ListTag list = new ListTag();
        for (Snapshot s : snapshots) {
            list.add(s.toNbt());
        }
        tag.put(KEY_SNAPSHOTS, list);
        return tag;
    }

    public static CsBossSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(new Factory<>(CsBossSavedData::new, CsBossSavedData::load, null), DATA_NAME);
    }

    public int allocateSessionId() {
        int id = nextSessionId++;
        setDirty();
        return id;
    }

    public void putSnapshot(Snapshot snapshot) {
        snapshots.removeIf(s -> s.sessionId() == snapshot.sessionId());
        snapshots.add(snapshot);
        setDirty();
    }

    public void removeSnapshot(int sessionId) {
        if (snapshots.removeIf(s -> s.sessionId() == sessionId)) {
            setDirty();
        }
    }

    public List<Snapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }

    public void clearSnapshots() {
        if (!snapshots.isEmpty()) {
            snapshots.clear();
            setDirty();
        }
    }

    /**
     * Minimal data to clean up a fight on reboot.
     */
    public record Snapshot(
            int sessionId,
            ResourceKey<Level> dimension,
            BlockPos triggerPos,
            UUID bossUuid,
            List<UUID> participants,
            List<BlockPos> changedBlocks
    ) {
        private static final String KEY_ID = "Id";
        private static final String KEY_DIM = "Dim";
        private static final String KEY_TRIGGER = "Trigger";
        private static final String KEY_BOSS = "Boss";
        private static final String KEY_PARTICIPANTS = "Participants";
        private static final String KEY_BLOCKS = "Blocks";

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putInt(KEY_ID, sessionId);
            tag.putString(KEY_DIM, dimension.location().toString());
            tag.put(KEY_TRIGGER, NbtUtils.writeBlockPos(triggerPos));
            tag.putUUID(KEY_BOSS, bossUuid);
            ListTag plist = new ListTag();
            for (UUID u : participants) {
                CompoundTag c = new CompoundTag();
                c.putUUID("U", u);
                plist.add(c);
            }
            tag.put(KEY_PARTICIPANTS, plist);
            ListTag blist = new ListTag();
            for (BlockPos p : changedBlocks) {
                blist.add(NbtUtils.writeBlockPos(p));
            }
            tag.put(KEY_BLOCKS, blist);
            return tag;
        }

        public static Snapshot fromNbt(CompoundTag tag) {
            try {
                int id = tag.getInt(KEY_ID);
                ResourceLocation dimLoc = ResourceLocation.parse(tag.getString(KEY_DIM));
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimLoc);
                BlockPos trigger = NbtUtils.readBlockPos(tag, KEY_TRIGGER).orElseThrow();
                UUID boss = tag.getUUID(KEY_BOSS);
                List<UUID> participants = new ArrayList<>();
                ListTag plist = tag.getList(KEY_PARTICIPANTS, Tag.TAG_COMPOUND);
                for (int i = 0; i < plist.size(); i++) {
                    participants.add(plist.getCompound(i).getUUID("U"));
                }
                List<BlockPos> blocks = new ArrayList<>();
                ListTag blist = tag.getList(KEY_BLOCKS, Tag.TAG_INT_ARRAY);
                for (int i = 0; i < blist.size(); i++) {
                    int[] arr = blist.getIntArray(i);
                    if (arr.length == 3) {
                        blocks.add(new BlockPos(arr[0], arr[1], arr[2]));
                    }
                }
                return new Snapshot(id, dim, trigger, boss, participants, blocks);
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[CSBoss] failed to read session snapshot", e);
                return null;
            }
        }
    }
}
