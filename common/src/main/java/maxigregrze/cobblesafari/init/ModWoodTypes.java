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
 * <p>The type is only <em>constructed</em> here, not added to {@code WoodType.TYPES} — that registration
 * method is not accessible from the common (vanilla-mapped) module, and is unnecessary: the only consumer
 * that matters is the client sign atlas, which each loader wires up via {@code Sheets.addWoodType(HYPERSPACE)}.</p>
 */
public final class ModWoodTypes {

    private ModWoodTypes() {}

    public static final WoodType HYPERSPACE = new WoodType(CobbleSafari.MOD_ID + ":hyperspace", BlockSetType.OAK);

    /** Touch point so the static field is initialised before the sign blocks are constructed. */
    public static void register() {
        // The static field initialiser constructs the type; this method just forces class load.
    }
}
