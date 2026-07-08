package maxigregrze.cobblesafari.client.audio;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.network.SetCsMusicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client csmusic player built on a custom OpenAL backend ({@link CsMusicVoice}) instead of the
 * vanilla mixer, so it can seek to an arbitrary offset and run two voices at once. This is what
 * makes the <b>synced crossfade</b> possible: the child ({@link SetCsMusicPayload#MODE_CROSSFADE})
 * starts at the parent's exact loop playhead and both cross-ramp over ~1&nbsp;s. Also supports a
 * one-shot <b>outro</b> ({@link SetCsMusicPayload#MODE_OUTRO}) played before the next track.
 *
 * <p>Server-authoritative: reacts to {@link SetCsMusicPayload}. Vanilla background music is kept
 * suppressed each tick (no mixin needed).</p>
 */
public final class CsMusicPlayer {

    private static final long CROSSFADE_MS = 1000L;
    private static final long FADE_MS = 1000L;

    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CsMusic-Loader");
        t.setDaemon(true);
        return t;
    });

    private static String currentId = null;
    private static CsMusicVoice currentVoice = null;
    @Nullable
    private static ResourceLocation currentOutroId = null;      // outro sound event of the current track
    private static final List<CsMusicVoice> retiring = new ArrayList<>();
    private static volatile long generation = 0L;
    private static boolean wasPaused = false;

    // Outro state: while the outgoing track's outro one-shot plays, hold the next instruction.
    private static boolean inOutro = false;
    @Nullable
    private static SetCsMusicPayload pendingAfterOutro = null;

    private CsMusicPlayer() {}

    // --- Server receive (runs on the client thread) ---------------------------------------------

    public static void accept(SetCsMusicPayload payload) {
        int mode = payload.outgoingMode();

        if (mode == SetCsMusicPayload.MODE_OUTRO && currentVoice != null && currentOutroId != null && !inOutro) {
            beginOutro(payload);
            return;
        }
        if (inOutro) {
            // A new instruction interrupts the outro.
            inOutro = false;
            pendingAfterOutro = null;
            if (currentVoice != null) {
                currentVoice.requestStop();
                currentVoice = null;
            }
            currentId = null;
            currentOutroId = null;
        }

        if (!payload.hasTrack()) {
            stopInto(mode);
            return;
        }
        if (payload.id().equals(currentId) && currentVoice != null && !currentVoice.isFinished()) {
            return; // already playing this track
        }
        loadAndApply(payload);
    }

    private static void loadAndApply(SetCsMusicPayload payload) {
        final long gen = ++generation;
        final String id = payload.id();
        final int mode = payload.outgoingMode();
        final int startMs = payload.startMs();
        final ResourceLocation outroId = payload.outro();
        final Minecraft mc = Minecraft.getInstance();

        final ResourceLocation loopFile = resolveOggFile(payload.loop());
        final ResourceLocation introFile = payload.intro() != null ? resolveOggFile(payload.intro()) : null;
        if (loopFile == null) {
            CobbleSafari.LOGGER.error("[CSMusic] cannot resolve loop sound {}", payload.loop());
            return;
        }

        LOADER.submit(() -> {
            byte[] loopBytes = readOggBytes(loopFile);
            byte[] introBytes = introFile != null ? readOggBytes(introFile) : null;
            mc.execute(() -> {
                if (gen != generation) {
                    return; // superseded
                }
                if (loopBytes == null) {
                    CobbleSafari.LOGGER.error("[CSMusic] loop bytes unavailable for {}", id);
                    return;
                }
                startVoice(id, introBytes, loopBytes, outroId, mode, startMs);
            });
        });
    }

    // --- Voice construction / transitions --------------------------------------------------------

    private static void startVoice(String id, @Nullable byte[] introBytes, byte[] loopBytes,
                                   @Nullable ResourceLocation outroId, int mode, int startMs) {
        CsMusicAudioStream loopStream;
        CsMusicAudioStream introStream = null;
        try {
            loopStream = new CsMusicAudioStream(loopBytes);
            if (introBytes != null) {
                introStream = new CsMusicAudioStream(introBytes);
            }
        } catch (RuntimeException e) {
            CobbleSafari.LOGGER.error("[CSMusic] decode failed for {}", id, e);
            return;
        }

        int effectiveMode = mode;
        if (effectiveMode == SetCsMusicPayload.MODE_OUTRO) {
            effectiveMode = SetCsMusicPayload.MODE_FADE; // outro handled separately; here just fade
        }

        long startLoopMs = startMs;
        boolean useIntro = introStream != null;

        if (effectiveMode == SetCsMusicPayload.MODE_CROSSFADE) {
            if (currentVoice != null && !currentVoice.isFinished()) {
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
        CsMusicVoice voice = new CsMusicVoice(alSource, segments, true, startLoopMs, startEnv, musicVolume());

        switch (effectiveMode) {
            case SetCsMusicPayload.MODE_CROSSFADE -> {
                voice.rampTo(1f, CROSSFADE_MS, false);
                retireCurrent(CROSSFADE_MS);
            }
            case SetCsMusicPayload.MODE_FADE -> {
                voice.rampTo(1f, FADE_MS, false);
                retireCurrent(FADE_MS);
            }
            default -> { // MODE_CUT
                if (currentVoice != null) {
                    currentVoice.requestStop();
                    currentVoice = null;
                }
            }
        }

        voice.start();
        currentVoice = voice;
        currentId = id;
        currentOutroId = outroId;
    }

    private static void retireCurrent(long fadeMs) {
        if (currentVoice != null) {
            currentVoice.rampTo(0f, fadeMs, true);
            retiring.add(currentVoice);
            currentVoice = null;
        }
    }

    private static void stopInto(int mode) {
        if (currentVoice != null) {
            if (mode == SetCsMusicPayload.MODE_FADE
                    || mode == SetCsMusicPayload.MODE_OUTRO
                    || mode == SetCsMusicPayload.MODE_CROSSFADE) {
                retireCurrent(FADE_MS);
            } else {
                currentVoice.requestStop();
                currentVoice = null;
            }
        }
        currentId = null;
        currentOutroId = null;
    }

    private static void forceStop() {
        if (currentVoice != null) {
            currentVoice.requestStop();
            currentVoice = null;
        }
        for (CsMusicVoice v : retiring) {
            v.requestStop();
        }
        retiring.clear();
        currentId = null;
        currentOutroId = null;
        inOutro = false;
        pendingAfterOutro = null;
    }

    // --- Outro one-shot --------------------------------------------------------------------------

    private static void beginOutro(SetCsMusicPayload pending) {
        ResourceLocation outroFile = resolveOggFile(currentOutroId);
        if (outroFile == null) {
            applyPending(pending); // no usable outro asset — go straight to the next instruction
            return;
        }
        if (currentVoice != null) {
            currentVoice.requestStop();
            currentVoice = null;
        }
        currentId = null;
        currentOutroId = null;
        inOutro = true;
        pendingAfterOutro = pending;

        final long gen = ++generation;
        final Minecraft mc = Minecraft.getInstance();
        LOADER.submit(() -> {
            byte[] bytes = readOggBytes(outroFile);
            mc.execute(() -> {
                if (gen != generation) {
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
                CsMusicVoice voice = new CsMusicVoice(src,
                        new CsMusicAudioStream[]{outro}, false, 0L, 1f, musicVolume());
                voice.start();
                currentVoice = voice;
            });
        });
    }

    private static void finishOutro() {
        inOutro = false;
        SetCsMusicPayload pending = pendingAfterOutro;
        pendingAfterOutro = null;
        if (currentVoice != null) {
            currentVoice.requestStop();
            currentVoice = null;
        }
        applyPending(pending);
    }

    private static void applyPending(@Nullable SetCsMusicPayload pending) {
        if (pending == null || !pending.hasTrack()) {
            currentId = null;
            currentOutroId = null;
            return;
        }
        // Start the pending track with a hard cut (its outro already played).
        loadAndApply(SetCsMusicPayload.track(pending.id(), pending.intro(), pending.loop(),
                pending.outro(), SetCsMusicPayload.MODE_CUT, pending.startMs()));
    }

    // --- Client tick -----------------------------------------------------------------------------

    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            if (currentVoice != null || !retiring.isEmpty() || inOutro) {
                forceStop();
            }
            return;
        }

        if (currentVoice == null && retiring.isEmpty() && !inOutro) {
            return;
        }

        // Priority over vanilla background music (jukebox on SoundSource.RECORDS is untouched).
        mc.getMusicManager().stopPlaying();

        boolean paused = mc.isPaused();
        if (paused != wasPaused) {
            wasPaused = paused;
            if (currentVoice != null) {
                currentVoice.setPaused(paused);
            }
            for (CsMusicVoice v : retiring) {
                v.setPaused(paused);
            }
        }

        float vol = musicVolume();
        if (currentVoice != null) {
            currentVoice.setVolume(vol);
            if (currentVoice.isFinished()) {
                if (inOutro) {
                    finishOutro();
                } else {
                    currentVoice = null;
                    currentId = null;
                    currentOutroId = null;
                }
            }
        }
        retiring.removeIf(v -> {
            if (v.isFinished()) {
                return true;
            }
            v.setVolume(vol);
            return false;
        });
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
