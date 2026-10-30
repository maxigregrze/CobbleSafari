package maxigregrze.cobblesafari.dungeon.hyperspace;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.data.DungeonInstanceRecord;
import maxigregrze.cobblesafari.data.DungeonPositionSavedData;
import maxigregrze.cobblesafari.data.HyperspaceSkyMapSavedData;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.network.HyperspaceSkyMapPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The overworld map in the Hyperspace sky (action plan 197, sections 5 and 6): sampling it when an
 * instance is created, falling back on the world spawn map, and sending the right map to each player
 * according to the instance they stand in.
 */
public final class HyperspaceSkyMapService {

    /** Above this share of empty pixels, the portal's own map is dropped for the spawn map (decision 5.2 bis). */
    private static final double MAX_MISSING_RATIO = 0.4;
    private static final int CONTEXT_INTERVAL_TICKS = 10;

    private static final Map<UUID, HyperspaceSkyMapJob> INSTANCE_JOBS = new HashMap<>();
    /** Player to the instance whose map they were last sent ({@code null}-free: absent = nothing sent). */
    private static final Map<UUID, UUID> SENT = new HashMap<>();
    private static HyperspaceSkyMapJob spawnJob;
    /** The spawn map was attempted this session and came out empty: do not retry until restart. */
    private static boolean spawnMapUnavailable;

    private HyperspaceSkyMapService() {}

    /** Section 5.1: called once, when a {@code dungeon_paris} instance is created at its Hoopa portal. */
    public static void startForInstance(MinecraftServer server, UUID instanceId, ServerLevel portalLevel, BlockPos portalPos) {
        if (portalLevel.dimensionType().hasCeiling()) {
            useSpawnMap(server, instanceId);
            return;
        }
        INSTANCE_JOBS.put(instanceId, HyperspaceSkyMapJob.start(portalLevel, portalPos.getX(), portalPos.getZ()));
    }

    public static void tick(MinecraftServer server) {
        tickInstanceJobs(server);
        tickSpawnJob(server);
        if (server.getTickCount() % CONTEXT_INTERVAL_TICKS == 0) {
            tickContext(server);
        }
    }

    /** The instance is gone: its map, any sampling still running for it. */
    public static void purgeInstance(MinecraftServer server, UUID instanceId) {
        INSTANCE_JOBS.remove(instanceId);
        HyperspaceSkyMapSavedData.get(server).remove(instanceId);
    }

    public static void onPlayerDisconnect(UUID playerId) {
        SENT.remove(playerId);
    }

    public static void clear() {
        INSTANCE_JOBS.clear();
        SENT.clear();
        spawnJob = null;
        spawnMapUnavailable = false;
    }

