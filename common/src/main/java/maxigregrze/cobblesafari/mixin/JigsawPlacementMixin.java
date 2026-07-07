package maxigregrze.cobblesafari.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import maxigregrze.cobblesafari.dungeon.DungeonStructureBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    /**
     * Captures the real assembled bounding box (union of every placed piece) so the dungeon clearer
     * can wipe exactly the structure's footprint instead of a fixed ±255-block window. The builder local
     * is in scope at the {@code build()} call that opens the placement loop. Defensive ({@code require = 0}):
     * if the injection point ever drifts, capture is simply skipped and the clearer falls back to the
     * legacy zoneSize radius.
     */
    @Inject(
            method = "generateJigsaw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/pieces/StructurePiecesBuilder;build()Lnet/minecraft/world/level/levelgen/structure/pieces/PiecesContainer;"
            ),
            require = 0
    )
    private static void cobblesafari$captureBounds(
            ServerLevel level, Holder<StructureTemplatePool> startPool, ResourceLocation startJigsawName,
            int maxDepth, BlockPos pos, boolean keepJigsaws,
            CallbackInfoReturnable<Boolean> cir,
            @Local StructurePiecesBuilder builder) {
        // Skip vanilla/other-mod jigsaws entirely: only a CobbleSafari placement flags this thread (B4).
        if (!DungeonStructureBounds.isCapturing()) {
            return;
        }
        DungeonStructureBounds.record(pos, builder.getBoundingBox());
    }
}
