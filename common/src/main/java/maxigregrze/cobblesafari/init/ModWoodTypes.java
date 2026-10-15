package maxigregrze.cobblesafari.init;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * Wood-type holder for the Hyperspace wood set, used by signs / hanging signs so the client can
 * resolve their atlas materials. The name carries the mod namespace ({@code cobblesafari:hyperspace});
 * {@code Sheets} splits it so the sign textures resolve to
 * {@code assets/cobblesafari/textures/entity/signs/hyperspace.png} (and {@code .../signs/hanging/}).
 * Reuses {@link BlockSetType#OAK} for interaction sounds/behaviour.
 *
 * <p>The type is only <em>constructed</em> here; each loader adds it to {@code WoodType.TYPES}. On NeoForge
 * that happens after {@code Sheets} has built its sign-material maps, so the NeoForge client setup also
 * calls {@code Sheets.addWoodType(HYPERSPACE)} - otherwise rendering a Hyperspace sign NPEs on a null Material.</p>
 */
public final class ModWoodTypes {

    private ModWoodTypes() {}

    public static final WoodType HYPERSPACE = new WoodType(CobbleSafari.MOD_ID + ":hyperspace", BlockSetType.OAK);

    /** Touch point so the static field is initialised before the sign blocks are constructed. */
    public static void register() {
        // The static field initialiser constructs the type; this method just forces class load.
    }
}
