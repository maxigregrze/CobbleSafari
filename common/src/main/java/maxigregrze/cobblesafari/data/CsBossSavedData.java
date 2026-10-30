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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Snapshots of in-progress boss fights, to cancel any fight on reboot.
 * Only the minimum needed for cleanup is persisted.
 */
public class CsBossSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_csboss";
    private static final String KEY_NEXT_ID = "NextSessionId";
    private static final String KEY_SNAPSHOTS = "Snapshots";
    private static final String KEY_PENDING_RETURNS = "PendingReturns";

    private int nextSessionId = 1;
    private final List<Snapshot> snapshots = new ArrayList<>();
    /**
     * Participants absents (hors ligne) quand l'arène a disparu, ou fight coupé par un crash :
     * replacés à leur position de départ à leur reconnexion s'ils sont dans cette dimension (plan 199 § 7.4).
     */
    private final Map<UUID, PendingReturn> pendingReturns = new HashMap<>();

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
        if (tag.contains(KEY_PENDING_RETURNS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(KEY_PENDING_RETURNS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                try {
                    ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION,
                            ResourceLocation.parse(c.getString("Dim")));
                    data.pendingReturns.put(c.getUUID("U"),
                            new PendingReturn(dim, new Vec3(c.getDouble("X"), c.getDouble("Y"), c.getDouble("Z"))));
                } catch (Exception e) {
                    CobbleSafari.LOGGER.warn("[CSBoss] ignoring malformed pending return", e);
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
        ListTag returns = new ListTag();
        for (Map.Entry<UUID, PendingReturn> e : pendingReturns.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("U", e.getKey());
            c.putString("Dim", e.getValue().dimension().location().toString());
            c.putDouble("X", e.getValue().pos().x);
            c.putDouble("Y", e.getValue().pos().y);
            c.putDouble("Z", e.getValue().pos().z);
            returns.add(c);
        }
        tag.put(KEY_PENDING_RETURNS, returns);
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

    public void putPendingReturn(UUID player, ResourceKey<Level> dimension, Vec3 pos) {
        pendingReturns.put(player, new PendingReturn(dimension, pos));
        setDirty();
    }

    /** Retire et renvoie le retour en attente du joueur, ou {@code null}. */
    public PendingReturn takePendingReturn(UUID player) {
        PendingReturn r = pendingReturns.remove(player);
        if (r != null) {
            setDirty();
        }
        return r;
    }

    public record PendingReturn(ResourceKey<Level> dimension, Vec3 pos) {}

    /**
     * Minimal data to clean up a fight on reboot.
     */
    public record Snapshot(
            int sessionId,
            ResourceKey<Level> dimension,
            BlockPos triggerPos,
            UUID bossUuid,
            List<UUID> participants,
            List<BlockPos> changedBlocks,
            /** Position de départ de chaque participant (plan 199 § 5.3) ; vide pour un ancien snapshot. */
            Map<UUID, Vec3> startPositions
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
                Vec3 start = startPositions.get(u);
                if (start != null) {
                    c.putDouble("X", start.x);
                    c.putDouble("Y", start.y);
                    c.putDouble("Z", start.z);
                }
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
                Map<UUID, Vec3> starts = new HashMap<>();
                ListTag plist = tag.getList(KEY_PARTICIPANTS, Tag.TAG_COMPOUND);
                for (int i = 0; i < plist.size(); i++) {
                    CompoundTag c = plist.getCompound(i);
                    UUID u = c.getUUID("U");
                    participants.add(u);
                    if (c.contains("X")) {
                        starts.put(u, new Vec3(c.getDouble("X"), c.getDouble("Y"), c.getDouble("Z")));
                    }
                }
                List<BlockPos> blocks = new ArrayList<>();
                ListTag blist = tag.getList(KEY_BLOCKS, Tag.TAG_INT_ARRAY);
                for (int i = 0; i < blist.size(); i++) {
                    int[] arr = blist.getIntArray(i);
                    if (arr.length == 3) {
                        blocks.add(new BlockPos(arr[0], arr[1], arr[2]));
                    }
                }
                return new Snapshot(id, dim, trigger, boss, participants, blocks, starts);
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[CSBoss] failed to read session snapshot", e);
                return null;
            }
        }
    }
}
