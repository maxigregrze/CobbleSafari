package maxigregrze.cobblesafari.unionroom;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.block.misc.UnionRoomExitTeleporterBlockEntity;
import maxigregrze.cobblesafari.network.UnionAppResultPayload;
import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.config.MiscConfig;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.data.PlayerTimerData;
import maxigregrze.cobblesafari.data.UnionRoomSavedData;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.manager.TimerManager;
import maxigregrze.cobblesafari.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UnionRoomManager {

    public enum CreateResult {
        OK,
        ALREADY_IN_SESSION,
        MAX_INSTANCES,
        BANNED_DIMENSION,
        DIMENSION_NOT_FOUND,
        CREATION_FAILED
    }

    public enum JoinResult {
        OK,
        INVALID_CODE,
        OWN_SESSION,
        ALREADY_JOINED,
        ALREADY_IN_SESSION,
        BANNED_DIMENSION,
        DIMENSION_NOT_FOUND,
        HOST_UNAVAILABLE,
        SESSION_FULL,
        FAILED
    }

    public static final ResourceKey<Level> UNION_ROOM_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "unionroom"));
    public static final String DIMENSION_ID = "cobblesafari:unionroom";

    private static final int INSTANCE_SPACING = 128;
    private static final int INSTANCE_Y = 64;
    private static final int CODE_LENGTH = 4;
    private static final int CODE_MIN = 1;
    private static final int CODE_MAX = 6;
    private static final int TIMER_SECONDS = 3600;
    private static final int JIGSAW_DEPTH = 5;
    private static final String JIGSAW_POOL = "cobblesafari:unionroom/start";
    private static final int EVAC_RADIUS = 64;
    private static final String MSG_VOLUNTARY_EXIT = "cobblesafari.unionroom.session_closed.exit";
    private static final String MSG_SESSION_GONE = "cobblesafari.unionroom.session_closed.session_gone";
    private static final String MSG_SESSION_TIMER = "cobblesafari.unionroom.session_closed.timer";
    /** Attempts to find the void marker + place the exit teleporter after jigsaw generation completes. */
    private static final ConcurrentHashMap<Integer, int[]> PENDING_EXIT_SETUPS = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private static final ThreadLocal<Boolean> BULK_UNION_EVAC = ThreadLocal.withInitial(() -> false);
    private static int hostCheckTick = 0;

    private UnionRoomManager() {}

    public static boolean isInUnionRoom(ServerPlayer player) {
        return player.level().dimension().equals(UNION_ROOM_DIMENSION);
    }

    public static void onBeforeUnionRoomTimerExpire(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            return;
        }
        if (Boolean.TRUE.equals(BULK_UNION_EVAC.get())) {
            data.findSessionContainingGuest(player.getUUID()).ifPresent(s -> {
                s.guestUUIDs.remove(player.getUUID());
                data.setDirty();
            });
            return;
        }
        Optional<UnionRoomSavedData.SessionData> asHost = data.findSessionByHost(player.getUUID());
        if (asHost.isPresent()) {
            closeSession(server, data, asHost.get().instanceId,
                    MSG_SESSION_TIMER,
                    player.getUUID(), false, null);
            return;
        }
        data.findSessionContainingGuest(player.getUUID()).ifPresent(s -> {
            s.guestUUIDs.remove(player.getUUID());
            data.setDirty();
        });
    }

    public static void onAfterUnionRoomTimerExpire(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data != null) {
            data.removePlayerOrigin(player.getUUID());
        }
    }

    public static CreateResult createSession(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return CreateResult.CREATION_FAILED;
        }
        if (!canPlayerEnterUnionRoom(player)) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.banned_dimension"));
            return CreateResult.BANNED_DIMENSION;
        }
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.dimension_not_found"));
            return CreateResult.DIMENSION_NOT_FOUND;
        }

        Optional<UnionRoomSavedData.SessionData> existing = findPlayerSession(data, player.getUUID());
        if (existing.isPresent()) {
            Optional<UnionRoomSavedData.InstanceData> existInst = data.getInstance(existing.get().instanceId);
            ServerLevel existLevel = server.getLevel(UNION_ROOM_DIMENSION);
            if (existInst.isPresent() && existLevel != null) {
                teleportToInstance(player, existLevel, existInst.get());
            }
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.already_in_session"));
            return CreateResult.ALREADY_IN_SESSION;
        }

        ServerLevel unionLevel = server.getLevel(UNION_ROOM_DIMENSION);
        if (unionLevel == null) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.dimension_not_found"));
            return CreateResult.DIMENSION_NOT_FOUND;
        }

        MiscConfig.RoomTypeConfig roomType = MiscConfig.getRoomType("default");

        synchronized (UnionRoomManager.class) {
            Optional<UnionRoomSavedData.InstanceData> vacant = findVacantInstance(data);
            UnionRoomSavedData.InstanceData instance;
            if (vacant.isPresent()) {
                instance = vacant.get();
            } else {
                if (data.getInstanceCount() >= roomType.maxInstances) {
                    player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.max_instances"));
                    return CreateResult.MAX_INSTANCES;
                }
                instance = createNewInstance(server, data, unionLevel);
                if (instance == null) {
                    player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.creation_failed"));
                    return CreateResult.CREATION_FAILED;
                }
            }

            int[] code = generateUniqueCode(data);
            UnionRoomSavedData.SessionData session = new UnionRoomSavedData.SessionData();
            session.instanceId = instance.id;
            session.hostUUID = player.getUUID();
            session.roomType = "default";
            System.arraycopy(code, 0, session.code, 0, CODE_LENGTH);
            data.addSession(session);
            instance.occupied = true;

            data.savePlayerOrigin(player.getUUID(), player.blockPosition(), player.level().dimension());
            PlayerTimerData timerData = TimerManager.getOrCreateData(player, DIMENSION_ID);
            timerData.setRemainingTicks(TIMER_SECONDS * 20);
            timerData.setLastResetTimestamp(System.currentTimeMillis());
            TimerManager.setPlayerOrigin(player, DIMENSION_ID, player.blockPosition(), player.level().dimension());

            teleportToInstance(player, unionLevel, instance);
            TimerManager.startTimer(player, DIMENSION_ID);

            sendCodeMessage(player, code);
            data.setDirty();

            CobbleSafari.LOGGER.info(
                    "Union Room session created: instanceId={} host={} ({}) roomCode={}-{}-{}-{}",
                    instance.id, player.getName().getString(), player.getUUID(),
                    code[0], code[1], code[2], code[3]);
        }
        int created = maxigregrze.cobblesafari.init.ModStats.awardAndGet(
                player, maxigregrze.cobblesafari.init.ModStats.UNION_ROOMS_CREATED);
        maxigregrze.cobblesafari.advancement.ModCriteria.UNION_CREATED.trigger(player, created);
        return CreateResult.OK;
    }

    public static JoinResult joinSession(ServerPlayer player, int[] code) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return JoinResult.FAILED;
        }
        if (!canPlayerEnterUnionRoom(player)) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.banned_dimension"));
            return JoinResult.BANNED_DIMENSION;
        }
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.dimension_not_found"));
            return JoinResult.DIMENSION_NOT_FOUND;
        }

        Optional<UnionRoomSavedData.SessionData> existing = findPlayerSession(data, player.getUUID());
        if (existing.isPresent()) {
            Optional<UnionRoomSavedData.InstanceData> existInst = data.getInstance(existing.get().instanceId);
            ServerLevel existLevel = server.getLevel(UNION_ROOM_DIMENSION);
            if (existInst.isPresent() && existLevel != null) {
                teleportToInstance(player, existLevel, existInst.get());
            }
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.already_in_session"));
            return JoinResult.ALREADY_IN_SESSION;
        }

        Optional<UnionRoomSavedData.SessionData> sessionOpt = data.findSessionByCode(code);
        if (sessionOpt.isEmpty()) {
            maxigregrze.cobblesafari.init.ModStats.award(
                    player, maxigregrze.cobblesafari.init.ModStats.UNION_ROOM_WRONG_CODES);
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.invalid_code"));
            return JoinResult.INVALID_CODE;
        }
        UnionRoomSavedData.SessionData session = sessionOpt.get();
        if (session.hostUUID.equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.own_session"));
            return JoinResult.OWN_SESSION;
        }
        if (session.guestUUIDs.contains(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.already_joined"));
            return JoinResult.ALREADY_JOINED;
        }

        ServerPlayer host = server.getPlayerList().getPlayer(session.hostUUID);
        if (host == null || !isInUnionRoom(host)) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.host_unavailable"));
            return JoinResult.HOST_UNAVAILABLE;
        }

        MiscConfig.RoomTypeConfig type = MiscConfig.getRoomType(session.roomType);
        if (session.guestUUIDs.size() >= type.maxGuestsPerSession) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.session_full"));
            return JoinResult.SESSION_FULL;
        }

        ServerLevel unionLevel = server.getLevel(UNION_ROOM_DIMENSION);
        if (unionLevel == null) {
            return JoinResult.FAILED;
        }
        Optional<UnionRoomSavedData.InstanceData> instOpt = data.getInstance(session.instanceId);
        if (instOpt.isEmpty()) {
            return JoinResult.FAILED;
        }
        UnionRoomSavedData.InstanceData instance = instOpt.get();

        synchronized (UnionRoomManager.class) {
            session.guestUUIDs.add(player.getUUID());
            data.savePlayerOrigin(player.getUUID(), player.blockPosition(), player.level().dimension());
            PlayerTimerData timerData = TimerManager.getOrCreateData(player, DIMENSION_ID);
            timerData.setRemainingTicks(TIMER_SECONDS * 20);
            timerData.setLastResetTimestamp(System.currentTimeMillis());
            TimerManager.setPlayerOrigin(player, DIMENSION_ID, player.blockPosition(), player.level().dimension());

            teleportToInstance(player, unionLevel, instance);
            TimerManager.startTimer(player, DIMENSION_ID);
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.joined"));
            data.setDirty();
        }
        maxigregrze.cobblesafari.init.ModStats.award(
                player, maxigregrze.cobblesafari.init.ModStats.UNION_ROOMS_JOINED);
        return JoinResult.OK;
    }

    public static void closeSession(MinecraftServer server, int instanceId, String reasonKey) {
        closeSession(server, instanceId, reasonKey, null);
    }

    public static void closeSession(MinecraftServer server, int instanceId, String reasonKey,
                                    @Nullable UUID skipTeleportUuid) {
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            return;
        }
        closeSession(server, data, instanceId, reasonKey, skipTeleportUuid, false, null);
    }

    public static void disbandSession(MinecraftServer server, int instanceId, @Nullable String adminDisbandDetail) {
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            return;
        }
        closeSession(server, data, instanceId, "cobblesafari.unionroom.session_closed.disbanded", null, true,
                adminDisbandDetail);
    }

    public static void tickSessionCheck(MinecraftServer server) {
        if (++hostCheckTick < 100) return;
        hostCheckTick = 0;

        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) return;

        for (UnionRoomSavedData.SessionData session : data.getAllSessions()) {
            ServerPlayer host = server.getPlayerList().getPlayer(session.hostUUID);
            if (host != null && !isInUnionRoom(host)) {
                PlayerTimerData td = TimerManager.getOrCreateData(host, DIMENSION_ID);
                td.setActive(false);
                td.setRemainingTicks(0);
                TimerManager.savePlayerData(host, td);
                closeSession(server, data, session.instanceId,
                        "cobblesafari.unionroom.session_closed.host_left", null, false, null);
            }
        }
    }

    public static boolean forceJoinInstance(ServerPlayer player, int instanceId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.dimension_not_found"));
            return false;
        }
        Optional<UnionRoomSavedData.SessionData> sessionOpt = data.getSessionByInstanceId(instanceId);
        if (sessionOpt.isEmpty()) {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.forcejoin.not_found", instanceId));
            return false;
        }
        UnionRoomSavedData.SessionData session = sessionOpt.get();
        int[] code = new int[4];
        System.arraycopy(session.code, 0, code, 0, 4);
        return joinSession(player, code) == JoinResult.OK;
    }

    public static void handlePlayerExit(ServerPlayer player, int instanceId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            return;
        }
        Optional<UnionRoomSavedData.SessionData> sessionOpt = data.getSessionByInstanceId(instanceId);
        if (sessionOpt.isEmpty()) {
            teleportPlayerBack(server, player, data, MSG_SESSION_GONE);
            return;
        }
        UnionRoomSavedData.SessionData session = sessionOpt.get();
        if (session.hostUUID.equals(player.getUUID())) {
            synchronized (UnionRoomManager.class) {
                closeSession(server, data, instanceId, "cobblesafari.unionroom.session_closed.host_left", null,
                        false, null);
            }
        } else {
            synchronized (UnionRoomManager.class) {
                session.guestUUIDs.remove(player.getUUID());
                data.setDirty();
            }
            teleportPlayerBack(server, player, data, MSG_VOLUNTARY_EXIT);
        }
    }

    public static void showRoomCode(ServerPlayer player, int instanceId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) {
            return;
        }
        data.getSessionByInstanceId(instanceId).ifPresentOrElse(session -> {
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.code.header"));
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.code.display",
                    Component.translatable("cobblesafari.unionroom.code." + session.code[0]),
                    Component.translatable("cobblesafari.unionroom.code." + session.code[1]),
                    Component.translatable("cobblesafari.unionroom.code." + session.code[2]),
                    Component.translatable("cobblesafari.unionroom.code." + session.code[3])));
            player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.code.exit_hint"));
        }, () -> player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.error.invalid_code")));
    }

    public static void teleportPlayerBack(MinecraftServer server, ServerPlayer player,
                                          UnionRoomSavedData data, String reasonKey) {
        teleportPlayerBack(server, player, data, reasonKey, null);
    }

    public static void teleportPlayerBack(MinecraftServer server, ServerPlayer player,
                                          UnionRoomSavedData data, String reasonKey, @Nullable String reasonArg) {
        Optional<UnionRoomSavedData.PlayerOriginData> originOpt = data.getPlayerOrigin(player.getUUID());
        BlockPos targetPos;
        ServerLevel targetLevel;
        if (originOpt.isPresent()) {
            UnionRoomSavedData.PlayerOriginData origin = originOpt.get();
            targetLevel = server.getLevel(origin.dimension);
            targetPos = origin.position;
            if (targetLevel == null || isDimensionRestricted(origin.dimension)) {
                targetLevel = server.overworld();
                targetPos = targetLevel.getSharedSpawnPos();
            }
        } else {
            targetLevel = server.overworld();
            targetPos = targetLevel.getSharedSpawnPos();
        }

        player.teleportTo(targetLevel,
                targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.resetFallDistance();

        PlayerTimerData td = TimerManager.getOrCreateData(player, DIMENSION_ID);
        td.setRemainingTicks(0);
        td.setActive(false);
        TimerManager.savePlayerData(player, td);

        if (reasonKey != null && !reasonKey.isEmpty()) {
            if (MSG_VOLUNTARY_EXIT.equals(reasonKey) || MSG_SESSION_GONE.equals(reasonKey)) {
                player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.session_closed.thanks"));
            } else if (reasonArg != null && !reasonArg.isEmpty()) {
                player.sendSystemMessage(Component.translatable(reasonKey, reasonArg));
            } else {
                player.sendSystemMessage(Component.translatable(reasonKey));
            }
        }
        data.removePlayerOrigin(player.getUUID());
        data.setDirty();
    }

    private static void closeSession(MinecraftServer server, UnionRoomSavedData data, int instanceId,
                                     String reasonKey, @Nullable UUID skipTeleportUuid, boolean adminDisbandChat,
                                     @Nullable String adminDisbandDetail) {
        synchronized (UnionRoomManager.class) {
        Optional<UnionRoomSavedData.SessionData> sessionOpt = data.getSessionByInstanceId(instanceId);
        if (sessionOpt.isEmpty()) {
            return;
        }
        UnionRoomSavedData.SessionData session = sessionOpt.get();
        Optional<UnionRoomSavedData.InstanceData> instOpt = data.getInstance(session.instanceId);
        ServerLevel unionLevel = server.getLevel(UNION_ROOM_DIMENSION);
        if (unionLevel == null) {
            CobbleSafari.LOGGER.error(
                    "Union Room session close aborted: union dimension missing while closing instanceId={} reason={}",
                    instanceId, reasonKey);
            return;
        }

        Set<UUID> members = new HashSet<>(session.guestUUIDs);
        members.add(session.hostUUID);

        // Party Popper: host closes a session while guests are still present (close / teleport / disconnect).
        // Admin disbands are excluded.
        if (!adminDisbandChat && !session.guestUUIDs.isEmpty()) {
            ServerPlayer hostPlayer = server.getPlayerList().getPlayer(session.hostUUID);
            if (hostPlayer != null) {
                maxigregrze.cobblesafari.advancement.ModCriteria.UNION_PARTY_POPPER.trigger(hostPlayer);
            }
        }

        BlockPos evacCenter = instOpt
                .map(i -> i.structurePos.equals(BlockPos.ZERO) ? i.anchorPos : i.structurePos)
                .orElse(BlockPos.ZERO);

        List<ServerPlayer> toEvac = new ArrayList<>();
        for (ServerPlayer p : unionLevel.players()) {
            if (!members.contains(p.getUUID())) {
                continue;
            }
            if (instOpt.isPresent() && !inHorizontalRadius(p.blockPosition(), evacCenter, EVAC_RADIUS)) {
                continue;
            }
            if (skipTeleportUuid != null && skipTeleportUuid.equals(p.getUUID())) {
                continue;
            }
            toEvac.add(p);
        }

        String hostLabel = Optional.ofNullable(server.getPlayerList().getPlayer(session.hostUUID))
                .map(p -> p.getName().getString())
                .orElseGet(() -> session.hostUUID.toString());
        CobbleSafari.LOGGER.info(
                "Union Room session closed: instanceId={} reason={} host={} guestCount={} playersEvacuated={} "
                        + "skipTeleportUuid={} adminDisbandChat={}",
                instanceId, reasonKey, hostLabel, session.guestUUIDs.size(), toEvac.size(),
                skipTeleportUuid, adminDisbandChat);

        boolean unionFriendlyEvacMessage = !MSG_SESSION_TIMER.equals(reasonKey);

        BULK_UNION_EVAC.set(true);
        try {
            for (ServerPlayer p : toEvac) {
                TimerManager.expireActiveTimerAndTeleport(p, DIMENSION_ID,
                        TimerManager.getOrCreateData(p, DIMENSION_ID), true, unionFriendlyEvacMessage);
            }
        } finally {
            BULK_UNION_EVAC.remove();
        }

        if (adminDisbandChat) {
            for (ServerPlayer p : toEvac) {
                p.sendSystemMessage(Component.translatable("cobblesafari.unionroom.session_closed.disbanded"));
                if (adminDisbandDetail != null && !adminDisbandDetail.isBlank()) {
                    p.sendSystemMessage(Component.translatable("cobblesafari.unionroom.disband.reason_prefix",
                            adminDisbandDetail));
                }
            }
        }

        for (UUID memberId : members) {
            if (skipTeleportUuid != null && skipTeleportUuid.equals(memberId)) {
                continue;
            }
            data.removePlayerOrigin(memberId);
        }

        instOpt.ifPresent(inst -> inst.occupied = false);
        data.removeSession(instanceId);
        data.setDirty();

        for (UUID memberId : members) {
            ServerPlayer online = server.getPlayerList().getPlayer(memberId);
            if (online != null) {
                Services.PLATFORM.sendPayloadToPlayer(online,
                        new UnionAppResultPayload(UnionAppResultPayload.SUB_CLOSE_GUI, 0, 0, new int[0], ""));
            }
        }
        }
    }

    private static boolean inHorizontalRadius(BlockPos pos, BlockPos center, int radius) {
        double dx = (double) pos.getX() - center.getX();
        double dz = (double) pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    private static boolean canPlayerEnterUnionRoom(ServerPlayer player) {
        String currentDim = player.level().dimension().location().toString();
        if (SafariTimerConfig.hasDimensionTimer(currentDim)) {
            return false;
        }
        return !MiscConfig.getUnionRoomBannedDimensions().contains(currentDim);
    }

    private static boolean isDimensionRestricted(ResourceKey<Level> dimension) {
        String dimId = dimension.location().toString();
        if (SafariTimerConfig.hasDimensionTimer(dimId)) {
            return true;
        }
        return MiscConfig.getUnionRoomBannedDimensions().contains(dimId);
    }

    private static Optional<UnionRoomSavedData.InstanceData> findVacantInstance(UnionRoomSavedData data) {
        for (UnionRoomSavedData.InstanceData inst : data.getInstances()) {
            if (!inst.occupied) {
                return Optional.of(inst);
            }
        }
        return Optional.empty();
    }

    private static @Nullable UnionRoomSavedData.InstanceData createNewInstance(
            MinecraftServer server, UnionRoomSavedData data, ServerLevel unionLevel) {
        int id = data.allocateNextInstanceId();
        BlockPos structurePos = new BlockPos(id * INSTANCE_SPACING, INSTANCE_Y, 0);

        ensureChunksLoaded(unionLevel, structurePos, 3);
        StructurePlacer.placeJigsawStructureStrict(unionLevel, structurePos, JIGSAW_POOL, JIGSAW_DEPTH);

        // Jigsaw generation may complete on a later tick; queue a retry loop to
        // find the void_block once the structure is fully placed and swap it for
        // the exit teleporter.  The array holds [attemptsRemaining].
        PENDING_EXIT_SETUPS.put(id, new int[]{40});

        UnionRoomSavedData.InstanceData instance = new UnionRoomSavedData.InstanceData();
        instance.id = id;
        instance.structurePos = structurePos;
        // anchorPos = jigsaw start position (structurePos) — where the player spawns.
        // Updated to the void marker location once jigsaw completes if needed.
        instance.anchorPos = structurePos;
        instance.occupied = false;
        data.addInstance(instance);
        return instance;
    }

    private static Optional<UnionRoomSavedData.SessionData> findPlayerSession(
            UnionRoomSavedData data, UUID playerId) {
        Optional<UnionRoomSavedData.SessionData> s = data.findSessionByHost(playerId);
        if (s.isPresent()) return s;
        return data.findSessionContainingGuest(playerId);
    }

    private static void ensureChunksLoaded(ServerLevel level, BlockPos center, int chunkRadius) {
        ChunkPos centerChunk = new ChunkPos(center);
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                level.getChunk(centerChunk.x + dx, centerChunk.z + dz);
            }
        }
    }

    /** Structure NBT marks the exit anchor with {@link ModBlocks#VOID_BLOCK} (single block). */
    private static @Nullable BlockPos findUnionRoomVoidMarker(ServerLevel level, BlockPos center) {
        int scanRadius = 48;
        BlockPos best = null;
        int bestDistSq = Integer.MAX_VALUE;
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dy = -16; dy <= 32; dy++) {
                for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                    BlockPos c = center.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(c);
                    if (!st.is(ModBlocks.VOID_BLOCK)) {
                        continue;
                    }
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 < bestDistSq) {
                        bestDistSq = d2;
                        best = c;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Called every server tick from {@link maxigregrze.cobblesafari.event.DimensionEvents#onServerTick}.
     * Retries finding the void_block marker (placed by jigsaw) and converting it to the exit
     * teleporter for each pending instance.  Gives up after 40 attempts (~2 seconds).
     */
    public static void tickPendingExitSetups(MinecraftServer server) {
        if (PENDING_EXIT_SETUPS.isEmpty()) return;
        ServerLevel unionLevel = server.getLevel(UNION_ROOM_DIMENSION);
        if (unionLevel == null) return;
        UnionRoomSavedData data = UnionRoomSavedData.get(server);
        if (data == null) return;

        Iterator<Map.Entry<Integer, int[]>> iter = PENDING_EXIT_SETUPS.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, int[]> entry = iter.next();
            int instanceId = entry.getKey();
            int[] counter = entry.getValue();

            ensureChunksLoaded(unionLevel, new BlockPos(instanceId * INSTANCE_SPACING, INSTANCE_Y, 0), 3);
            BlockPos structurePos = new BlockPos(instanceId * INSTANCE_SPACING, INSTANCE_Y, 0);
            BlockPos voidMarker = findUnionRoomVoidMarker(unionLevel, structurePos);

            if (voidMarker != null) {
                replaceVoidMarkerWithExit(unionLevel, voidMarker, instanceId);
                CobbleSafari.LOGGER.info(
                        "Union Room: exit teleporter placed for instance {} at {} (attempt {})",
                        instanceId, voidMarker, 41 - counter[0]);
                iter.remove();
            } else if (counter[0] <= 0) {
                CobbleSafari.LOGGER.warn(
                        "Union Room: void marker not found after 40 ticks for instance {} — "
                                + "placing fallback exit at structure origin {}",
                        instanceId, structurePos);
                placeFallbackExit(unionLevel, structurePos, instanceId);
                iter.remove();
            } else {
                counter[0]--;
            }
        }
    }

    /** Fallback: directly place the exit teleporter when no void marker was found. */
    private static void placeFallbackExit(ServerLevel level, BlockPos pos, int instanceId) {
        BlockState exitLower = ModBlocks.UNION_ROOM_EXIT_TELEPORTER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState exitUpper = exitLower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        level.setBlockAndUpdate(pos, exitLower);
        level.setBlockAndUpdate(pos.above(), exitUpper);
        if (level.getBlockEntity(pos) instanceof UnionRoomExitTeleporterBlockEntity be) {
            be.setInstanceId(instanceId);
        }
    }

    private static void replaceVoidMarkerWithExit(ServerLevel level, BlockPos lowerPos, int instanceId) {
        BlockState lower = level.getBlockState(lowerPos);
        if (lower.is(ModBlocks.UNION_ROOM_EXIT_TELEPORTER)
                && lower.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            if (level.getBlockEntity(lowerPos) instanceof UnionRoomExitTeleporterBlockEntity be) {
                be.setInstanceId(instanceId);
            }
            return;
        }
        if (!lower.is(ModBlocks.VOID_BLOCK)) {
            CobbleSafari.LOGGER.warn(
                    "Union Room exit replacement failed at {} in dim {} instance {}: expected void_block marker or "
                            + "exit teleporter lower, found {}",
                    lowerPos, level.dimension().location(), instanceId,
                    BuiltInRegistries.BLOCK.getKey(lower.getBlock()));
            return;
        }
        Direction facing = Direction.SOUTH;
        BlockPos upperPos = lowerPos.above();
        BlockState exitLower = ModBlocks.UNION_ROOM_EXIT_TELEPORTER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, facing)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState exitUpper = exitLower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        level.setBlockAndUpdate(lowerPos, exitLower);
        level.setBlockAndUpdate(upperPos, exitUpper);
        BlockState placedLower = level.getBlockState(lowerPos);
        BlockState placedUpper = level.getBlockState(upperPos);
        if (!placedLower.is(ModBlocks.UNION_ROOM_EXIT_TELEPORTER)
                || !placedUpper.is(ModBlocks.UNION_ROOM_EXIT_TELEPORTER)) {
            CobbleSafari.LOGGER.warn(
                    "Union Room exit replacement failed at {} in dim {} instance {}: after setBlock, lower={}, "
                            + "upper={}",
                    lowerPos, level.dimension().location(), instanceId,
                    BuiltInRegistries.BLOCK.getKey(placedLower.getBlock()),
                    BuiltInRegistries.BLOCK.getKey(placedUpper.getBlock()));
        }
        if (level.getBlockEntity(lowerPos) instanceof UnionRoomExitTeleporterBlockEntity be) {
            be.setInstanceId(instanceId);
        }
    }

    private static int[] generateUniqueCode(UnionRoomSavedData data) {
        int[] code = new int[CODE_LENGTH];
        for (int attempt = 0; attempt < 2000; attempt++) {
            for (int i = 0; i < CODE_LENGTH; i++) {
                code[i] = RANDOM.nextInt(CODE_MAX - CODE_MIN + 1) + CODE_MIN;
            }
            if (data.findSessionByCode(code).isEmpty()) {
                return code;
            }
        }
        return code;
    }

    private static void teleportToInstance(ServerPlayer player, ServerLevel unionLevel,
                                           UnionRoomSavedData.InstanceData instance) {
        player.teleportTo(unionLevel,
                instance.anchorPos.getX() + 0.5,
                instance.anchorPos.getY(),
                instance.anchorPos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.resetFallDistance();
    }

    private static void sendCodeMessage(ServerPlayer player, int[] code) {
        player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.created"));
        player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.code.header"));
        player.sendSystemMessage(Component.translatable("cobblesafari.unionroom.code.display",
                Component.translatable("cobblesafari.unionroom.code." + code[0]),
                Component.translatable("cobblesafari.unionroom.code." + code[1]),
                Component.translatable("cobblesafari.unionroom.code." + code[2]),
                Component.translatable("cobblesafari.unionroom.code." + code[3])));
    }
}