    private static void tickInstanceJobs(MinecraftServer server) {
        Iterator<Map.Entry<UUID, HyperspaceSkyMapJob>> it = INSTANCE_JOBS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, HyperspaceSkyMapJob> entry = it.next();
            HyperspaceSkyMapJob job = entry.getValue();
            job.tick();
            if (!job.isFinished()) {
                continue;
            }
            it.remove();
            UUID instanceId = entry.getKey();
            double missing = job.missingRatio();
            if (missing > MAX_MISSING_RATIO) {
                CobbleSafari.LOGGER.info("[Hyperspace] sky map of instance {} is {}% empty, using the spawn map",
                        instanceId, Math.round(missing * 100));
                useSpawnMap(server, instanceId);
            } else {
                HyperspaceSkyMapSavedData.get(server).put(instanceId,
                        new HyperspaceSkyMapSavedData.Entry(HyperspaceSkyMapSavedData.Source.PORTAL, job.encode()));
                resend(server, instanceId);
            }
        }
    }

    private static void tickSpawnJob(MinecraftServer server) {
        if (spawnJob == null) {
            return;
        }
        spawnJob.tick();
        if (!spawnJob.isFinished()) {
            return;
        }
        HyperspaceSkyMapJob job = spawnJob;
        spawnJob = null;
        if (job.missingRatio() >= 1.0) {
            CobbleSafari.LOGGER.warn("[Hyperspace] no generated chunk around the world spawn, the sky shows the fixed image");
            spawnMapUnavailable = true;
            return;
        }
        HyperspaceSkyMapSavedData data = HyperspaceSkyMapSavedData.get(server);
        data.setSpawnColors(job.encode());
        // Every instance on the spawn map was shown the fixed image meanwhile.
        for (UUID instanceId : SENT.values().stream().distinct().toList()) {
            HyperspaceSkyMapSavedData.Entry entry = data.get(instanceId);
            if (entry == null || entry.source() == HyperspaceSkyMapSavedData.Source.SPAWN) {
                resend(server, instanceId);
            }
        }
    }

    private static void useSpawnMap(MinecraftServer server, UUID instanceId) {
        HyperspaceSkyMapSavedData.get(server).put(instanceId,
                new HyperspaceSkyMapSavedData.Entry(HyperspaceSkyMapSavedData.Source.SPAWN, null));
        ensureSpawnMap(server);
        resend(server, instanceId);
    }

    /** Section 5.4: built on first need, then kept. */
    private static void ensureSpawnMap(MinecraftServer server) {
        if (spawnJob != null || spawnMapUnavailable || HyperspaceSkyMapSavedData.get(server).getSpawnColors() != null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        spawnJob = HyperspaceSkyMapJob.start(overworld, spawn.getX(), spawn.getZ());
    }

    /** Section 6.2: which instance each player in Hyperspace stands in, and a map when it changes. */
    private static void tickContext(MinecraftServer server) {
        DungeonPositionSavedData positions = DungeonPositionSavedData.get(server);
        String dimensionId = DungeonDimensions.DUNGEON_PARIS.location().toString();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID instanceId = null;
            if (player.level().dimension().equals(DungeonDimensions.DUNGEON_PARIS)) {
                DungeonInstanceRecord record = positions.findInstanceByFootprint(dimensionId, player.blockPosition());
                instanceId = record == null ? null : record.instanceId();
            }
            UUID previous = SENT.get(player.getUUID());
            if (Objects.equals(instanceId, previous)) {
                continue;
            }
            if (instanceId == null) {
                SENT.remove(player.getUUID());
                Services.PLATFORM.sendPayloadToPlayer(player, HyperspaceSkyMapPayload.none());
            } else {
                SENT.put(player.getUUID(), instanceId);
                Services.PLATFORM.sendPayloadToPlayer(player, payloadFor(server, instanceId));
            }
        }
    }

    private static void resend(MinecraftServer server, UUID instanceId) {
        HyperspaceSkyMapPayload payload = null;
        for (Map.Entry<UUID, UUID> sent : SENT.entrySet()) {
            if (!instanceId.equals(sent.getValue())) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(sent.getKey());
            if (player != null) {
                if (payload == null) {
                    payload = payloadFor(server, instanceId);
                }
                Services.PLATFORM.sendPayloadToPlayer(player, payload);
            }
        }
    }

    private static HyperspaceSkyMapPayload payloadFor(MinecraftServer server, UUID instanceId) {
        HyperspaceSkyMapSavedData data = HyperspaceSkyMapSavedData.get(server);
        HyperspaceSkyMapSavedData.Entry entry = data.get(instanceId);
        if (entry != null && entry.source() == HyperspaceSkyMapSavedData.Source.PORTAL && entry.colors() != null) {
            return HyperspaceSkyMapPayload.map(entry.colors());
        }
        if (entry == null && INSTANCE_JOBS.containsKey(instanceId)) {
            // Still sampling: the portal's map arrives when the job finishes.
            return HyperspaceSkyMapPayload.staticImage();
        }
        // On the spawn map, or an instance older than the sky maps (section 5.3).
        byte[] spawn = data.getSpawnColors();
        if (spawn != null) {
            return HyperspaceSkyMapPayload.map(spawn);
        }
        ensureSpawnMap(server);
        return HyperspaceSkyMapPayload.staticImage();
    }
}
