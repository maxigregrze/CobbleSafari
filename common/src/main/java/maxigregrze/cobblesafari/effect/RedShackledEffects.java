package maxigregrze.cobblesafari.effect;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class RedShackledEffects {

    public static final int DURATION_TICKS_DEFAULT = 100;
    public static final int DURATION_TICKS_POKEMON = 300;

    private RedShackledEffects() {}

    /**
     * Authoritative on the server. On the client this only answers for the local player - vanilla
     * never sends another entity's effect list - which is enough for every caller here; the purely
     * visual client read lives in
     * {@link maxigregrze.cobblesafari.client.effect.ClientShackleTracker}.
     */
    public static boolean isShackled(LivingEntity entity) {
        return entity != null && entity.hasEffect(ModEffects.RED_SHACKLED.holder);
    }

    public static boolean isShackled(Player player) {
        return isShackled((LivingEntity) player);
    }

    public static boolean isShackled(PokemonEntity entity) {
        return isShackled((LivingEntity) entity);
    }
}
