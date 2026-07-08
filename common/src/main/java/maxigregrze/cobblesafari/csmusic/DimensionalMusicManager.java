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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side music arbitration. Per player, picks the winning track between the boss override
 * (which <b>always</b> wins) and the set of trigger rules + areas (highest priority, deterministic
 * tiebreak). Server-authoritative; sends a packet only on change, choosing a transition
 * (fade / outro / cut / synced-crossfade when the winner is the child of the current track).
 */
public final class DimensionalMusicManager {

    private static final Random RANDOM = new Random();

    /** Last csmusic id sent per player ("" = silence). */
    private static final Map<UUID, String> lastSent = new HashMap<>();
    /** Per-player "boss" override (csmusic id) while the fight lasts. */
    private static final Map<UUID, String> bossOverride = new HashMap<>();
    /** Exit mode to apply on the next send for this player (otherwise chosen). One-shot. */
    private static final Map<UUID, Integer> nextMode = new HashMap<>();
    /** Players under a {@code /csmusic debug} override: normal arbitration is suspended. */
    private static final Set<UUID> debugPlayers = new HashSet<>();
    /** Last dimension seen per player, to hard-cut (not fade) across a dimension change. */
    private static final Map<UUID, String> lastDimension = new HashMap<>();
    /** Latch for random tag pools: the winning source key + the id it resolved to, per player. */
    private static final Map<UUID, String> latchedKey = new HashMap<>();
    private static final Map<UUID, String> latchedId = new HashMap<>();

    private DimensionalMusicManager() {}

    public record SourceInfo(String source, String csmusicId, int priority, boolean winner) {}

    private record MusicSource(String key, int priority, @Nullable String musicId, @Nullable String poolTag) {}

    private static final Comparator<MusicSource> SOURCE_ORDER =
            Comparator.comparingInt(MusicSource::priority).reversed().thenComparing(MusicSource::key);

