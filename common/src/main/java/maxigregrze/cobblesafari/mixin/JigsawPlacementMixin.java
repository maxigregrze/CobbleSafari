package maxigregrze.cobblesafari.mixin;

import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Lifts vanilla's 128-block jigsaw generation limit so a dungeon may assemble across a whole zone.
 *
 * <p>This is the half of the old combined mixin that genuinely conflicts with other mods doing the
 * same thing, and it is the only half {@code CobbleSafariMixinPlugin} switches off when one is
 * detected. The bounds capture that used to sit beside it now lives in {@code JigsawBoundsMixin},
 * which is never gated - see the reasoning there.</p>
 */
@Mixin(value = JigsawPlacement.class, priority = 1100)
public class JigsawPlacementMixin {

    private JigsawPlacementMixin() {
        // Mixin utility class; not meant to be instantiated.
    }

    @ModifyConstant(
            method = "generateJigsaw",
            constant = @Constant(intValue = 128),
            require = 0
    )
    private static int cobblesafari$modifyMaxGenDistance(int value) {
        return 512;
    }

}
