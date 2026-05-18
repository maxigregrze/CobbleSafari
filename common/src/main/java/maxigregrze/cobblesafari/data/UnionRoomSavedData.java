package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UnionRoomSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_union_room";

    private static final ResourceKey<Level> UNION_ROOM_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "unionroom"));

    private int nextInstanceId;
    private final List<InstanceData> instances = new ArrayList<>();
    private final Map<Integer, SessionData> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerOriginData> playerOrigins = new ConcurrentHashMap<>();
    private final Set<UUID> reconnectPending = ConcurrentHashMap.newKeySet();

    public UnionRoomSavedData() {
        this.nextInstanceId = 0;
    }

    public static class InstanceData {
        public int id;
        public BlockPos structurePos = BlockPos.ZERO;
        public BlockPos anchorPos = BlockPos.ZERO;
        public boolean occupied;

        CompoundTag toNbt() {
            CompoundTag t = new CompoundTag();
            t.putInt("Id", id);
            t.putIntArray("StructurePos", new int[]{structurePos.getX(), structurePos.getY(), structurePos.getZ()});
            t.putIntArray("AnchorPos", new int[]{anchorPos.getX(), anchorPos.getY(), anchorPos.getZ()});
            t.putBoolean("Occupied", occupied);
            return t;
        }

        static InstanceData fromNbt(CompoundTag t) {
            InstanceData d = new InstanceData();
            d.id = t.getInt("Id");
            int[] sp = t.getIntArray("StructurePos");
            int[] ap = t.getIntArray("AnchorPos");
            if (sp.length == 3) {
                d.structurePos = new BlockPos(sp[0], sp[1], sp[2]);
            }
            if (ap.length == 3) {
                d.anchorPos = new BlockPos(ap[0], ap[1], ap[2]);
            }
            d.occupied = t.getBoolean("Occupied");
            return d;
        }
    }

    public static class SessionData {
        public int instanceId;
        public UUID hostUUID;
        public final int[] code = new int[4];
        public final Set<UUID> guestUUIDs = new HashSet<>();

        CompoundTag toNbt() {
            CompoundTag t = new CompoundTag();
            t.putInt("InstanceId", instanceId);
            t.putUUID("Host", hostUUID);
            t.putIntArray("Code", code);
            ListTag guests = new ListTag();
            for (UUID g : guestUUIDs) {
                CompoundTag gt = new CompoundTag();
                gt.putUUID("Id", g);
                guests.add(gt);
            }
            t.put("Guests", guests);
            return t;
        }

        static SessionData fromNbt(CompoundTag t) {
            SessionData s = new SessionData();
            s.instanceId = t.getInt("InstanceId");
            s.hostUUID = t.getUUID("Host");
            int[] c = t.getIntArray("Code");
            for (int i = 0; i < 4 && i < c.length; i++) {
                s.code[i] = c[i];
            }
            if (t.contains("Guests", Tag.TAG_LIST)) {
                ListTag list = t.getList("Guests", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag gt = list.getCompound(i);
                    if (gt.hasUUID("Id")) {
                        s.guestUUIDs.add(gt.getUUID("Id"));
                    }
                }
            }
            return s;
        }
    }

    public static class PlayerOriginData {
        public BlockPos position = BlockPos.ZERO;
        public ResourceKey<Level> dimension = Level.OVERWORLD;

        CompoundTag toNbt(HolderLookup.Provider registries) {
            CompoundTag t = new CompoundTag();
            t.putIntArray("Pos", new int[]{position.getX(), position.getY(), position.getZ()});
            t.putString("Dimension", dimension.location().toString());
            return t;
        }

        static PlayerOriginData fromNbt(CompoundTag t) {
            PlayerOriginData d = new PlayerOriginData();
            int[] p = t.getIntArray("Pos");
            if (p.length == 3) {
                d.position = new BlockPos(p[0], p[1], p[2]);
            }
            if (t.contains("Dimension")) {
                ResourceLocation loc = ResourceLocation.parse(t.getString("Dimension"));
                d.dimension = ResourceKey.create(Registries.DIMENSION, loc);
            }
            return d;
        }
    }

    public static UnionRoomSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        UnionRoomSavedData data = new UnionRoomSavedData();
        data.nextInstanceId = tag.getInt("NextInstanceId");
        if (tag.contains("Instances", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Instances", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                data.instances.add(InstanceData.fromNbt(list.getCompound(i)));
            }
        }
        if (tag.contains("Sessions", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Sessions", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                SessionData s = SessionData.fromNbt(list.getCompound(i));
                data.activeSessions.put(s.instanceId, s);
            }
        }
        if (tag.contains("PlayerOrigins", Tag.TAG_COMPOUND)) {
            CompoundTag origins = tag.getCompound("PlayerOrigins");
            for (String key : origins.getAllKeys()) {
                try {
                    UUID id = UUID.fromString(key);
                    data.playerOrigins.put(id, PlayerOriginData.fromNbt(origins.getCompound(key)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (tag.contains("Reconnect", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Reconnect", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag rt = list.getCompound(i);
                if (rt.hasUUID("Id")) {
                    data.reconnectPending.add(rt.getUUID("Id"));
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("NextInstanceId", nextInstanceId);
        ListTag instList = new ListTag();
        for (InstanceData inst : instances) {
            instList.add(inst.toNbt());
        }
        tag.put("Instances", instList);
        ListTag sessList = new ListTag();
        for (SessionData s : activeSessions.values()) {
            sessList.add(s.toNbt());
        }
        tag.put("Sessions", sessList);
        CompoundTag origins = new CompoundTag();
        for (Map.Entry<UUID, PlayerOriginData> e : playerOrigins.entrySet()) {
            origins.put(e.getKey().toString(), e.getValue().toNbt(registries));
        }
        tag.put("PlayerOrigins", origins);
        ListTag reconnect = new ListTag();
        for (UUID id : reconnectPending) {
            CompoundTag rt = new CompoundTag();
            rt.putUUID("Id", id);
            reconnect.add(rt);
        }
        tag.put("Reconnect", reconnect);
        return tag;
    }

    public static UnionRoomSavedData get(MinecraftServer server) {
        ServerLevel unionLevel = server.getLevel(UNION_ROOM_KEY);
        if (unionLevel == null) {
            return null;
        }
        DimensionDataStorage storage = unionLevel.getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(UnionRoomSavedData::new, UnionRoomSavedData::load, null),
                DATA_NAME);
    }

    public int allocateNextInstanceId() {
        int id = nextInstanceId;
        nextInstanceId++;
        setDirty();
        return id;
    }

    public List<InstanceData> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    public int getInstanceCount() {
        return instances.size();
    }

    public void addInstance(InstanceData instance) {
        instances.add(instance);
        setDirty();
    }

    public Optional<InstanceData> getInstance(int id) {
        for (InstanceData i : instances) {
            if (i.id == id) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public List<SessionData> getAllSessions() {
        return new ArrayList<>(activeSessions.values());
    }

    public void addSession(SessionData session) {
        activeSessions.put(session.instanceId, session);
        setDirty();
    }

    public void removeSession(int instanceId) {
        activeSessions.remove(instanceId);
        setDirty();
    }

    public Optional<SessionData> getSessionByInstanceId(int instanceId) {
        return Optional.ofNullable(activeSessions.get(instanceId));
    }

    public Optional<SessionData> findSessionByHost(UUID host) {
        for (SessionData s : activeSessions.values()) {
            if (host.equals(s.hostUUID)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public Optional<SessionData> findSessionByCode(int[] code) {
        if (code == null || code.length != 4) {
            return Optional.empty();
        }
        outer:
        for (SessionData s : activeSessions.values()) {
            for (int i = 0; i < 4; i++) {
                if (s.code[i] != code[i]) {
                    continue outer;
                }
            }
            return Optional.of(s);
        }
        return Optional.empty();
    }

    public Optional<SessionData> findSessionContainingGuest(UUID guest) {
        for (SessionData s : activeSessions.values()) {
            if (s.guestUUIDs.contains(guest)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public void removeGuestFromAllSessions(UUID guestId) {
        boolean changed = false;
        for (SessionData s : activeSessions.values()) {
            if (s.guestUUIDs.remove(guestId)) {
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public void savePlayerOrigin(UUID playerId, BlockPos pos, ResourceKey<Level> dimension) {
        PlayerOriginData d = new PlayerOriginData();
        d.position = pos;
        d.dimension = dimension;
        playerOrigins.put(playerId, d);
        setDirty();
    }

    public Optional<PlayerOriginData> getPlayerOrigin(UUID playerId) {
        return Optional.ofNullable(playerOrigins.get(playerId));
    }

    public void removePlayerOrigin(UUID playerId) {
        if (playerOrigins.remove(playerId) != null) {
            setDirty();
        }
    }

    public void markReconnectTeleport(UUID playerId) {
        if (reconnectPending.add(playerId)) {
            setDirty();
        }
    }

    public boolean isMarkedForReconnectTeleport(UUID playerId) {
        return reconnectPending.contains(playerId);
    }

    public void clearReconnectMark(UUID playerId) {
        if (reconnectPending.remove(playerId)) {
            setDirty();
        }
    }
}
