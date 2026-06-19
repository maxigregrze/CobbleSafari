package maxigregrze.cobblesafari.client.audio;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Custom sound instance: non-positional, optionally looped, with
 * volume ramp for fading. {@link SoundEvent#createVariableRangeEvent} allows playing
 * any resource-pack sound id without registering it.
 */
public class CsMusicSoundInstance extends AbstractTickableSoundInstance {

    private boolean fadingOut = false;
    private float fadeStep = 0.05F;

    public CsMusicSoundInstance(net.minecraft.resources.ResourceLocation event, boolean loop) {
        super(SoundEvent.createVariableRangeEvent(event), SoundSource.MUSIC, RandomSource.create());
        this.looping = loop;
        this.attenuation = Attenuation.NONE;
        this.relative = true;
        this.volume = 1.0F;
    }

    public void fadeOut(int ticks) {
        this.fadingOut = true;
        this.fadeStep = 1.0F / Math.max(1, ticks);
    }

    @Override
    public void tick() {
        if (fadingOut) {
            this.volume = Math.max(0.0F, this.volume - fadeStep);
            if (this.volume <= 0.0F) {
                this.stop();
            }
        }
    }
}
