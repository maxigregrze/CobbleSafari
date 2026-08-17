package maxigregrze.cobblesafari.client.audio;

import maxigregrze.cobblesafari.CobbleSafari;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * One non-positional OpenAL source driven by a background streaming thread. Plays a sequence of
 * segments - {@code [intro?, loop]} - where the last segment loops <b>gaplessly</b> (on end-of-stream
 * we {@code seekMs(0)} and keep feeding). {@code loopCount} bounds those plays: {@code 0} loops
 * forever, {@code 1} is a one-shot (the outro), and {@code n} ends the voice naturally after n
 * plays, which is what drives a tag playlist to its next entry. Carries a gain <b>envelope</b>
 * (0..1) for crossfades, multiplied by an externally-pushed {@code volume} (MUSIC × MASTER).
 *
 * <p>We drive our own source instead of the vanilla mixer so we can (a) seek to an arbitrary
 * offset - the synced crossfade starts the child at the parent's exact playhead - and (b) run
 * two voices at once with independent gain envelopes.</p>
 *
 * <p>The playhead is derived from <b>OpenAL</b> (consumed samples + {@code AL_SAMPLE_OFFSET}), not
 * from wall clock: an underrun (the pump recovers from those) or a pause would otherwise leave a
 * permanent phase error, and the synced crossfade reads this value.</p>
 *
 * <p>Threading: the pump thread owns all per-frame AL calls on this source, and publishes the
 * playhead through volatile fields. The client thread only calls {@link #setVolume},
 * {@link #rampTo}, {@link #setPaused}, {@link #requestStop} and reads
 * {@link #loopPositionMs}/{@link #isFinished}.</p>
 */
final class CsMusicVoice {

    private static final int BUFFER_BYTES = 32768;
    private static final int BUFFER_COUNT = 4;
    private static final long POLL_SLEEP_MS = 10L;
    /** Consecutive erroring iterations tolerated before assuming the AL context is gone. */
    private static final int MAX_AL_ERROR_STREAK = 20;

    private final int source;
    private final CsMusicAudioStream[] segments;
    /** Plays of the last segment: {@code 0} = forever, {@code 1} = one-shot, {@code n} = n plays. */
    private final int loopCount;
    private final long startLoopMs;
    /** Total duration of the leading (non-looping) segments, i.e. the intro. */
    private final long introDurationMs;
    private final Thread pump;

    private volatile boolean stopRequested = false;
    private volatile boolean finished = false;

    // Gain envelope (0..1). computeEnvelope() is pure; only rampTo() mutates the ramp params.
    private volatile float baseEnvelope;
    private volatile float rampFrom;
    private volatile float rampTo;
    private volatile long rampStartMs;
    private volatile long rampDurMs = 0L;
    private volatile boolean stopWhenSilent = false;

    // External volume (MUSIC × MASTER), 0..1.
    private volatile float volume;

    // Playhead published by the pump: samples consumed since the voice started, -1 before playback.
    private volatile long positionSamples = -1L;
    private volatile int positionRate = 0;

    private volatile boolean pausedFlag = false;

    // Pump-thread-only decode state.
    private int segIndex = 0;
    private int lastFormat;
    private int lastRate;
    private long playedSamples = 0L;
    /** Plays of the last segment started so far, 1-based. Pump-thread only. */
    private int playsDone = 1;

    CsMusicVoice(int source, CsMusicAudioStream[] segments, int loopCount,
                 long startLoopMs, float envelope, float volume) {
        this.source = source;
        this.segments = segments;
        this.loopCount = Math.max(0, loopCount);
        this.startLoopMs = Math.max(0L, startLoopMs);
        long intro = 0L;
        for (int i = 0; i < segments.length - 1; i++) {
            intro += Math.max(0L, segments[i].durationMs);
        }
        this.introDurationMs = intro;
        this.baseEnvelope = envelope;
        this.volume = volume;
        this.pump = new Thread(this::run, "CsMusic-Voice-" + source);
        this.pump.setDaemon(true);
    }

    void start() {
        pump.start();
    }

    void setVolume(float v) {
        this.volume = v;
    }

    /** Ramp the envelope to {@code target} over {@code durMs}; if {@code andStop}, free the voice at 0. */
    void rampTo(float target, long durMs, boolean andStop) {
        this.rampFrom = computeEnvelope();
        this.rampTo = target;
        this.rampStartMs = System.currentTimeMillis();
        this.rampDurMs = Math.max(1L, durMs);
        this.stopWhenSilent = andStop;
    }

    boolean isFinished() {
        return finished;
    }

    void requestStop() {
        stopRequested = true;
        pump.interrupt();
    }

    void setPaused(boolean p) {
        if (p == pausedFlag) {
            return;
        }
        pausedFlag = p;
        // No playhead bookkeeping needed: a paused source consumes no buffers and freezes its
        // sample offset, so the published position simply stops advancing.
        if (p) {
            safeSourcePause();
        } else {
            safeSourcePlay();
        }
    }

    /** Current loop playhead in ms (folded modulo the loop duration), derived from OpenAL. */
    long loopPositionMs() {
        long elapsed = loopElapsedMs();
        long dur = loopDurationMs();
        if (elapsed < 0L) {
            return startLoopMs;
        }
        return dur > 0L ? Math.floorMod(elapsed, dur) : elapsed;
    }

    /** Duration of the looping segment, or {@code -1} if the decoder could not report one. */
    long loopDurationMs() {
        return segments[segments.length - 1].durationMs;
    }

    /**
     * Milliseconds elapsed since the loop segment began, or {@code -1} while the intro is still
     * playing (or before playback starts). Derived from the OpenAL playhead rather than from
     * {@link #playsDone}, which runs up to four buffers ahead of what is audible.
     */
    private long loopElapsedMs() {
        long samples = positionSamples;
        int rate = positionRate;
        if (samples < 0L || rate <= 0) {
            return -1L;
        }
        long pos = startLoopMs + (samples * 1000L / rate) - introDurationMs;
        return pos < 0L ? -1L : pos;
    }

    /** True once the <b>audible</b> playhead has entered the last requested loop pass. */
    boolean isFinalLoop() {
        long dur = loopDurationMs();
        long elapsed = loopElapsedMs();
        if (loopCount <= 0 || dur <= 0L || elapsed < 0L) {
            return false;
        }
        return elapsed / dur >= (long) loopCount - 1L;
    }

    /** Milliseconds left before the loop currently playing ends, or {@code -1} if unknown. */
    long remainingInLoopMs() {
        long dur = loopDurationMs();
        long elapsed = loopElapsedMs();
        if (dur <= 0L || elapsed < 0L) {
            return -1L;
        }
        return dur - Math.floorMod(elapsed, dur);
    }

    /**
     * Reverses a fade-out in progress: the voice stops being scheduled for teardown and ramps back
     * to full. Returns {@code false} if the pump already tore it down, in which case the caller must
     * fall back to loading the track afresh.
     *
     * <p>The return ramp is <b>shortened in proportion to the ground left to cover</b>. A descent of
     * five seconds interrupted at the fourth leaves the envelope at 0.2; ramping back over another
     * five seconds would be four times slower than the descent and would sound like the track
     * struggling to come back. Reusing the in-flight ramp duration makes the round trip symmetric,
     * and needs no constant of its own.</p>
     */
    synchronized boolean revive() {
        if (finished || stopRequested) {
            return false;
        }
        float env = computeEnvelope();
        long back = rampDurMs > 0L ? Math.round(rampDurMs * (1.0 - env)) : 0L;
        rampTo(1f, Math.max(1L, back), false);
        return true;
    }

    private float computeEnvelope() {
        if (rampDurMs <= 0L) {
            return baseEnvelope;
        }
        float t = Math.min(1f, (System.currentTimeMillis() - rampStartMs) / (float) rampDurMs);
        return rampFrom + (rampTo - rampFrom) * t;
    }

    private boolean rampDone() {
        return rampDurMs <= 0L || System.currentTimeMillis() - rampStartMs >= rampDurMs;
    }

    private void run() {
        ByteBuffer pcm = MemoryUtil.memAlloc(BUFFER_BYTES);
        int[] buffers = new int[BUFFER_COUNT];
        try {
            AL10.alGenBuffers(buffers);
            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE); // we loop ourselves
            AL10.alSourcef(source, AL10.AL_GAIN, computeEnvelope() * volume);

            // Synced start: begin directly in the (single) loop segment at the requested offset.
            if (loopCount != 1 && startLoopMs > 0 && segments.length == 1) {
                segments[0].seekMs(startLoopMs);
            }

            int queued = 0;
            for (int i = 0; i < BUFFER_COUNT; i++) {
                if (decodeNext(pcm)) {
                    AL10.alBufferData(buffers[i], lastFormat, pcm, lastRate);
                    AL10.alSourceQueueBuffers(source, buffers[i]);
                    queued++;
                } else {
                    break;
                }
            }
            if (queued == 0) {
                return;
            }

            AL10.alSourcePlay(source);
            publishPosition();

            int errorStreak = 0;
            while (!stopRequested) {
                AL10.alSourcef(source, AL10.AL_GAIN, computeEnvelope() * volume);
                if (stopWhenSilent && rampDone() && computeEnvelope() <= 0.001f) {
                    break;
                }

                int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
                while (processed-- > 0 && !stopRequested) {
                    int buf = AL10.alSourceUnqueueBuffers(source);
                    playedSamples += samplesIn(buf);
                    if (decodeNext(pcm)) {
                        AL10.alBufferData(buf, lastFormat, pcm, lastRate);
                        AL10.alSourceQueueBuffers(source, buf);
                    }
                }

                if (!pausedFlag && AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_STOPPED) {
                    if (AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) > 0) {
                        AL10.alSourcePlay(source); // recover from an underrun
                    } else {
                        break; // natural end (one-shot / exhausted, non-looping)
                    }
                }

                publishPosition();

                // The AL context is destroyed when the sound engine reloads (F3+T) or the output
                // device changes; bail out instead of spinning on a dead context.
                if (!AL10.alIsSource(source)) {
                    CobbleSafari.LOGGER.debug("[CSMusic] voice {} source is gone - stopping", source);
                    break;
                }
                if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                    if (++errorStreak >= MAX_AL_ERROR_STREAK) {
                        CobbleSafari.LOGGER.warn("[CSMusic] voice {} stopped after {} consecutive AL errors",
                                source, errorStreak);
                        break;
                    }
                } else {
                    errorStreak = 0;
                }

                Thread.sleep(POLL_SLEEP_MS);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt(); // stop requested - restore the flag for teardown
        } catch (RuntimeException e) {
            CobbleSafari.LOGGER.error("[CSMusic] voice {} streaming error", source, e);
        } finally {
            teardown(pcm, buffers);
            finished = true;
        }
    }

    /** Publishes the playhead for the client thread: consumed samples + offset in the live buffer. */
    private void publishPosition() {
        if (lastRate <= 0) {
            return;
        }
        int offset = AL10.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET);
        positionRate = lastRate;
        positionSamples = playedSamples + Math.max(0, offset);
    }

    /** Sample count of an AL buffer, read back from the buffer itself (chunks are not all full). */
    private static long samplesIn(int buffer) {
        int bytes = AL10.alGetBufferi(buffer, AL10.AL_SIZE);
        int channels = AL10.alGetBufferi(buffer, AL10.AL_CHANNELS);
        int bits = AL10.alGetBufferi(buffer, AL10.AL_BITS);
        int frameBytes = Math.max(1, channels * bits / 8);
        return Math.max(0, bytes) / frameBytes;
    }

    /** Fills {@code pcm} with the next chunk, advancing segments and looping the last one. */
    private boolean decodeNext(ByteBuffer pcm) {
        pcm.clear();
        int guard = 0;
        while (guard++ < segments.length + 2) {
            CsMusicAudioStream seg = segments[segIndex];
            int read = seg.readPcm(pcm);
            if (read > 0) {
                lastFormat = seg.format;
                lastRate = seg.sampleRate;
                pcm.flip();
                return true;
            }
            boolean isLast = segIndex == segments.length - 1;
            if (isLast) {
                if (loopCount == 0) {
                    seg.seekMs(0);          // loops forever: gapless wrap, retry once more
                } else if (playsDone < loopCount) {
                    playsDone++;
                    seg.seekMs(0);          // one more requested pass
                } else {
                    return false;           // count exhausted: natural end of the voice
                }
            } else {
                segIndex++;
            }
        }
        return false;
    }

    private void teardown(ByteBuffer pcm, int[] buffers) {
        try {
            AL10.alSourceStop(source);
            int q = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
            if (q > 0) {
                AL10.alSourceUnqueueBuffers(source, new int[q]);
            }
            AL10.alDeleteBuffers(buffers);
            AL10.alDeleteSources(source);
        } catch (RuntimeException ignored) {
            // context may already be gone (world unload / sound engine reload)
        }
        MemoryUtil.memFree(pcm);
        for (CsMusicAudioStream s : segments) {
            try {
                s.close();
            } catch (RuntimeException ignored) {
                // best-effort
            }
        }
    }

    private void safeSourcePause() {
        try {
            AL10.alSourcePause(source);
        } catch (RuntimeException ignored) {
            // best-effort
        }
    }

    private void safeSourcePlay() {
        try {
            AL10.alSourcePlay(source);
        } catch (RuntimeException ignored) {
            // best-effort
        }
    }
}
