package maxigregrze.cobblesafari.csmusic;

import maxigregrze.cobblesafari.config.CsMusicSettingsConfig;
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
 * Server-side music arbitration. Per player, resolves the winning <b>instruction</b> between the
 * boss override (which <b>always</b> wins) and the set of trigger rules + areas (highest priority,
 * deterministic tiebreak). Server-authoritative for <i>what</i> plays; a tag pool is handed to the
 * client whole, and the client draws and sequences inside it (§ 3.1 of plan 172).
 *
 * <p>A packet is sent only when the resolved instruction's <b>content signature</b> changes. That is
 * what replaced the old per-rule latch: two rules naming the same tag resolve to the same signature,
 * so walking the border between them no longer re-draws a track every sweep.</p>
 *
 * <p>The periodic sweep runs every {@code arbitrationIntervalTicks} ticks - transitions fade over
 * one to five seconds, so a few ticks of granularity are inaudible. Boss transitions bypass the
 * cadence entirely and update immediately.</p>
 */
public final class DimensionalMusicManager {

    private static final int DEFAULT_ARBITRATION_INTERVAL = 5;
    /** Rank of "nothing playing", so silence always sorts below any real source. */
    private static final int SILENCE_PRIORITY = Integer.MIN_VALUE;
    /** Rank of a boss override, which outranks everything by construction. */
    private static final int BOSS_PRIORITY = Integer.MAX_VALUE;

    /** Everything the arbiter remembers about one player. Removed wholesale on disconnect. */
    private static final class PlayerState {
        /** Signature of the last instruction sent ("" = silence, null = nothing sent yet). */
        @Nullable String lastSignature;
        /** Priority of that instruction, for the transition matrix (§ 3.9). */
        int lastPriority = SILENCE_PRIORITY;
        /** Id of the outgoing track when it was unambiguous, else null (pool of two or more). */
        @Nullable String lastTrackId;
        /** Whether every entry of the outgoing instruction declares an outro. */
        boolean lastHasOutro;
        /** "Boss" override (csmusic id) while the fight lasts. */
        @Nullable String bossOverride;
        /** Exit mode to apply on the next send (otherwise chosen). One-shot. */
        @Nullable Integer nextMode;
        /** Under a {@code /csmusic debug} override: normal arbitration is suspended. */
        boolean debug;
        /** Last dimension seen, to hard-cut (not fade) across a dimension change. */
        @Nullable String lastDimension;
    }

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    private DimensionalMusicManager() {}

    /** One line of {@code /csmusic current}. {@code poolSize} is 0 for a single-track source. */
    public record SourceInfo(String source, String csmusicId, int poolSize, int repeat,
                             int priority, boolean winner) {}

    private record MusicSource(String key, int priority, @Nullable String musicId, @Nullable String poolTag) {}

    private static final Comparator<MusicSource> SOURCE_ORDER =
            Comparator.comparingInt(MusicSource::priority).reversed().thenComparing(MusicSource::key);

    private static PlayerState state(UUID uuid) {
        return STATES.computeIfAbsent(uuid, k -> new PlayerState());
    }

    // --- Per-tick sweep --------------------------------------------------------

    public static void tick(MinecraftServer server) {
        if ((server.getTickCount() % arbitrationInterval()) != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayer(player);
        }
    }

    private static int arbitrationInterval() {
        int interval = CsMusicSettingsConfig.data.arbitrationIntervalTicks;
        return interval < 1 ? DEFAULT_ARBITRATION_INTERVAL : interval;
    }

    private static void updatePlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerState st = state(uuid);
        if (st.debug) {
            return; // debug override: leave whatever the debug command last sent untouched
        }
        String dimNow = player.level().dimension().location().toString();
        String dimPrev = st.lastDimension;
        st.lastDimension = dimNow;
        boolean dimChanged = dimPrev != null && !dimPrev.equals(dimNow);