    // --- Per-tick sweep --------------------------------------------------------

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayer(player);
        }
    }

    private static void updatePlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (debugPlayers.contains(uuid)) {
            return; // debug override: leave whatever the debug command last sent untouched
        }
        String dimNow = player.level().dimension().location().toString();
        String dimPrev = lastDimension.put(uuid, dimNow);
        boolean dimChanged = dimPrev != null && !dimPrev.equals(dimNow);

        CsMusicDefinition boss = CsMusicRegistry.get(bossOverride.get(uuid)).orElse(null);
        CsMusicDefinition winner = boss != null ? boss : resolveWinner(player);

        String desiredId = winner != null ? winner.id() : "";
        if (Objects.equals(lastSent.get(uuid), desiredId)) {
            nextMode.remove(uuid);
            return;
        }

        String prevId = lastSent.get(uuid);
        CsMusicDefinition prev = CsMusicRegistry.get(prevId).orElse(null);
        int mode;
        if (dimChanged && prev != null) {
            // Crossing a dimension boundary (portal, dungeon exit, cross-dim /tp) is a discontinuity:
            // hard-cut the outgoing track. Entering from silence still fades in.
            mode = SetCsMusicPayload.MODE_CUT;
        } else if (nextMode.containsKey(uuid)) {
            mode = nextMode.get(uuid);
        } else if (winner != null && winner.hasParent() && winner.parent().equals(prevId)) {
            mode = SetCsMusicPayload.MODE_CROSSFADE; // child takes over from its parent
        } else {
            mode = defaultExitMode(prev, winner);
        }
        nextMode.remove(uuid);
        send(player, winner, mode);
    }

    // --- Winner resolution -----------------------------------------------------

    @Nullable
    private static CsMusicDefinition resolveWinner(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!musicEnabled()) {
            clearLatch(uuid);
            return null;
        }
        List<MusicSource> sources = collectSources(player);
        if (sources.isEmpty()) {
            clearLatch(uuid);
            return null;
        }
        sources.sort(SOURCE_ORDER);
        for (MusicSource source : sources) {
            CsMusicDefinition def = resolveSource(uuid, source);
            if (def != null) {
                return def;
            }
        }
        clearLatch(uuid);
        return null;
    }

    private static List<MusicSource> collectSources(ServerPlayer player) {
        List<MusicSource> list = new ArrayList<>();
        for (CsMusicRule rule : CsMusicTriggerRegistry.all()) {
            if (rule.condition().matches(player)) {
                list.add(new MusicSource(rule.source(), rule.priority(), rule.musicId(), rule.poolTag()));
            }
        }
        int x = player.getBlockX();
        int y = player.getBlockY();
        int z = player.getBlockZ();
        for (CsMusicArea area : CsMusicAreaStore.areasIn(player.serverLevel())) {
            if (area.activated() && area.contains(x, y, z)) {
                list.add(new MusicSource("area:" + area.id(), area.priority(), area.musicId(), null));
            }
        }
        return list;
    }

    /** Resolves a source to a definition, applying the random-pool latch (updates it for the winner). */
    @Nullable
    private static CsMusicDefinition resolveSource(UUID uuid, MusicSource source) {
        if (source.poolTag() != null) {
            String prevKey = latchedKey.get(uuid);
            String prevId = latchedId.get(uuid);
            if (source.key().equals(prevKey) && prevId != null) {
                CsMusicDefinition held = CsMusicRegistry.get(prevId).orElse(null);
                if (held != null && held.hasTag(source.poolTag())) {
                    return held; // keep the same pick while this pool stays the winner
                }
            }
            List<CsMusicDefinition> pool = CsMusicRegistry.byTag(source.poolTag());
            if (pool.isEmpty()) {
                return null;
            }
            CsMusicDefinition picked = pool.get(RANDOM.nextInt(pool.size()));
            latchedKey.put(uuid, source.key());
            latchedId.put(uuid, picked.id());
            return picked;
        }
        CsMusicDefinition def = CsMusicRegistry.get(source.musicId()).orElse(null);
        if (def != null) {
            latchedKey.put(uuid, source.key());
            latchedId.put(uuid, def.id());
        }
        return def;
    }

    /** Read-only resolution (no latch mutation) for {@code /csmusic current}. */
    @Nullable
    private static CsMusicDefinition peekResolve(UUID uuid, MusicSource source) {
        if (source.poolTag() != null) {
            String prevId = latchedId.get(uuid);
            if (source.key().equals(latchedKey.get(uuid)) && prevId != null) {
                CsMusicDefinition held = CsMusicRegistry.get(prevId).orElse(null);
                if (held != null && held.hasTag(source.poolTag())) {
                    return held;
                }
            }
            List<CsMusicDefinition> pool = CsMusicRegistry.byTag(source.poolTag());
            return pool.isEmpty() ? null : pool.get(0);
        }
        return CsMusicRegistry.get(source.musicId()).orElse(null);
    }

    private static void clearLatch(UUID uuid) {
        latchedKey.remove(uuid);
        latchedId.remove(uuid);
    }

    private static boolean musicEnabled() {
        DimensionalMusicData cfg = DimensionalMusicConfig.data;
        return cfg == null || cfg.enabled;
    }

    private static int defaultExitMode(@Nullable CsMusicDefinition prev, @Nullable CsMusicDefinition winner) {
        if (prev != null && prev.hasOutro() && winner == null) {
            return SetCsMusicPayload.MODE_OUTRO;
        }
        return SetCsMusicPayload.MODE_FADE;
    }

    // --- /csmusic current ------------------------------------------------------

    public static List<SourceInfo> describeSourcesFor(ServerPlayer player) {
        List<SourceInfo> result = new ArrayList<>();
        UUID uuid = player.getUUID();
        CsMusicDefinition boss = CsMusicRegistry.get(bossOverride.get(uuid)).orElse(null);
        if (boss != null) {
            result.add(new SourceInfo("boss", boss.id(), Integer.MAX_VALUE, true));
        }
        List<MusicSource> sources = collectSources(player);
        sources.sort(SOURCE_ORDER);

        String winnerKey = null;
        if (boss == null) {
            for (MusicSource source : sources) {
                if (peekResolve(uuid, source) != null) {
                    winnerKey = source.key();
                    break;
                }
            }
        }
        for (MusicSource source : sources) {
            CsMusicDefinition def = peekResolve(uuid, source);
            String id = def != null
                    ? def.id()
                    : (source.poolTag() != null ? "#" + source.poolTag() : String.valueOf(source.musicId()));
            boolean winner = boss == null && source.key().equals(winnerKey);
            result.add(new SourceInfo(source.key(), id, source.priority(), winner));
        }
        return result;
    }

    // --- Boss hooks ------------------------------------------------------------

    public static void onBossStart(Collection<ServerPlayer> aliveParticipants, String csmusicId) {
        if (!CsMusicRegistry.has(csmusicId)) {
            return;
        }
        for (ServerPlayer p : aliveParticipants) {
            UUID uuid = p.getUUID();
            // A boss override already present means this is a phase-to-phase switch. If the next
            // phase's music is the child of the current phase's music, do a synced crossfade;
            // otherwise hard-cut to the new phase.
            if (bossOverride.containsKey(uuid)) {
                String prev = bossOverride.get(uuid);
                CsMusicDefinition next = CsMusicRegistry.get(csmusicId).orElse(null);
                boolean childOfPrev = next != null && next.hasParent() && next.parent().equals(prev);
                nextMode.put(uuid, childOfPrev
                        ? SetCsMusicPayload.MODE_CROSSFADE
                        : SetCsMusicPayload.MODE_CUT);
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
        debugPlayers.remove(uuid);
        lastDimension.remove(uuid);
        clearLatch(uuid);
    }

    // --- Debug (test commands) -------------------------------------------------

    /**
     * Sends a track directly to the player, bypassing the normal arbitration (which is suspended
     * for this player until {@link #debugStop}). Used by {@code /csmusic debug play|crossfade}.
     */
    public static void debugPlay(ServerPlayer player, CsMusicDefinition def, int mode, int startMs) {
        UUID uuid = player.getUUID();
        debugPlayers.add(uuid);
        SetCsMusicPayload payload = SetCsMusicPayload.track(
                def.id(), def.intro(), def.loop(), def.outro(), mode, startMs);
        Services.PLATFORM.sendPayloadToPlayer(player, payload);
        lastSent.put(uuid, def.id());
    }

    /** Clears the debug override and silences the player. */
    public static void debugStop(ServerPlayer player) {
        UUID uuid = player.getUUID();
        debugPlayers.remove(uuid);
        Services.PLATFORM.sendPayloadToPlayer(player, SetCsMusicPayload.silence(SetCsMusicPayload.MODE_CUT));
        lastSent.remove(uuid);
    }

    // --- Send ------------------------------------------------------------------

    private static void send(ServerPlayer player, @Nullable CsMusicDefinition def, int mode) {
        SetCsMusicPayload payload = def != null
                ? SetCsMusicPayload.track(def.id(), def.intro(), def.loop(), def.outro(), mode)
                : SetCsMusicPayload.silence(mode);
        Services.PLATFORM.sendPayloadToPlayer(player, payload);
        lastSent.put(player.getUUID(), def != null ? def.id() : "");
    }
}
