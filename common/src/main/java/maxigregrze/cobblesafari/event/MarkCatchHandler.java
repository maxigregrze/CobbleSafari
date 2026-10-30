package maxigregrze.cobblesafari.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import kotlin.Unit;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.mark.RibbonAwards;

/**
 * Catch marks (plan 201, D8): 10 % of the Pokémon caught in any registered dungeon (sub-dungeons,
 * Underground and API add-ons included) get the Dungeon Mark, 10 % of Safari catches the Safari Mark.
 */
public final class MarkCatchHandler {

    private static final double MARK_CHANCE = 0.10;
    private static final String SAFARI_DIMENSION = "cobblesafari:domedimension";

    private MarkCatchHandler() {}

    public static void register() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            var player = event.getPlayer();
            String dimension = player.level().dimension().location().toString();
            if (SAFARI_DIMENSION.equals(dimension)) {
                RibbonAwards.rollMark(player, event.getPokemon(), RibbonAwards.MARK_SAFARI, MARK_CHANCE);
            } else if (DungeonDimensions.isDungeonDimension(dimension)) {
                RibbonAwards.rollMark(player, event.getPokemon(), RibbonAwards.MARK_DUNGEON, MARK_CHANCE);
            }
            return Unit.INSTANCE;
        });
    }
}
