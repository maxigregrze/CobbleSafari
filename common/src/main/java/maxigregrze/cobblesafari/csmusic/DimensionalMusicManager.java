package maxigregrze.cobblesafari.csmusic;

import maxigregrze.cobblesafari.config.DimensionalMusicConfig;
import maxigregrze.cobblesafari.config.DimensionalMusicData;
import maxigregrze.cobblesafari.network.SetCsMusicPayload;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Server-side music arbitration (plan 105 § 4). Computes, per player, the winning track
 * between the "dimension" source (config) and the "boss" source (override) <b>by priority</b>:
 * boss music wins only if its {@code priority} (csmusic JSON) is strictly
 * higher than the dimension's — or if the dimension has no music. Server-authoritative;
 * sends a packet only on change.
 */
public final class DimensionalMusicManager {

    /** Last csmusic id sent per player ("" = silence). */
    private static final Map<UUID, String> lastSent = new HashMap<>();
    /** Per-player "boss" override (csmusic id) while the fight lasts. */
    private static final Map<UUID, String> bossOverride = new HashMap<>();
    /** Exit mode to apply on the next send for this player (otherwise FADE). One-shot. */
    private static final Map<UUID, Integer> nextMode = new HashMap<>();

    private DimensionalMusicManager() {}

    // --- Per-tick sweep --------------------------------------------------------

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayer(player);
        }
    }

    /** Recomputes the winning track for this player and sends if different from the last sent. */
    private static void updatePlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        CsMusicDefinition boss = CsMusicRegistry.get(bossOverride.get(uuid)).orElse(null);
        CsMusicDefinition dim = dimensionalDefFor(player);
        CsMusicDefinition winner = pickWinner(boss, dim);

        String desiredId = winner != null ? winner.id() : "";
        if (Objects.equals(lastSent.get(uuid), desiredId)) {
            nextMode.remove(uuid); // no change: consume any pending mode
            return;
        }
        int mode = nextMode.getOrDefault(uuid, SetCsMusicPayload.MODE_FADE);
        nextMode.remove(uuid);
        send(player, winner, mode);
    }

    /**
     * Boss wins only if it is <b>strictly</b> higher priority than the dimension
     * (or if the dimension has no music). At equal priority, dimension stays — boss music
     * has no "default" advantage (it distinguishes itself via {@code priority}).
     */
    @Nullable
    private static CsMusicDefinition pickWinner(@Nullable CsMusicDefinition boss, @Nullable CsMusicDefinition dim) {
        if (boss == null) {
            return dim;
        }
        if (dim == null) {
            return boss;
        }
        return boss.priority() > dim.priority() ? boss : dim;
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

    // --- Hooks boss (plan 105 § 4.2) -----------------------------------------

    /** Fight start: registers boss override; priority arbitration decides the effect. */
    public static void onBossStart(Collection<ServerPlayer> aliveParticipants, String csmusicId) {
        if (!CsMusicRegistry.has(csmusicId)) {
            return;
        }
        for (ServerPlayer p : aliveParticipants) {
            bossOverride.put(p.getUUID(), csmusicId);
            updatePlayer(p); // FADE by default if boss wins
        }
    }

    /** Victory: if boss music was playing, outro then resume dimension music. */
    public static void onBossWin(Collection<ServerPlayer> aliveParticipants) {
        for (ServerPlayer p : aliveParticipants) {
            endBossFor(p, SetCsMusicPayload.MODE_OUTRO);
        }
    }

    /** Defeat / discarded participant (death, disconnect, out-of-dim): hard cut, no outro. */
    public static void onBossLossOrLeave(ServerPlayer player) {
        endBossFor(player, SetCsMusicPayload.MODE_CUT);
    }

    private static void endBossFor(ServerPlayer player, int mode) {
        UUID uuid = player.getUUID();
        String bid = bossOverride.remove(uuid);
        if (bid == null) {
            return;
        }
        // Exit mode (outro/cut) applies only if boss music was actually playing.
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

    // --- Send ------------------------------------------------------------------

    private static void send(ServerPlayer player, @Nullable CsMusicDefinition def, int mode) {
        SetCsMusicPayload payload = def != null
                ? SetCsMusicPayload.track(def.id(), def.intro(), def.loop(), def.outro(), mode)
                : SetCsMusicPayload.silence(mode);
        Services.PLATFORM.sendPayloadToPlayer(player, payload);
        lastSent.put(player.getUUID(), def != null ? def.id() : "");
    }
}
