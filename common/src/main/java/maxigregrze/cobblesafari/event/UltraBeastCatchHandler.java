package maxigregrze.cobblesafari.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import kotlin.Unit;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.advancement.ModCriteria;
import net.minecraft.resources.ResourceLocation;

/**
 * "Beast Unleashed": catching an Ultra Beast inside an Ultra Space destination. Those dimensions are
 * only reachable through a Warp Ride, so any Ultra Beast caught there came from a dive.
 */
public final class UltraBeastCatchHandler {

    private static final String ULTRA_SPACE_DIM_PREFIX = "dungeon_ultra";

    private UltraBeastCatchHandler() {}

    public static void register() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            var player = event.getPlayer();
            ResourceLocation dimension = player.level().dimension().location();
            if (dimension.getNamespace().equals(CobbleSafari.MOD_ID)
                    && dimension.getPath().startsWith(ULTRA_SPACE_DIM_PREFIX)
                    && event.getPokemon().hasLabels(CobblemonPokemonLabels.ULTRA_BEAST)) {
                ModCriteria.ULTRA_BEAST_CAUGHT.trigger(player);
            }
            return Unit.INSTANCE;
        });
    }
}
