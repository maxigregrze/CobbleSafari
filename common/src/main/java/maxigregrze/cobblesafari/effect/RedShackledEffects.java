package maxigregrze.cobblesafari.effect;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import maxigregrze.cobblesafari.init.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class RedShackledEffects {

    public static final int DURATION_TICKS_DEFAULT = 100;
    public static final int DURATION_TICKS_POKEMON = 300;

    private RedShackledEffects() {}

    public static boolean isShackled(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        // The client never receives another entity's active effect list, so on the client we read
        // the replicated flag (see RedShackledSynced); the server stays authoritative via hasEffect.
        if (entity.level().isClientSide()) {
            return entity instanceof RedShackledSynced synced && synced.cobblesafari$isShackledSynced();
        }
        return entity.hasEffect(ModEffects.RED_SHACKLED.holder);
    }

    public static boolean isShackled(Player player) {
        return isShackled((LivingEntity) player);
    }

    public static boolean isShackled(PokemonEntity entity) {
        return isShackled((LivingEntity) entity);
    }
}