        // Failsafe: a boss override always wins arbitration, so a missed end-of-fight hook would
        // pin this player's music forever. Drop it as soon as they are no longer in a fight.
        if (st.bossOverride != null
                && !maxigregrze.cobblesafari.csboss.BossBattleManager.isParticipant(uuid)) {
            st.bossOverride = null;
        }
        CsMusicDefinition boss = CsMusicRegistry.get(st.bossOverride).orElse(null);
        CsMusicInstruction winner = boss != null
                ? CsMusicInstruction.single(boss, BOSS_PRIORITY)
                : resolveWinner(player);

        String desired = winner != null ? winner.signature() : "";
        if (Objects.equals(st.lastSignature, desired)) {
            st.nextMode = null;
            // Same content at a different priority: nothing to re-emit, but the rank must stay
            // current or the next real change would compare against a stale one.
            st.lastPriority = winner != null ? winner.priority() : SILENCE_PRIORITY;
            return;
        }

        int mode = chooseMode(st, winner, dimChanged);
        st.nextMode = null;
        send(player, st, winner, mode);
    }

    // --- Transition matrix (§ 3.9) ---------------------------------------------

    /**
     * Precedence: dimension change, then an explicit boss mode, then a {@code parent} relation -
     * which imposes the synced crossfade <b>whatever the priorities</b> - and only then the
     * priority gap. The outro is a conclusion, not a transition: it plays when the music winds down
     * (silence or a lower-priority winner), never when something more urgent takes over.
     */
    private static int chooseMode(PlayerState st, @Nullable CsMusicInstruction winner, boolean dimChanged) {
        boolean somethingPlaying = st.lastSignature != null && !st.lastSignature.isEmpty();
        if (dimChanged && somethingPlaying) {
            // Crossing a dimension boundary (portal, dungeon exit, cross-dim /tp) is a
            // discontinuity: hard-cut the outgoing track. Entering from silence still fades in.
            return SetCsMusicPayload.MODE_CUT;
        }
        if (st.nextMode != null) {
            return st.nextMode;
        }
        if (!somethingPlaying) {
            return SetCsMusicPayload.MODE_FADE; // entering from silence: nothing to cross with
        }

        CsMusicDefinition prev = CsMusicRegistry.get(st.lastTrackId).orElse(null);
        CsMusicDefinition next = winner != null ? winner.soleTrack() : null;
        if (crossfadeRelated(prev, next)) {
            return SetCsMusicPayload.MODE_CROSSFADE;
        }

        int newPriority = winner != null ? winner.priority() : SILENCE_PRIORITY;
        if (newPriority > st.lastPriority) {
            return SetCsMusicPayload.MODE_CUT; // an interruption: waiting on an outro would lag it
        }
        if (newPriority == st.lastPriority) {
            return SetCsMusicPayload.MODE_FADE; // lateral move: cross-ramp from the current position
        }
        return st.lastHasOutro ? SetCsMusicPayload.MODE_OUTRO : SetCsMusicPayload.MODE_FADE;
    }

    /**
     * True when two tracks are musically related, so the transition is a <b>synced crossfade</b>
     * (the incoming track starts at the outgoing track's loop playhead): the winner is the child of
     * the current track, the current track is the child of the winner (return to parent), or both
     * declare the same parent (siblings). Related tracks must share the same loop duration.
     *
     * <p>Only decidable when both sides are unambiguous. Inside a pool of two or more the server
     * does not know which entry is playing, so the client re-checks and promotes the mode itself
     * ({@code CsMusicPlayer#promoteIfRelated}).</p>
     */
    private static boolean crossfadeRelated(@Nullable CsMusicDefinition prev, @Nullable CsMusicDefinition winner) {
        if (prev == null || winner == null) {
            return false;
        }
        if (winner.hasParent() && winner.parent().equals(prev.id())) {
            return true; // parent -> child
        }
        if (prev.hasParent() && prev.parent().equals(winner.id())) {
            return true; // child -> parent
        }
        return prev.hasParent() && winner.hasParent() && winner.parent().equals(prev.parent()); // siblings
    }

    // --- Winner resolution -----------------------------------------------------

    @Nullable
    private static CsMusicInstruction resolveWinner(ServerPlayer player) {
        if (!musicEnabled()) {
            return null;
        }
        List<MusicSource> sources = collectSources(player);
        if (sources.isEmpty()) {
            return null;
        }
        sources.sort(SOURCE_ORDER);
        for (MusicSource source : sources) {
            CsMusicInstruction instruction = resolveSource(source);
            if (instruction != null) {
                return instruction;
            }
        }
        return null;
    }

    private static List<MusicSource> collectSources(ServerPlayer player) {
        return collectSources(new CsMusicEvalContext(player));
    }

    private static List<MusicSource> collectSources(CsMusicEvalContext ctx) {
        List<MusicSource> list = new ArrayList<>();
        // Areas first: rules may carry an area axis, and the very same resolved list then feeds the
        // implicit sources below - the area store is walked once per sweep, as before. Insertion
        // order is irrelevant, SOURCE_ORDER sorts by priority then key.
        for (CsMusicArea area : ctx.areasHere()) {
            if (area.hasMusic()) {
                list.add(new MusicSource("area:" + area.id(), area.priority(), area.musicId(), null));
            }
        }
        for (CsMusicRule rule : CsMusicTriggerRegistry.all()) {
            if (rule.condition().matches(ctx)) {
                list.add(new MusicSource(rule.source(), rule.priority(), rule.musicId(), rule.poolTag()));
            }
        }
        return list;
    }

    /**
     * Resolves a source to an instruction. Pure: with the draw moved client-side there is no latch
     * left to keep, so this is also what {@code /csmusic current} calls.
     */
    @Nullable
    private static CsMusicInstruction resolveSource(MusicSource source) {
        if (source.poolTag() != null) {
            List<CsMusicDefinition> pool = CsMusicRegistry.byTag(source.poolTag());
            return pool.isEmpty()
                    ? null
                    : CsMusicInstruction.pool(source.poolTag(), pool, repeatBeforeShuffle(), source.priority());
        }
        CsMusicDefinition def = CsMusicRegistry.get(source.musicId()).orElse(null);
        return def == null ? null : CsMusicInstruction.single(def, source.priority());
    }

    private static boolean musicEnabled() {
        return CsMusicSettingsConfig.data.enabled;
    }

    private static int repeatBeforeShuffle() {
        return Math.max(0, CsMusicSettingsConfig.data.repeatBeforeShuffle);
    }

    // --- /csmusic current ------------------------------------------------------

    /** Areas the player currently stands in, so {@code /csmusic current} can explain a rule that
     * carries an area axis but never fires (wrong id, wrong tag, area disabled, box misplaced). */
    public static List<CsMusicArea> areasHereFor(ServerPlayer player) {
        return new CsMusicEvalContext(player).areasHere();
    }

    public static List<SourceInfo> describeSourcesFor(ServerPlayer player) {
        List<SourceInfo> result = new ArrayList<>();
        PlayerState st = state(player.getUUID());
        CsMusicDefinition boss = CsMusicRegistry.get(st.bossOverride).orElse(null);
        if (boss != null) {
            result.add(new SourceInfo("boss", boss.id(), 0, 0, Integer.MAX_VALUE, true));
        }
        List<MusicSource> sources = collectSources(player);
        sources.sort(SOURCE_ORDER);

        String winnerKey = null;
        if (boss == null) {
            for (MusicSource source : sources) {
                if (resolveSource(source) != null) {
                    winnerKey = source.key();
                    break;
                }
            }
        }
        for (MusicSource source : sources) {
            CsMusicInstruction instruction = resolveSource(source);
            String label = instruction != null
                    ? instruction.displayId()
                    : (source.poolTag() != null ? "#" + source.poolTag() : String.valueOf(source.musicId()));
            // A pool of one is reported as a plain track: the client has no choice to make either.
            int poolSize = instruction != null && instruction.isPool() && instruction.entries().size() > 1
                    ? instruction.entries().size() : 0;
            int repeat = instruction != null ? instruction.repeat() : 0;
            boolean winner = boss == null && source.key().equals(winnerKey);
            result.add(new SourceInfo(source.key(), label, poolSize, repeat, source.priority(), winner));
        }
        return result;
    }

    // --- Boss hooks ------------------------------------------------------------

    public static void onBossStart(Collection<ServerPlayer> aliveParticipants, String csmusicId) {
        if (!CsMusicRegistry.has(csmusicId)) {
            return;
        }
        for (ServerPlayer p : aliveParticipants) {
            PlayerState st = state(p.getUUID());
            // A boss override already present means this is a phase-to-phase switch. If the next
            // phase's music is related to the current phase's music (parent/child/sibling), do a
            // synced crossfade; otherwise hard-cut to the new phase.
            if (st.bossOverride != null) {
                CsMusicDefinition prev = CsMusicRegistry.get(st.bossOverride).orElse(null);
                CsMusicDefinition next = CsMusicRegistry.get(csmusicId).orElse(null);
                st.nextMode = crossfadeRelated(prev, next)
                        ? SetCsMusicPayload.MODE_CROSSFADE
                        : SetCsMusicPayload.MODE_CUT;
            }
            st.bossOverride = csmusicId;
            updatePlayer(p); // immediate: a boss transition must not wait for the sweep cadence
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
        PlayerState st = state(player.getUUID());
        String bid = st.bossOverride;
        if (bid == null) {
            return;
        }
        st.bossOverride = null;
        if (bid.equals(st.lastTrackId)) {
            st.nextMode = mode;
        }
        updatePlayer(player);
    }

    public static void onPlayerDisconnect(UUID uuid) {
        STATES.remove(uuid);
    }

    /**
     * Forces every player's instruction to be re-sent at the next sweep. Called after a datapack
     * reload: clients hold a copy of their pool's entries and would otherwise keep sequencing a
     * pool whose membership just changed.
     */
    public static void invalidateAll() {
        for (PlayerState st : STATES.values()) {
            st.lastSignature = null;
        }
    }

    /** Server shutdown: this state is static and would otherwise outlive an integrated server. */
    public static void clearAll() {
        STATES.clear();
    }

    // --- Debug (test commands) -------------------------------------------------

    /**
     * Sends a track directly to the player, bypassing the normal arbitration (which is suspended
     * for this player until {@link #debugStop}). Used by {@code /csmusic debug play|crossfade}.
     */
    public static void debugPlay(ServerPlayer player, CsMusicDefinition def, int mode, int startMs) {
        PlayerState st = state(player.getUUID());
        st.debug = true;
        SetCsMusicPayload payload = SetCsMusicPayload.track(
                def.id(), def.intro(), def.loop(), def.outro(), mode, startMs);
        Services.PLATFORM.sendPayloadToPlayer(player, payload);
        st.lastSignature = def.id();
        st.lastTrackId = def.id();
        st.lastHasOutro = def.hasOutro();
    }

    /** Clears the debug override and silences the player. */
    public static void debugStop(ServerPlayer player) {
        PlayerState st = state(player.getUUID());
        st.debug = false;
        Services.PLATFORM.sendPayloadToPlayer(player, SetCsMusicPayload.silence(SetCsMusicPayload.MODE_CUT));
        st.lastSignature = null;
        st.lastTrackId = null;
        st.lastHasOutro = false;
    }

    // --- Send ------------------------------------------------------------------

    private static void send(ServerPlayer player, PlayerState st,
                             @Nullable CsMusicInstruction instruction, int mode) {
        SetCsMusicPayload payload = instruction != null
                ? SetCsMusicPayload.instruction(instruction.displayId(), toEntries(instruction),
                        instruction.repeat(), mode)
                : SetCsMusicPayload.silence(mode);
        Services.PLATFORM.sendPayloadToPlayer(player, payload);

        st.lastSignature = instruction != null ? instruction.signature() : "";
        st.lastPriority = instruction != null ? instruction.priority() : SILENCE_PRIORITY;
        CsMusicDefinition sole = instruction != null ? instruction.soleTrack() : null;
        st.lastTrackId = sole != null ? sole.id() : null;
        st.lastHasOutro = instruction != null && instruction.allHaveOutro();
    }

    private static List<SetCsMusicPayload.Entry> toEntries(CsMusicInstruction instruction) {
        List<SetCsMusicPayload.Entry> out = new ArrayList<>(instruction.entries().size());
        for (CsMusicDefinition def : instruction.entries()) {
            out.add(new SetCsMusicPayload.Entry(def.id(), def.intro(), def.loop(), def.outro(), def.parent()));
        }
        return out;
    }
}
