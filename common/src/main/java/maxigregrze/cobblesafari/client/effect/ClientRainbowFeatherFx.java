package maxigregrze.cobblesafari.client.effect;

import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Plays the Rainbow Feather revival feedback, fed by
 * {@link maxigregrze.cobblesafari.network.RainbowFeatherUsedPayload}.
 *
 * <p>Reproduces what vanilla does for entity event 35 - a tracking totem emitter, a sound, and the
 * full-screen item flash for the saved player - except that the sound and the item are the
 * feather's own. The particle stays {@code TOTEM_OF_UNDYING}: its green and gold suits Ho-Oh, and
 * a dedicated particle type would not be worth registering for a single use.</p>
 */
public final class ClientRainbowFeatherFx {

    /** Same emitter lifetime vanilla uses for the totem. */
    private static final int EMITTER_LIFETIME = 30;

    private ClientRainbowFeatherFx() {}

    /** Called on the client thread by the payload receiver. */
    public static void accept(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            return;
        }

        minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, EMITTER_LIFETIME);
        level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                ModSounds.RAINBOW_FEATHER_REVIVE, entity.getSoundSource(), 1.0F, 1.0F, false);
        if (entity == minecraft.player) {
            minecraft.gameRenderer.displayItemActivation(new ItemStack(ModItems.RAINBOW_FEATHER));
        }
    }
}
