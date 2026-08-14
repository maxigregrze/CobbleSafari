package maxigregrze.cobblesafari.mixin.client;

import maxigregrze.cobblesafari.client.audio.CsMusicPlayer;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compatibility with <b>Constant Music</b>, applied only when that mod is present (see
 * {@code CobbleSafariMixinPlugin}).
 *
 * <p>Constant Music redirects the {@code Math.min(nextSongDelay, music.getMaxDelay())} at the end of
 * {@link MusicManager#tick()} to {@code Math.min(nextSongDelay, timer * 20)}, and its timer is
 * {@code 0} until its config file is written. That defeats the per-tick {@code stopPlaying()} csmusic
 * relies on: the manager starts a new track every single tick, and each track stopped while its OGG
 * stream is still loading leaks an orphan channel that plays to completion over the csmusic voice.
 * Cancelling the whole tick removes the restart at the source - the redirect simply never runs.</p>
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerConstantMusicMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cobblesafari$suppressWhileCsMusicPlays(CallbackInfo ci) {
        if (CsMusicPlayer.isSuppressingVanillaMusic()) {
            ci.cancel();
        }
    }
}
