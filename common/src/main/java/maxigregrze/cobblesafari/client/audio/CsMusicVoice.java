package maxigregrze.cobblesafari.client.audio;

import maxigregrze.cobblesafari.CobbleSafari;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * One non-positional OpenAL source driven by a background streaming thread. Plays a sequence of
 * segments — {@code [intro?, loop]} — where the last segment loops <b>gaplessly</b> (on end-of-stream
 * we {@code seekMs(0)} and keep feeding). Carries a gain <b>envelope</b> (0..1) for crossfades,
 * multiplied by an externally-pushed {@code volume} (MUSIC × MASTER).
 *
 * <p>We drive our own source instead of the vanilla mixer so we can (a) seek to an arbitrary
 * offset — the synced crossfade starts the child at the parent's exact playhead — and (b) run
 * two voices at once with independent gain envelopes.</p>
 *
 * <p>Threading: the pump thread owns all per-frame AL calls on this source. The client thread only
 * calls {@link #setVolume}, {@link #rampTo}, {@link #setPaused}, {@link #requestStop} and reads
 * {@link #loopPositionMs}/{@link #isFinished} — all via volatile fields.</p>
 */
final class CsMusicVoice {

    private static final int BUFFER_BYTES = 32768;
    private static final int BUFFER_COUNT = 4;
    private static final long POLL_SLEEP_MS = 10L;

    private final int source;
    private final CsMusicAudioStream[] segments;
    private final boolean loopLast;
    private final long startLoopMs;
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

    // Wall-clock instant that maps to loop-playhead 0 (accounts for intro length + synced start).
    private volatile long loopAnchorWallMs = 0L;

    // Pause bookkeeping.
    private volatile boolean pausedFlag = false;
    private volatile long pauseStartMs = 0L;

    // Pump-thread-only decode state.
    private int segIndex = 0;
    private int lastFormat;
    private int lastRate;

    CsMusicVoice(int source, CsMusicAudioStream[] segments, boolean loopLast,
                 long startLoopMs, float envelope, float volume) {
        this.source = source;
        this.segments = segments;
        this.loopLast = loopLast;
        this.startLoopMs = Math.max(0L, startLoopMs);
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
        if (p) {
            pauseStartMs = System.currentTimeMillis();
            safeSourcePause();
        } else {
            loopAnchorWallMs += System.currentTimeMillis() - pauseStartMs; // keep playhead coherent
            safeSourcePlay();
        }
    }

    /** Current loop playhead in ms (folded modulo the loop duration). */
    long loopPositionMs() {
        long anchor = loopAnchorWallMs;
        if (anchor == 0L) {
            return startLoopMs;
        }
        long dur = segments[segments.length - 1].durationMs;
        long pos = System.currentTimeMillis() - anchor;
        return dur > 0 ? Math.floorMod(pos, dur) : Math.max(0L, pos);
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
            if (loopLast && startLoopMs > 0 && segments.length == 1) {
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

            // Anchor loop-playhead-0 to a wall-clock instant, derived from intro length so the
            // position query stays accurate regardless of how far ahead the decoder runs.
            long introDur = 0L;
            for (int i = 0; i < segments.length - 1; i++) {
                introDur += Math.max(0L, segments[i].durationMs);
            }
            loopAnchorWallMs = System.currentTimeMillis() + introDur - startLoopMs;

            AL10.alSourcePlay(source);

            while (!stopRequested) {
                AL10.alSourcef(source, AL10.AL_GAIN, computeEnvelope() * volume);
                if (stopWhenSilent && rampDone() && computeEnvelope() <= 0.001f) {
                    break;
                }

                int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
                while (processed-- > 0 && !stopRequested) {
                    int buf = AL10.alSourceUnqueueBuffers(source);
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

                Thread.sleep(POLL_SLEEP_MS);
            }
        } catch (InterruptedException ignored) {
            // stop requested
        } catch (RuntimeException e) {
            CobbleSafari.LOGGER.error("[CSMusic] voice {} streaming error", source, e);
        } finally {
            teardown(pcm, buffers);
            finished = true;
        }
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
                if (!loopLast) {
                    return false;
                }
                seg.seekMs(0); // gapless wrap, retry once more
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
