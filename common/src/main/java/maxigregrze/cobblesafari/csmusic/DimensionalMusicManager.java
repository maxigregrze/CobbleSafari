package maxigregrze.cobblesafari.csmusic;

import maxigregrze.cobblesafari.config.DimensionalMusicConfig;
import maxigregrze.cobblesafari.config.DimensionalMusicData;
import maxigregrze.cobblesafari.network.SetCsMusicPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Server-side music arbitration. Computes, per player, the winning track
 * between the "boss", "area", and "dimension" sources <b>by effective priority</b>.
 * Server-authoritative; sends a packet only on change.
 */
public final class DimensionalMusicManager {

    private static final int RANK_BOSS = 2;
    private static final int RANK_AREA = 1;
    private static final int RANK_DIM = 0;

    /** Last csmusic id sent per player ("" = silence). */
    private static final Map<UUID, String> lastSent = new HashMap<>();
    /** Per-player "boss" override (csmusic id) while the fight lasts. */
    private static final Map<UUID, String> bossOverride = new HashMap<>();
    /** Exit mode to apply on the next send for this player (otherwise FADE). One-shot. */
    private static final Map<UUID, Integer> nextMode = new HashMap<>();

    private DimensionalMusicManager() {}

    public record SourceInfo(String source, String csmusicId, int priority, boolean winner) {}

    private record AreaPick(CsMusicDefinition def, int priority, String areaId) {}

