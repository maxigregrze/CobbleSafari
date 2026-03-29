package maxigregrze.cobblesafari.mixin;

import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = JigsawPlacement.class, priority = 1100)
public class JigsawPlacementMixin {

    @ModifyConstant(
            method = "generateJigsaw",
            constant = @Constant(intValue = 128),
            require = 0
    )
    private static int cobblesafari$modifyMaxGenDistance(int value) {
        return 512;
    }
}
