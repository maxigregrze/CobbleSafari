package maxigregrze.cobblesafari.mark;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.api.mark.Marks;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Grants the mod's marks and ribbons from code (plan 201, D8). Cobblemon 1.8.0 marks only carry a
 * {@code chance}, no spawn conditions, so every condition lives at the call site.
 *
 * <p>Marks reward a <b>catch</b>, ribbons an <b>accomplishment</b> in a dungeon. A ribbon goes to
 * one random party Pokémon that does not have it yet; a fully decorated party gets nothing.</p>
 */
public final class RibbonAwards {

    public static final ResourceLocation MARK_DUNGEON = id("mark_dungeon");
    public static final ResourceLocation MARK_SAFARI = id("mark_safari");
    public static final ResourceLocation RIBBON_UNDERGROUND = id("ribbon_underground");
    public static final ResourceLocation RIBBON_PARIS = id("ribbon_paris");
    public static final ResourceLocation RIBBON_ULTRASPACE = id("ribbon_dungeon_ultraspace");

    private RibbonAwards() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, path);
    }

    /** Rolls {@code chance} and gives the mark to {@code pokemon} (a fresh catch). */
    public static void rollMark(ServerPlayer player, Pokemon pokemon, ResourceLocation markId, double chance) {
        if (chance <= 0 || player.getRandom().nextDouble() >= chance) {
            return;
        }
        Mark mark = Marks.getByIdentifier(markId);
        if (mark != null && !pokemon.getMarks().contains(mark)) {
            pokemon.exchangeMark(mark, true);
        }
    }

    /** Rolls {@code chance} and gives the ribbon to one random party Pokémon that lacks it. */
    public static void rollRibbon(ServerPlayer player, ResourceLocation ribbonId, double chance) {
        if (chance <= 0 || player.getRandom().nextDouble() >= chance) {
            return;
        }
        Mark ribbon = Marks.getByIdentifier(ribbonId);
        if (ribbon == null) {
            CobbleSafari.LOGGER.warn("[Ribbons] Unknown ribbon {}", ribbonId);
            return;
        }
        List<Pokemon> candidates = new ArrayList<>();
        for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
            if (pokemon != null && !pokemon.getMarks().contains(ribbon)) {
                candidates.add(pokemon);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        candidates.get(player.getRandom().nextInt(candidates.size())).exchangeMark(ribbon, true);
    }
}
