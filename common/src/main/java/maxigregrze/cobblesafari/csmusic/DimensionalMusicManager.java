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
 * Arbitrage serveur de la musique (plan 105 § 4). Calcule, par joueur, le morceau gagnant
 * entre la source « dimension » (config) et la source « boss » (override) <b>par priorité</b> :
 * la musique de boss ne l'emporte que si sa {@code priority} (csmusic JSON) est strictement
 * supérieure à celle de la dimension — ou si la dimension n'a pas de musique. Serveur autoritaire ;
 * n'émet un paquet que sur changement.
 */
public final class DimensionalMusicManager {

    /** Dernier csmusic id envoyé par joueur ("" = silence). */
    private static final Map<UUID, String> lastSent = new HashMap<>();
    /** Override « boss » par joueur (csmusic id) tant que le combat dure. */
    private static final Map<UUID, String> bossOverride = new HashMap<>();
    /** Mode de sortie à appliquer au prochain envoi pour ce joueur (sinon FADE). One‑shot. */
    private static final Map<UUID, Integer> nextMode = new HashMap<>();

    private DimensionalMusicManager() {}

    // --- Balayage par tick ---------------------------------------------------

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayer(player);
        }
    }

    /** Recalcule le morceau gagnant pour ce joueur et l'envoie si différent du dernier envoyé. */
    private static void updatePlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        CsMusicDefinition boss = CsMusicRegistry.get(bossOverride.get(uuid)).orElse(null);
        CsMusicDefinition dim = dimensionalDefFor(player);
        CsMusicDefinition winner = pickWinner(boss, dim);

        String desiredId = winner != null ? winner.id() : "";
        if (Objects.equals(lastSent.get(uuid), desiredId)) {
            nextMode.remove(uuid); // pas de changement : on consomme un éventuel mode en attente
            return;
        }
        int mode = nextMode.getOrDefault(uuid, SetCsMusicPayload.MODE_FADE);
        nextMode.remove(uuid);
        send(player, winner, mode);
    }

    /**
     * Le boss ne gagne que s'il est <b>strictement</b> plus prioritaire que la dimension
     * (ou si la dimension n'a pas de musique). À priorité égale, la dimension reste — la musique
     * de boss n'a aucun avantage « par défaut » (elle se distingue par sa {@code priority}).
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

    /** Démarrage du combat : enregistre l'override boss, l'arbitrage par priorité décide de l'effet. */
    public static void onBossStart(Collection<ServerPlayer> aliveParticipants, String csmusicId) {
        if (!CsMusicRegistry.has(csmusicId)) {
            return;
        }
        for (ServerPlayer p : aliveParticipants) {
            bossOverride.put(p.getUUID(), csmusicId);
            updatePlayer(p); // FADE par défaut si le boss l'emporte
        }
    }

    /** Victoire : si la musique de boss jouait, outro puis reprise de la dimension. */
    public static void onBossWin(Collection<ServerPlayer> aliveParticipants) {
        for (ServerPlayer p : aliveParticipants) {
            endBossFor(p, SetCsMusicPayload.MODE_OUTRO);
        }
    }

    /** Défaite / participant écarté (mort, déco, hors‑dim) : coupure sèche, pas d'outro. */
    public static void onBossLossOrLeave(ServerPlayer player) {
        endBossFor(player, SetCsMusicPayload.MODE_CUT);
    }

    private static void endBossFor(ServerPlayer player, int mode) {
        UUID uuid = player.getUUID();
        String bid = bossOverride.remove(uuid);
        if (bid == null) {
            return;
        }
        // Le mode de sortie (outro/cut) ne s'applique que si la musique de boss jouait réellement.
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

    // --- Envoi ---------------------------------------------------------------

    private static void send(ServerPlayer player, @Nullable CsMusicDefinition def, int mode) {
        SetCsMusicPayload payload = def != null
                ? SetCsMusicPayload.track(def.id(), def.intro(), def.loop(), def.outro(), mode)
                : SetCsMusicPayload.silence(mode);
        Services.PLATFORM.sendPayloadToPlayer(player, payload);
        lastSent.put(player.getUUID(), def != null ? def.id() : "");
    }
}