    // --- Per-tick sweep --------------------------------------------------------

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayer(player);
        }
    }

    private static void updatePlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        CsMusicDefinition boss = CsMusicRegistry.get(bossOverride.get(uuid)).orElse(null);
        AreaPick area = areaDefFor(player);
        CsMusicDefinition dim = dimensionalDefFor(player);
        CsMusicDefinition winner = pickWinner(boss, area, dim);

        String desiredId = winner != null ? winner.id() : "";
        if (Objects.equals(lastSent.get(uuid), desiredId)) {
            nextMode.remove(uuid);
            return;
        }
        CsMusicDefinition prev = CsMusicRegistry.get(lastSent.get(uuid)).orElse(null);
        int mode = nextMode.getOrDefault(uuid, defaultExitMode(prev, winner));
        nextMode.remove(uuid);
        send(player, winner, mode);
    }

    @Nullable
    private static AreaPick areaDefFor(ServerPlayer player) {
        DimensionalMusicData cfg = DimensionalMusicConfig.data;
        if (cfg == null || !cfg.enabled) {
            return null;
        }
        Collection<CsMusicArea> areas = CsMusicAreaStore.areasIn(player.serverLevel());
        if (areas.isEmpty()) {
            return null;
        }
        int x = player.getBlockX();
        int y = player.getBlockY();
        int z = player.getBlockZ();
        CsMusicArea best = null;
        CsMusicDefinition bestDef = null;
        int bestPrio = Integer.MIN_VALUE;
        for (CsMusicArea area : areas) {
            if (!area.activated() || !area.contains(x, y, z)) {
                continue;
            }
            CsMusicDefinition def = CsMusicRegistry.get(area.musicId()).orElse(null);
            if (def == null) {
                continue;
            }
            int prio = def.priority();
            if (best == null || prio > bestPrio
                    || (prio == bestPrio && area.id().compareTo(best.id()) < 0)) {
                best = area;
                bestDef = def;
                bestPrio = prio;
            }
        }
        return bestDef == null ? null : new AreaPick(bestDef, bestPrio, best.id());
    }

    @Nullable
    private static CsMusicDefinition pickWinner(@Nullable CsMusicDefinition boss,
                                                @Nullable AreaPick area,
                                                @Nullable CsMusicDefinition dim) {
        CsMusicDefinition winner = null;
        int wp = Integer.MIN_VALUE;
        int wr = Integer.MIN_VALUE;
        if (boss != null && better(boss.priority(), RANK_BOSS, wp, wr)) {
            winner = boss;
            wp = boss.priority();
            wr = RANK_BOSS;
        }
        if (area != null && better(area.priority(), RANK_AREA, wp, wr)) {
            winner = area.def();
            wp = area.priority();
            wr = RANK_AREA;
        }
        if (dim != null && better(dim.priority(), RANK_DIM, wp, wr)) {
            winner = dim;
        }
        return winner;
    }

    private static boolean better(int p, int r, int wp, int wr) {
        return p > wp || (p == wp && r > wr);
    }

    private static int defaultExitMode(@Nullable CsMusicDefinition prev, @Nullable CsMusicDefinition winner) {
        if (prev == null || !prev.hasOutro()) {
            return SetCsMusicPayload.MODE_FADE;
        }
        if (winner == null) {
            return SetCsMusicPayload.MODE_OUTRO;
        }
        return winner.priority() <= prev.priority()
                ? SetCsMusicPayload.MODE_OUTRO
                : SetCsMusicPayload.MODE_FADE;
    }

    @Nullable
    private static CsMusicDefinition dimensionalDefFor(ServerPlayer player) {
        DimensionalMusicData cfg = DimensionalMusicConfig.data;
        if (cfg == null || !cfg.enabled) {
            return null;
        }
        String dimId = player.level().dimension().location().toString();
        return CsMusicRegistry.get(cfg.dimensions.get(dimId)).orElse(null);
    }

    public static List<SourceInfo> describeSourcesFor(ServerPlayer player) {
        List<SourceInfo> result = new ArrayList<>();
        UUID uuid = player.getUUID();
        CsMusicDefinition boss = CsMusicRegistry.get(bossOverride.get(uuid)).orElse(null);
        AreaPick winningArea = areaDefFor(player);
        CsMusicDefinition dim = dimensionalDefFor(player);
        String winnerKey = winningSourceKey(boss, winningArea, dim);

        if (boss != null) {
            result.add(new SourceInfo("boss", boss.id(), boss.priority(),
                    winnerKey != null && winnerKey.equals("boss:" + boss.id())));
        }

        int x = player.getBlockX();
        int y = player.getBlockY();
        int z = player.getBlockZ();
        List<ActiveArea> activeAreas = new ArrayList<>();
        for (CsMusicArea area : CsMusicAreaStore.areasIn(player.serverLevel())) {
            if (!area.activated() || !area.contains(x, y, z)) {
                continue;
            }
            CsMusicDefinition def = CsMusicRegistry.get(area.musicId()).orElse(null);
            if (def == null) {
                continue;
            }
            activeAreas.add(new ActiveArea(area, def, def.priority()));
        }
        activeAreas.sort(Comparator.comparingInt((ActiveArea a) -> a.priority).reversed()
                .thenComparing(a -> a.area.id()));
        for (ActiveArea active : activeAreas) {
            String key = "area:" + active.area.id();
            result.add(new SourceInfo(active.area.id(), active.def.id(), active.priority,
                    winnerKey != null && winnerKey.equals(key)));
        }

        if (dim != null) {
            result.add(new SourceInfo("dimension", dim.id(), dim.priority(),
                    winnerKey != null && winnerKey.equals("dimension:" + dim.id())));
        }

        return result;
    }

    private record ActiveArea(CsMusicArea area, CsMusicDefinition def, int priority) {}

    @Nullable
    private static String winningSourceKey(@Nullable CsMusicDefinition boss,
                                           @Nullable AreaPick area,
                                           @Nullable CsMusicDefinition dim) {
        int wp = Integer.MIN_VALUE;
        int wr = Integer.MIN_VALUE;
        String key = null;
        if (boss != null && better(boss.priority(), RANK_BOSS, wp, wr)) {
            key = "boss:" + boss.id();
            wp = boss.priority();
            wr = RANK_BOSS;
        }
        if (area != null && better(area.priority(), RANK_AREA, wp, wr)) {
            key = "area:" + area.areaId();
            wp = area.priority();
            wr = RANK_AREA;
        }
        if (dim != null && better(dim.priority(), RANK_DIM, wp, wr)) {
            key = "dimension:" + dim.id();
        }
        return key;
    }

    // --- Hooks boss -----------------------------------------

    public static void onBossStart(Collection<ServerPlayer> aliveParticipants, String csmusicId) {
        if (!CsMusicRegistry.has(csmusicId)) {
            return;
        }
        for (ServerPlayer p : aliveParticipants) {
            UUID uuid = p.getUUID();
            // A boss override already present means this is a phase-to-phase switch
            // (not the initial fight start): hard cut to the new phase music, no fade/outro.
            if (bossOverride.containsKey(uuid)) {
                nextMode.put(uuid, SetCsMusicPayload.MODE_CUT);
            }
            bossOverride.put(uuid, csmusicId);
            updatePlayer(p);
        }
    }

    public static void onBossWin(Collection<ServerPlayer> aliveParticipants) {
        for (ServerPlayer p : aliveParticipants) {
            endBossFor(p, SetCsMusicPayload.MODE_OUTRO);
        }
    }

    public static void onBossLossOrLeave(ServerPlayer player) {
        endBossFor(player, SetCsMusicPayload.MODE_CUT);
    }

    private static void endBossFor(ServerPlayer player, int mode) {
        UUID uuid = player.getUUID();
        String bid = bossOverride.remove(uuid);
        if (bid == null) {
            return;
        }
        if (bid.equals(lastSent.get(uuid))) {
            nextMode.put(uuid, mode);
        }
        updatePlayer(player);
    }

    public static void onPlayerDisconnect(UUID uuid) {
        lastSent.remove(uuid);
        bossOverride.remove(uuid);
        nextMode.remove(uuid);
    }

    private static void send(ServerPlayer player, @Nullable CsMusicDefinition def, int mode) {
        SetCsMusicPayload payload = def != null
                ? SetCsMusicPayload.track(def.id(), def.intro(), def.loop(), def.outro(), mode)
                : SetCsMusicPayload.silence(mode);
        Services.PLATFORM.sendPayloadToPlayer(player, payload);
        lastSent.put(player.getUUID(), def != null ? def.id() : "");
    }
}
