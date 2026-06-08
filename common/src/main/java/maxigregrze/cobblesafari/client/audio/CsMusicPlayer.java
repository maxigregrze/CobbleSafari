package maxigregrze.cobblesafari.client.audio;

import maxigregrze.cobblesafari.network.SetCsMusicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Client csmusic player (plan 105 § 7): intro → loop → outro state machine, with fade,
 * pending track, and priority over vanilla background music (jukebox exception, § 7.3).
 * Replaces the old {@code DungeonMusicHandler}.
 */
public final class CsMusicPlayer {

    private static final int FADE_TICKS = 20; // ~1 s

    private enum Phase { IDLE, INTRO, LOOP, OUTRO }

    private record TrackDef(String id, @Nullable ResourceLocation intro,
                            ResourceLocation loop, @Nullable ResourceLocation outro) {
        static TrackDef of(SetCsMusicPayload p) {
            return new TrackDef(p.id(), p.intro(), p.loop(), p.outro());
        }
    }

    private static Phase phase = Phase.IDLE;
    private static String currentId = null;
    private static TrackDef currentTrack = null;
    private static CsMusicSoundInstance current = null;
    private static TrackDef pending = null;
    private static boolean fading = false;

    private CsMusicPlayer() {}

    // --- Server receive --------------------------------------------------------

    public static void accept(SetCsMusicPayload payload) {
        TrackDef next = payload.hasTrack() ? TrackDef.of(payload) : null;

        // Already playing this track (intro/loop) ⇒ no-op.
        if (next != null && next.id.equals(currentId) && (phase == Phase.INTRO || phase == Phase.LOOP) && !fading) {
            return;
        }

        if (phase == Phase.IDLE || current == null) {
            startTrack(next);
            return;
        }

        switch (payload.outgoingMode()) {
            case SetCsMusicPayload.MODE_OUTRO -> {
                if (currentTrack != null && currentTrack.outro != null) {
                    pending = next;
                    startOutro();
                } else {
                    hardSwap(next);
                }
            }
            case SetCsMusicPayload.MODE_FADE -> {
                pending = next;
                fadeCurrent();
            }
            default -> hardSwap(next); // MODE_CUT
        }
    }

    // --- Tick client ---------------------------------------------------------

    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            if (phase != Phase.IDLE) {
                stopAll(mc);
            }
            return;
        }
        if (phase == Phase.IDLE) {
            return;
        }
        // Priority over vanilla background music (jukebox on SoundSource.RECORDS is untouched).
        mc.getMusicManager().stopPlaying();

        if (mc.isPaused()) {
            return;
        }

        if (fading) {
            if (current == null || !mc.getSoundManager().isActive(current)) {
                fading = false;
                TrackDef next = pending;
                pending = null;
                startTrack(next);
            }
            return;
        }

        if (current != null && !mc.getSoundManager().isActive(current)) {
            switch (phase) {
                case INTRO -> playLoop(mc);
                case OUTRO -> {
                    TrackDef next = pending;
                    pending = null;
                    startTrack(next);
                }
                case LOOP -> playLoop(mc); // safety: restart loop if stopped prematurely
                default -> { /* IDLE: nothing */ }
            }
        }
    }

    // --- Transitions ---------------------------------------------------------

    private static void startTrack(@Nullable TrackDef next) {
        Minecraft mc = Minecraft.getInstance();
        if (next == null) {
            stopAll(mc);
            return;
        }
        currentTrack = next;
        currentId = next.id;
        if (next.intro != null) {
            current = new CsMusicSoundInstance(next.intro, false);
            phase = Phase.INTRO;
        } else {
            current = new CsMusicSoundInstance(next.loop, true);
            phase = Phase.LOOP;
        }
        mc.getSoundManager().play(current);
    }

    private static void playLoop(Minecraft mc) {
        if (currentTrack == null) {
            stopAll(mc);
            return;
        }
        current = new CsMusicSoundInstance(currentTrack.loop, true);
        phase = Phase.LOOP;
        mc.getSoundManager().play(current);
    }

    private static void startOutro() {
        Minecraft mc = Minecraft.getInstance();
        if (current != null) {
            mc.getSoundManager().stop(current);
        }
        current = new CsMusicSoundInstance(currentTrack.outro, false);
        phase = Phase.OUTRO;
        mc.getSoundManager().play(current);
    }

    private static void fadeCurrent() {
        if (current != null) {
            current.fadeOut(FADE_TICKS);
            fading = true;
        } else {
            TrackDef next = pending;
            pending = null;
            startTrack(next);
        }
    }

    private static void hardSwap(@Nullable TrackDef next) {
        Minecraft mc = Minecraft.getInstance();
        if (current != null) {
            mc.getSoundManager().stop(current);
        }
        fading = false;
        startTrack(next);
    }

    private static void stopAll(Minecraft mc) {
        if (current != null) {
            mc.getSoundManager().stop(current);
        }
        current = null;
        currentTrack = null;
        currentId = null;
        pending = null;
        fading = false;
        phase = Phase.IDLE;
    }
}
