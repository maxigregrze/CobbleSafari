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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the real assembled bounding box of a CobbleSafari jigsaw placement - the union of every
 * placed piece - so the dungeon clearer can wipe exactly the structure's footprint and the marker
 * sweep can search exactly its volume.
 *
 * <p><b>Deliberately its own class, and never gated.</b> This injection used to live beside the
 * {@code @ModifyConstant} that lifts vanilla's 128-block generation limit, in a single
 * {@code JigsawPlacementMixin}. That constant is a genuine conflict with any mod doing the same
 * thing, so {@code CobbleSafariMixinPlugin} switches the whole class off when one is present - and
 * took this capture with it, for no reason: reading a bounding box conflicts with nothing.</p>
 *
 * <p>The consequences of losing it are entirely silent and entirely downstream. The clear window
 * falls back to a config guess and leaves the bottom courses of the structure standing; both marker
 * sweeps fall back to a box around the anchor and miss the attached pieces, so the exit portal and
 * every spawn anchor outside the start piece go unfound. Separating the two concerns is what
 * guarantees the capture survives whatever else is installed.</p>
 *
 * <p>Scoped by {@link DungeonStructureBounds#isCapturing()}: only a thread running one of the mod's
 * own placements records anything, so vanilla villages and other mods' structures are untouched and
 * leak nothing (action plan 145, B4).</p>
 */
@Mixin(value = JigsawPlacement.class, priority = 1100)
public class JigsawBoundsMixin {

    private JigsawBoundsMixin() {
        // Mixin utility class; not meant to be instantiated.
    }

    /**
     * The builder local is in scope at the {@code build()} call that opens the placement loop, and
     * holds every piece by then.
     *
     * <p>{@code require = 1}, unlike the sibling limit mixin: if this injection ever stops applying
     * the mod must fail loudly at load rather than quietly generate broken instances for weeks.</p>
     */
    @Inject(
            method = "generateJigsaw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/pieces/StructurePiecesBuilder;build()Lnet/minecraft/world/level/levelgen/structure/pieces/PiecesContainer;"
            )
    )
    private static void cobblesafari$captureBounds(
            ServerLevel level, Holder<StructureTemplatePool> startPool, ResourceLocation startJigsawName,
            int maxDepth, BlockPos pos, boolean keepJigsaws,
            CallbackInfoReturnable<Boolean> cir,
            @Local StructurePiecesBuilder builder) {
        if (!DungeonStructureBounds.isCapturing()) {
            return;
        }
        DungeonStructureBounds.record(pos, builder.getBoundingBox());
    }
}
