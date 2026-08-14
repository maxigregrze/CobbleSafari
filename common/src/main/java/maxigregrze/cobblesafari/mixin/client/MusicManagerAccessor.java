package maxigregrze.cobblesafari.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the vanilla background track before {@link MusicManager#stopPlaying()} nulls the field, so
 * csmusic can keep re-stopping it until the sound engine really lets it go (see
 * {@code CsMusicPlayer.tickVanillaKill}).
 */
@Mixin(MusicManager.class)
public interface MusicManagerAccessor {

    @Accessor("currentMusic")
    @Nullable
    SoundInstance cobblesafari$getCurrentMusic();
}
