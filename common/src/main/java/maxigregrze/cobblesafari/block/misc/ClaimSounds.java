package maxigregrze.cobblesafari.block.misc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Vanilla's item pickup sound, played to one player only: lost items and Auspicious Pokeballs stay in
 * place for the others, and a completed task is that player's own (action plan 196, sections 7.7 and 7.9).
 */
public final class ClaimSounds {

    private ClaimSounds() {}

    /** Same volume and random pitch as an item entity picked up. */
    public static void playPickup(ServerPlayer player) {
        RandomSource random = player.getRandom();
        player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f,
                ((random.nextFloat() - random.nextFloat()) * 0.7f + 1.0f) * 2.0f);
    }
}
