package maxigregrze.cobblesafari.compat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import java.lang.reflect.Method;

public final class CobblemonFishingInterop {

    private static final Method ALTER_ALPHA_ATTEMPT;
    private static final Method ALTER_HA_ATTEMPT;
    private static final Object FISHING_SPAWN_CAUSE_COMPANION;

    static {
        try {
            Class<?> outer = Class.forName("com.cobblemon.mod.common.api.spawning.fishing.FishingSpawnCause");
            FISHING_SPAWN_CAUSE_COMPANION = outer.getField("Companion").get(null);
            Class<?> comp = FISHING_SPAWN_CAUSE_COMPANION.getClass();
            ALTER_ALPHA_ATTEMPT = comp.getMethod("alterAlphaAttempt", PokemonEntity.class);
            ALTER_HA_ATTEMPT = comp.getMethod("alterHAAttempt", PokemonEntity.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private CobblemonFishingInterop() {}

    public static void alterAlphaAttempt(PokemonEntity entity) {
        try {
            ALTER_ALPHA_ATTEMPT.invoke(FISHING_SPAWN_CAUSE_COMPANION, entity);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void alterHiddenAbilityAttempt(PokemonEntity entity) {
        try {
            ALTER_HA_ATTEMPT.invoke(FISHING_SPAWN_CAUSE_COMPANION, entity);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
