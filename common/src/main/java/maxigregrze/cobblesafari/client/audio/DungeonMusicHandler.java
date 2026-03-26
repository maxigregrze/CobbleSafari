package maxigregrze.cobblesafari.client.audio;

import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;

public class DungeonMusicHandler {

    private enum Phase { NONE, INTRO, LOOP }

    private record DimensionMusic(SoundEvent intro, SoundEvent loop) {}

    private static Phase currentPhase = Phase.NONE;
    private static SimpleSoundInstance currentSound = null;
    private static ResourceKey<Level> currentDimension = null;

    private DungeonMusicHandler() {}

    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        ResourceKey<Level> dim = mc.level.dimension();
        DimensionMusic music = getDimensionMusic(dim);

        if (music == null) {
            if (currentPhase != Phase.NONE) {
                stopAll(mc);
            }
            return;
        }

        if (mc.isPaused()) return;

        if (!dim.equals(currentDimension)) {
            stopAll(mc);
            currentDimension = dim;
            mc.getMusicManager().stopPlaying();
            playIntro(mc, music);
            return;
        }

        if ((currentPhase == Phase.INTRO || currentPhase == Phase.LOOP)
                && currentSound != null
                && !mc.getSoundManager().isActive(currentSound)) {
            playLoop(mc, music);
        }
    }

    private static DimensionMusic getDimensionMusic(ResourceKey<Level> dim) {
        if (dim.equals(DungeonDimensions.DUNGEON_UNDERGROUND)) {
            return new DimensionMusic(ModSounds.MUSIC_UNDERGROUND_INTRO, ModSounds.MUSIC_UNDERGROUND_LOOP);
        }
        if (dim.equals(DungeonDimensions.DUNGEON_DISTORTION)) {
            return new DimensionMusic(ModSounds.MUSIC_DISTORTION_INTRO, ModSounds.MUSIC_DISTORTION_LOOP);
        }
        return null;
    }

    private static void playIntro(Minecraft mc, DimensionMusic music) {
        currentSound = SimpleSoundInstance.forMusic(music.intro());
        currentPhase = Phase.INTRO;
        mc.getSoundManager().play(currentSound);
    }

    private static void playLoop(Minecraft mc, DimensionMusic music) {
        currentSound = SimpleSoundInstance.forMusic(music.loop());
        currentPhase = Phase.LOOP;
        mc.getSoundManager().play(currentSound);
    }

    private static void stopAll(Minecraft mc) {
        if (currentSound != null) {
            mc.getSoundManager().stop(currentSound);
        }
        currentSound = null;
        currentPhase = Phase.NONE;
        currentDimension = null;
    }
}
