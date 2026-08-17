package maxigregrze.cobblesafari.client.audio;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.mixin.client.MusicManagerAccessor;
import maxigregrze.cobblesafari.network.SetCsMusicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client csmusic player built on a custom OpenAL backend ({@link CsMusicVoice}) instead of the
 * vanilla mixer, so it can seek to an arbitrary offset and run two voices at once. This is what
 * makes the <b>synced crossfade</b> possible: the child ({@link SetCsMusicPayload#MODE_CROSSFADE})
 * starts at the parent's exact loop playhead and both cross-ramp. Also supports a one-shot
 * <b>outro</b> ({@link SetCsMusicPayload#MODE_OUTRO}) played before the next track.
 *
 * <p>The server sends an <b>instruction</b>: one track, or a whole tag pool. For a pool this class
 * draws an entry, plays {@code intro → loop × repeat → outro}, draws again, and keeps going until a
 * new instruction arrives. Everything the server can decide it still decides - which pool wins, and
 * the base transition mode; this class only ever <i>promotes</i> a mode to a synced crossfade when
 * it sees a {@code parent} relation the server could not (plan 172 § 3.6bis).</p>
 *
 * <p>Vanilla background music is kept suppressed each tick (no mixin needed).</p>
 */
public final class CsMusicPlayer {

    /** Two related tracks share a bar: a long joint would let their timbres drift audibly apart. */
    private static final long SYNCED_CROSSFADE_MS = 1_000L;
    /** Two unrelated tracks overlapping. Long on purpose - this window is also the revive window. */
    private static final long CROSSFADE_MS = 5_000L;
    /** Nothing to cross with: entering from silence, or leaving to silence. */
    private static final long FADE_MS = 2_500L;
    /** Retiring voices hold an OpenAL source until their ramp ends; five-second ramps stack up. */
    private static final int MAX_RETIRING = 3;
    /** ~5 s: ample for a stream to finish opening, and a hard bound if the instance never clears. */
    private static final int VANILLA_KILL_TICKS = 100;

    private static final Random RANDOM = new Random();

    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CsMusic-Loader");
        t.setDaemon(true);
        return t;
    });

    /** Identity of the instruction being played ({@code #route} for a pool), for deduplication. */
    @Nullable
    private static String currentInstructionId = null;
    @Nullable
    private static String currentTrackId = null;       // entry actually playing
    private static CsMusicVoice currentVoice = null;
    @Nullable
    private static ResourceLocation currentOutroId = null;
    @Nullable
    private static String currentParent = null;        // parent of the playing entry (§ 3.6bis)

    // Playlist state of the current instruction. A single track is a playlist of one, repeat 0.
    private static List<SetCsMusicPayload.Entry> playlistEntries = List.of();
    private static int playlistRepeat = 0;
    private static int playlistIndex = -1;
    private static int playlistNextIndex = -1;
    /** Set once the pre-boundary transition has fired for the current entry, so it fires once. */
    private static boolean boundaryTriggered = false;

    private static final List<Retired> retiring = new ArrayList<>();
    // Bumped on every load/outro so a late async callback can tell it has been superseded.
    private static final AtomicLong GENERATION = new AtomicLong();
    private static boolean wasPaused = false;

    /** A voice on its way out, with everything needed to bring it back (§ 3.10). */
    private record Retired(String instructionId,
                           @Nullable String trackId,
                           CsMusicVoice voice,
                           @Nullable ResourceLocation outro,
                           @Nullable String parent,
                           List<SetCsMusicPayload.Entry> entries,
                           int repeat,
                           int index,
                           int nextIndex,
                           boolean boundaryTriggered) {}

    /**
     * An instruction resolved down to the entry about to start, carried through the asynchronous
     * decode. It is <b>not</b> committed to the fields above until {@link #startVoice} actually
     * produces a voice: between the request and the decode the audible track is still the previous
     * one, and the state has to keep saying so.
     */
    private record Pending(String instructionId,
                           List<SetCsMusicPayload.Entry> entries,
                           int repeat,
                           int index,
                           int nextIndex) {
        SetCsMusicPayload.Entry entry() {
            return entries.get(index);
        }
    }

    /** Decoded bytes of the next playlist entry, so an entry without an outro still starts gapless. */
    private record Preloaded(String entryId, byte @Nullable [] intro, byte[] loop) {}

    @Nullable
    private static volatile Preloaded preloaded = null;

    // Vanilla track we asked to stop, re-stopped until the sound engine really lets it go, plus the
    // edge tracker that captures it exactly once when csmusic takes the music channel over.
    @Nullable
    private static SoundInstance vanillaMusicToKill = null;
    private static int vanillaKillTicksLeft = 0;
    private static boolean wasSuppressing = false;

    // Outro state: while the outgoing track's outro one-shot plays, hold the next instruction.
    private static boolean inOutro = false;
    @Nullable
    private static SetCsMusicPayload pendingAfterOutro = null;
    /** The outro running is a playlist advance, not a server instruction: continue the playlist. */
    private static boolean advanceAfterOutro = false;

    private CsMusicPlayer() {}

    // --- Server receive (runs on the client thread) ---------------------------------------------

    public static void accept(SetCsMusicPayload payload) {
        int mode = payload.outgoingMode();

        if (mode == SetCsMusicPayload.MODE_OUTRO && currentVoice != null && currentOutroId != null && !inOutro) {
            beginOutro(payload, false);
            return;
        }
        if (inOutro) {
            // A new instruction interrupts the outro.
            inOutro = false;
            pendingAfterOutro = null;
            advanceAfterOutro = false;
            if (currentVoice != null) {
                currentVoice.requestStop();
                currentVoice = null;
            }
            clearCurrent();
        }

        if (!payload.hasTrack() || payload.entries().isEmpty()) {
            stopInto(mode);
            return;
        }
        if (payload.id().equals(currentInstructionId) && currentVoice != null
                && !currentVoice.isFinished() && playingOneOf(payload.entries())) {
            // Already audible. Bumping the generation also cancels a load still in flight for some
            // other instruction: this is the fast round trip, where the player left and came back
            // before the track they left for had even finished decoding.
            GENERATION.incrementAndGet();
            return;
        }
        // A round trip inside the fade window brings the previous instruction back where it was,
        // instead of decoding a second copy of it and restarting it from zero.
        if ((mode == SetCsMusicPayload.MODE_FADE || mode == SetCsMusicPayload.MODE_CROSSFADE)
                && tryRevive(payload.id())) {
            return;
        }
        applyInstruction(payload, mode);
    }

    private static void applyInstruction(SetCsMusicPayload payload, int mode) {
        List<SetCsMusicPayload.Entry> entries = payload.entries();
        int start = entries.size() < 2 ? 0 : RANDOM.nextInt(entries.size());
        startEntry(new Pending(payload.id(), entries, Math.max(0, payload.repeat()),
                start, pickNext(entries, start)), mode, payload.startMs(), 0L);
    }

    /**
     * True when the track actually coming out of the speakers is one of {@code entries}. The
     * instruction id alone is not enough to skip a payload: if the two ever disagree, trusting the
     * id would drop every further packet for that instruction and leave the client stuck on the
     * wrong track with nothing to correct it. Checking what is audible makes the divergence
     * self-healing - the payload falls through and the client converges on what the server asked.
     */
    private static boolean playingOneOf(List<SetCsMusicPayload.Entry> entries) {
        if (currentTrackId == null) {
            return false;
        }
        for (SetCsMusicPayload.Entry e : entries) {
            if (currentTrackId.equals(e.id())) {
                return true;
            }
        }
        return false;
    }

    /** Never the entry just played, as long as there is anything else to play. */
    private static int pickNext(List<SetCsMusicPayload.Entry> entries, int current) {
        int size = entries.size();
        if (size < 2) {
            return current;
        }
        int offset = 1 + RANDOM.nextInt(size - 1);
        return (current + offset) % size;
    }

    /** A pool of one has nothing to shuffle to, so it loops forever whatever the server asked. */
    private static int effectiveRepeat(List<SetCsMusicPayload.Entry> entries, int repeat) {
        return entries.size() < 2 ? 0 : repeat;
    }

    private static int effectiveRepeat() {
        return effectiveRepeat(playlistEntries, playlistRepeat);
    }

    // --- Voice construction / transitions --------------------------------------------------------

    /**
     * @param fadeMs explicit ramp duration, or {@code 0} to derive it from the mode and from whether
     *               there is anything live to cross with
     */
    private static void startEntry(Pending next, int mode, int startMs, long fadeMs) {
        final SetCsMusicPayload.Entry entry = next.entry();
        final int effectiveMode = promoteIfRelated(entry, mode);
        final long gen = GENERATION.incrementAndGet();
        final Minecraft mc = Minecraft.getInstance();

        final ResourceLocation loopFile = resolveOggFile(entry.loop());
        final ResourceLocation introFile = entry.intro() != null ? resolveOggFile(entry.intro()) : null;
        if (loopFile == null) {
            CobbleSafari.LOGGER.error("[CSMusic] cannot resolve loop sound {}", entry.loop());
            return;
        }

        Preloaded ready = preloaded;
        if (ready != null && ready.entryId().equals(entry.id())) {
            preloaded = null;
            startVoice(next, ready.intro(), ready.loop(), effectiveMode, startMs, fadeMs);
            prefetchNext();
            return;
        }

        LOADER.submit(() -> {
            byte[] loopBytes = readOggBytes(loopFile);
            byte[] introBytes = introFile != null ? readOggBytes(introFile) : null;
            mc.execute(() -> {
                if (gen != GENERATION.get()) {
                    return; // superseded
                }
                if (loopBytes == null) {
                    CobbleSafari.LOGGER.error("[CSMusic] loop bytes unavailable for {}", entry.id());
                    return;
                }
                startVoice(next, introBytes, loopBytes, effectiveMode, startMs, fadeMs);
                prefetchNext();
            });
        });
    }

    /**
     * The server decides the base mode, but inside a pool it does not know which entry is playing,
     * so it cannot see a {@code parent} relation. This class can, and a relation always outranks the
     * priority-based choice - related tracks share a bar and must be joined at the playhead.
     * One-way by design: a received mode is never downgraded.
     */
    private static int promoteIfRelated(SetCsMusicPayload.Entry incoming, int mode) {
        if (mode == SetCsMusicPayload.MODE_CROSSFADE || currentTrackId == null
                || currentVoice == null || currentVoice.isFinished()) {
            return mode;
        }
        boolean related = currentTrackId.equals(incoming.parent())
                || incoming.id().equals(currentParent)
                || (currentParent != null && currentParent.equals(incoming.parent()));
        return related ? SetCsMusicPayload.MODE_CROSSFADE : mode;
    }

    private static void startVoice(Pending next, byte @Nullable [] introBytes,
                                   byte[] loopBytes, int mode, int startMs, long fadeMs) {
        SetCsMusicPayload.Entry entry = next.entry();
        CsMusicAudioStream loopStream;
        CsMusicAudioStream introStream = null;
        try {
            loopStream = new CsMusicAudioStream(loopBytes);
            if (introBytes != null) {
                introStream = new CsMusicAudioStream(introBytes);
            }
        } catch (RuntimeException e) {
            CobbleSafari.LOGGER.error("[CSMusic] decode failed for {}", entry.id(), e);
            return;
        }

        int effectiveMode = mode;
        if (effectiveMode == SetCsMusicPayload.MODE_OUTRO) {
            effectiveMode = SetCsMusicPayload.MODE_FADE; // outro handled separately; here just fade
        }

        long startLoopMs = startMs;
        boolean useIntro = introStream != null;
        boolean hasLiveOutgoing = currentVoice != null && !currentVoice.isFinished();

        if (effectiveMode == SetCsMusicPayload.MODE_CROSSFADE) {
            if (hasLiveOutgoing) {
                startLoopMs = currentVoice.loopPositionMs(); // sync child to parent's playhead
                useIntro = false;
                if (introStream != null) {
                    introStream.close();
                    introStream = null;
                }
            } else {
                effectiveMode = SetCsMusicPayload.MODE_CUT; // nothing to sync to
            }
        }

        CsMusicAudioStream[] segments = useIntro
                ? new CsMusicAudioStream[]{introStream, loopStream}
                : new CsMusicAudioStream[]{loopStream};

        boolean fadingIn = effectiveMode == SetCsMusicPayload.MODE_FADE
                || effectiveMode == SetCsMusicPayload.MODE_CROSSFADE;
        float startEnv = fadingIn ? 0f : 1f;

        int alSource = AL10.alGenSources();
        if (alSource == 0 || AL10.alGetError() != AL10.AL_NO_ERROR) {
            CobbleSafari.LOGGER.error("[CSMusic] no free OpenAL source for {} - track skipped", entry.id());
            for (CsMusicAudioStream segment : segments) {
                segment.close();     // otherwise the decoded streams leak native memory
            }
            return;
        }
        // From the pending instruction, not the committed one: the commit happens below, once the
        // voice exists, so until then every field still describes the track that is still playing.
        CsMusicVoice voice = new CsMusicVoice(alSource, segments,
                effectiveRepeat(next.entries(), next.repeat()), startLoopMs, startEnv, musicVolume());

        long ramp = fadeMs > 0L ? fadeMs : defaultRamp(effectiveMode, hasLiveOutgoing);
        switch (effectiveMode) {
            case SetCsMusicPayload.MODE_CROSSFADE, SetCsMusicPayload.MODE_FADE -> {
                voice.rampTo(1f, ramp, false);
                retireCurrent(ramp);
            }
            default -> { // MODE_CUT
                if (currentVoice != null) {
                    currentVoice.requestStop();
                    currentVoice = null;
                }
            }
        }

        voice.start();
        // wasPaused only tracks *changes*, so a voice created while the game is already paused
        // would otherwise start playing behind the pause menu.
        if (Minecraft.getInstance().isPaused()) {
            voice.setPaused(true);
        }
        // Commit point. Everything above still described the outgoing track on purpose: an
        // instruction that never got as far as a voice must leave no trace, or a player who turns
        // back during the decode is told they are already elsewhere and a second copy of the track
        // they never left gets cross-ramped against the first.
        currentVoice = voice;
        currentInstructionId = next.instructionId();
        playlistEntries = next.entries();
        playlistRepeat = next.repeat();
        playlistIndex = next.index();
        playlistNextIndex = next.nextIndex();
        currentTrackId = entry.id();
        currentOutroId = entry.outro();
        currentParent = entry.parent();
        boundaryTriggered = false;
    }

    private static long defaultRamp(int mode, boolean hasLiveOutgoing) {
        if (mode == SetCsMusicPayload.MODE_CROSSFADE) {
            return SYNCED_CROSSFADE_MS;
        }
        return hasLiveOutgoing ? CROSSFADE_MS : FADE_MS;
    }

    private static void retireCurrent(long fadeMs) {
        if (currentVoice == null) {
            return;
        }
        // At most one retiring voice per instruction. Bouncing across a biome border otherwise
        // stacks several, and the revive would have a stale one to choose from - the source of
        // "it came back at the wrong place, quietly, over five seconds".
        String id = String.valueOf(currentInstructionId);
        retiring.removeIf(r -> {
            if (!r.instructionId().equals(id)) {
                return false;
            }
            r.voice().requestStop();
            return true;
        });
        currentVoice.rampTo(0f, fadeMs, true);
        retiring.add(new Retired(id, currentTrackId, currentVoice,
                currentOutroId, currentParent, playlistEntries, playlistRepeat,
                playlistIndex, playlistNextIndex, boundaryTriggered));
        currentVoice = null;
        // Five-second ramps hold their OpenAL source five times longer than one-second ramps did;
        // a player pacing an area border would otherwise reach alGenSources exhaustion, whose
        // failure mode is a silently skipped track.
        while (retiring.size() > MAX_RETIRING) {
            retiring.remove(0).voice().requestStop();
        }
    }

    private static void stopInto(int mode) {
        // Silence has to cancel a decode in flight too, or the track we are silencing for would
        // start playing a moment after the fade-out began.
        GENERATION.incrementAndGet();
        if (currentVoice != null) {
            if (mode == SetCsMusicPayload.MODE_FADE
                    || mode == SetCsMusicPayload.MODE_OUTRO
                    || mode == SetCsMusicPayload.MODE_CROSSFADE) {
                retireCurrent(FADE_MS); // leaving to silence: nothing to cross with
            } else {
                currentVoice.requestStop();
                currentVoice = null;
            }
        }
        clearCurrent();
        clearPlaylist();
    }

    private static void clearCurrent() {
        currentInstructionId = null;
        currentTrackId = null;
        currentOutroId = null;
        currentParent = null;
        boundaryTriggered = false;
    }

    private static void clearPlaylist() {
        playlistEntries = List.of();
        playlistRepeat = 0;
        playlistIndex = -1;
        playlistNextIndex = -1;
        preloaded = null;
    }

    /** Stops every voice at once. Safe to call from a resource-reload / device-change hook. */
    public static void forceStop() {
        GENERATION.incrementAndGet(); // nothing decoding may land after this
        if (currentVoice != null) {
            currentVoice.requestStop();
            currentVoice = null;
        }
        for (Retired r : retiring) {
            r.voice().requestStop();
        }
        retiring.clear();
        clearCurrent();
        clearPlaylist();
        inOutro = false;
        pendingAfterOutro = null;
        advanceAfterOutro = false;
        vanillaMusicToKill = null;
        vanillaKillTicksLeft = 0;
        wasSuppressing = false;
    }

    // --- Revive (§ 3.10) -------------------------------------------------------------------------

    /**
     * Brings back an instruction still fading out. The playlist snapshot travels with the voice, so
     * a pool resumes on the same entry and - since {@code playsDone} is untouched inside the voice -
     * on the same loop pass.
     */
    private static boolean tryRevive(String instructionId) {
        // Newest first. An older entry for the same instruction is further into its fade and sits
        // at a playhead the player left long ago; resuming it sounds like the track restarting
        // somewhere random and creeping back up over several seconds.
        for (int i = retiring.size() - 1; i >= 0; i--) {
            Retired r = retiring.get(i);
            if (!r.instructionId().equals(instructionId) || r.voice().isFinished()) {
                continue;
            }
            // Losing the race with the pump thread is not an error, just a fall-through to a
            // normal load: the fade had already run out.
            if (!r.voice().revive() || r.voice().isFinished()) {
                return false;
            }
            retiring.remove(i);
            retireCurrent(CROSSFADE_MS);   // the incoming instruction goes the other way
            // Any async load still in flight for the instruction we are abandoning must not land.
            GENERATION.incrementAndGet();
            currentVoice = r.voice();
            currentInstructionId = r.instructionId();
            currentTrackId = r.trackId();
            currentOutroId = r.outro();
            currentParent = r.parent();
            playlistEntries = r.entries();
            playlistRepeat = r.repeat();
            playlistIndex = r.index();
            playlistNextIndex = r.nextIndex();
            boundaryTriggered = r.boundaryTriggered();
            return true;
        }
        return false;
    }

    // --- Outro one-shot --------------------------------------------------------------------------

    private static void beginOutro(@Nullable SetCsMusicPayload pending, boolean advance) {
        ResourceLocation outroFile = resolveOggFile(currentOutroId);
        if (outroFile == null) {
            afterOutro(pending, advance); // no usable outro asset - go straight to what comes next
            return;
        }
        if (currentVoice != null) {
            currentVoice.requestStop();
            currentVoice = null;
        }
        currentTrackId = null;
        currentOutroId = null;
        currentParent = null;
        if (!advance) {
            currentInstructionId = null;
        }
        inOutro = true;
        pendingAfterOutro = pending;
        advanceAfterOutro = advance;

        final long gen = GENERATION.incrementAndGet();
        final Minecraft mc = Minecraft.getInstance();
        LOADER.submit(() -> {
            byte[] bytes = readOggBytes(outroFile);
            mc.execute(() -> {
                if (gen != GENERATION.get()) {
                    return;
                }
                if (bytes == null) {
                    finishOutro();
                    return;
                }
                CsMusicAudioStream outro;
                try {
                    outro = new CsMusicAudioStream(bytes);
                } catch (RuntimeException e) {
                    CobbleSafari.LOGGER.error("[CSMusic] outro decode failed", e);
                    finishOutro();
                    return;
                }
                int src = AL10.alGenSources();
                if (src == 0 || AL10.alGetError() != AL10.AL_NO_ERROR) {
                    CobbleSafari.LOGGER.error("[CSMusic] no free OpenAL source for outro - skipped");
                    outro.close();
                    finishOutro();
                    return;
                }
                CsMusicVoice voice = new CsMusicVoice(src,
                        new CsMusicAudioStream[]{outro}, 1, 0L, 1f, musicVolume());
                voice.start();
                if (mc.isPaused()) {
                    voice.setPaused(true);
                }
                currentVoice = voice;
            });
        });
    }

    private static void finishOutro() {
        inOutro = false;
        SetCsMusicPayload pending = pendingAfterOutro;
        boolean advance = advanceAfterOutro;
        pendingAfterOutro = null;
        advanceAfterOutro = false;
        if (currentVoice != null) {
            currentVoice.requestStop();
            currentVoice = null;
        }
        afterOutro(pending, advance);
    }

    private static void afterOutro(@Nullable SetCsMusicPayload pending, boolean advance) {
        if (advance) {
            advancePlaylist();
            return;
        }
        if (pending == null || !pending.hasTrack() || pending.entries().isEmpty()) {
            clearCurrent();
            clearPlaylist();
            return;
        }
        // Start the pending instruction; its predecessor's outro already played, so the entry
        // either cuts in on its intro or fades its loop up from silence (§ 3.8, rows 1 and 2).
        applyInstruction(pending, entryModeAfterOutro(pending.entries().get(0)));
    }

    /** After an outro there is nothing left to cross with: an intro cuts in, a bare loop fades in. */
    private static int entryModeAfterOutro(SetCsMusicPayload.Entry next) {
        return next.intro() != null ? SetCsMusicPayload.MODE_CUT : SetCsMusicPayload.MODE_FADE;
    }

    // --- Playlist advance (§ 3.8) ----------------------------------------------------------------

    /**
     * Fires once per entry, a little before its final loop reaches its end, for the two rows of the
     * matrix where the outgoing entry has no outro and the overlap has to be taken out of it.
     */
    private static void tickBoundary() {
        if (boundaryTriggered || currentVoice == null || inOutro
                || playlistEntries.size() < 2 || effectiveRepeat() <= 0
                || currentOutroId != null || !currentVoice.isFinalLoop()) {
            return;
        }
        long remaining = currentVoice.remainingInLoopMs();
        if (remaining < 0L) {
            return;
        }
        SetCsMusicPayload.Entry next = playlistEntries.get(playlistNextIndex);
        boolean nextHasIntro = next.intro() != null;
        long nominal = nextHasIntro ? FADE_MS : CROSSFADE_MS;
        // Never eat more than half the loop: the final pass must still be heard at full volume,
        // and a fade can then never need to start before the loop it rides on.
        long loopMs = currentVoice.loopDurationMs();
        long overlap = loopMs > 0L ? Math.min(nominal, loopMs / 2L) : nominal;
        if (remaining > overlap) {
            return;
        }
        boundaryTriggered = true;
        if (nextHasIntro) {
            // Row 3: the outgoing loop bows out on its own tail, the intro cuts in at the seam.
            // No andStop - the voice ends by itself when its final pass runs out.
            currentVoice.rampTo(0f, overlap, false);
        } else {
            // Row 4: a true crossfade, so the incoming loop starts before the seam.
            advanceTo(playlistNextIndex, SetCsMusicPayload.MODE_FADE, overlap);
        }
    }

    /** The current entry has ended on its own. */
    private static void advancePlaylist() {
        if (playlistEntries.isEmpty()) {
            clearCurrent();
            clearPlaylist();
            return;
        }
        if (playlistEntries.size() < 2) {
            return; // nothing to draw; a single entry loops forever and never gets here
        }
        advanceTo(playlistNextIndex, SetCsMusicPayload.MODE_CUT, 0L);
    }

    private static void advanceTo(int index, int mode, long fadeMs) {
        if (index < 0 || index >= playlistEntries.size() || currentInstructionId == null) {
            return;
        }
        // Same instruction, next entry: the commit in startVoice only moves the indices along.
        startEntry(new Pending(currentInstructionId, playlistEntries, playlistRepeat,
                index, pickNext(playlistEntries, index)), mode, 0, fadeMs);
    }

    private static void prefetchNext() {
        if (effectiveRepeat() <= 0 || playlistNextIndex < 0 || playlistNextIndex >= playlistEntries.size()) {
            return;
        }
        SetCsMusicPayload.Entry next = playlistEntries.get(playlistNextIndex);
        Preloaded ready = preloaded;
        if (ready != null && ready.entryId().equals(next.id())) {
            return;
        }
        // Sound-event resolution needs the client thread; only the file read goes to the loader.
        final ResourceLocation loopFile = resolveOggFile(next.loop());
        final ResourceLocation introFile = next.intro() != null ? resolveOggFile(next.intro()) : null;
        if (loopFile == null) {
            return;
        }
        final String entryId = next.id();
        LOADER.submit(() -> {
            byte[] loopBytes = readOggBytes(loopFile);
            byte[] introBytes = introFile != null ? readOggBytes(introFile) : null;
            if (loopBytes != null) {
                preloaded = new Preloaded(entryId, introBytes, loopBytes);
            }
        });
    }

    // --- Client tick -----------------------------------------------------------------------------

    /**
     * {@code true} while csmusic owns the music channel (a voice is playing, one is fading out, or an
     * outro one-shot is running). Vanilla background music must stay suppressed for as long as this
     * holds - {@code MusicManagerConstantMusicMixin} reads it to cancel {@code MusicManager.tick}.
     */
    public static boolean isSuppressingVanillaMusic() {
        return currentVoice != null || !retiring.isEmpty() || inOutro;
    }

    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            if (isSuppressingVanillaMusic()) {
                forceStop();
            }
            wasSuppressing = false;
            return;
        }

        if (!isSuppressingVanillaMusic()) {
            // Keep chasing a still-unkilled orphan even once csmusic is done: the music manager has
            // already forgotten that instance, so any track it starts from now on is a different one.
            tickVanillaKill(mc);
            wasSuppressing = false;
            return;
        }

        // Priority over vanilla background music (jukebox on SoundSource.RECORDS is untouched).
        MusicManager musicManager = mc.getMusicManager();
        if (!wasSuppressing) {
            // Capture before stopping: stopPlaying() nulls the field, and a track stopped while its OGG
            // stream is still loading survives the stop (the sound engine plays it from a deferred
            // callback), so the instance is needed to keep hitting it until it is really gone.
            vanillaMusicToKill = ((MusicManagerAccessor) musicManager).cobblesafari$getCurrentMusic();
            vanillaKillTicksLeft = vanillaMusicToKill != null ? VANILLA_KILL_TICKS : 0;
            wasSuppressing = true;
        }
        musicManager.stopPlaying();
        tickVanillaKill(mc);

        boolean paused = mc.isPaused();
        if (paused != wasPaused) {
            wasPaused = paused;
            if (currentVoice != null) {
                currentVoice.setPaused(paused);
            }
            for (Retired r : retiring) {
                r.voice().setPaused(paused);
            }
        }

        tickBoundary();

        float vol = musicVolume();
        if (currentVoice != null) {
            currentVoice.setVolume(vol);
            if (currentVoice.isFinished()) {
                if (inOutro) {
                    finishOutro();
                } else {
                    currentVoice = null;
                    onCurrentEntryEnded();
                }
            }
        }
        retiring.removeIf(r -> {
            if (r.voice().isFinished()) {
                return true;
            }
            r.voice().setVolume(vol);
            return false;
        });
    }

    /** The playing entry ran out of loop passes: continue the playlist, or fall silent. */
    private static void onCurrentEntryEnded() {
        if (playlistEntries.size() > 1 && effectiveRepeat() > 0) {
            if (currentOutroId != null) {
                beginOutro(null, true); // rows 1 and 2: conclude, then draw
                return;
            }
            advancePlaylist();          // row 3: the loop already bowed out on its own tail
            return;
        }
        clearCurrent();
        clearPlaylist();
    }

    private static void tickVanillaKill(Minecraft mc) {
        if (vanillaMusicToKill == null) {
            return;
        }
        if (--vanillaKillTicksLeft <= 0 || !mc.getSoundManager().isActive(vanillaMusicToKill)) {
            vanillaMusicToKill = null;      // really stopped, or gave up
            return;
        }
        mc.getSoundManager().stop(vanillaMusicToKill);
    }

    // --- Resolution / IO -------------------------------------------------------------------------

    /** Resolve a csmusic sound-event id (respecting {@code sounds.json} pools) to its {@code .ogg} file. */
    @Nullable
    private static ResourceLocation resolveOggFile(@Nullable ResourceLocation soundEventId) {
        if (soundEventId == null) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        SoundEvent event = SoundEvent.createVariableRangeEvent(soundEventId);
        SimpleSoundInstance instance = SimpleSoundInstance.forMusic(event);
        instance.resolve(mc.getSoundManager());
        Sound sound = instance.getSound();
        if (sound == null || sound == SoundManager.EMPTY_SOUND) {
            CobbleSafari.LOGGER.warn("[CSMusic] unknown / unresolved sound event {}", soundEventId);
            return null;
        }
        ResourceLocation loc = sound.getLocation();
        return ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "sounds/" + loc.getPath() + ".ogg");
    }

    @Nullable
    private static byte[] readOggBytes(ResourceLocation file) {
        Minecraft mc = Minecraft.getInstance();
        var resource = mc.getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            CobbleSafari.LOGGER.error("[CSMusic] OGG resource missing {}", file);
            return null;
        }
        try (InputStream in = resource.get().open()) {
            return in.readAllBytes();
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[CSMusic] OGG read failed {}", file, e);
            return null;
        }
    }

    private static float musicVolume() {
        Options options = Minecraft.getInstance().options;
        return options.getSoundSourceVolume(SoundSource.MUSIC) * options.getSoundSourceVolume(SoundSource.MASTER);
    }
}
