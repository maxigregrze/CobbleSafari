package maxigregrze.cobblesafari.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = JigsawStructure.class, priority = 1100)
public abstract class JigsawStructureMixin extends Structure {

    protected JigsawStructureMixin(StructureSettings settings) {
        super(settings);
    }

    @WrapOperation(
            method = "lambda$static$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/serialization/Codec;intRange(II)Lcom/mojang/serialization/Codec;"
            ),
            require = 0
    )
    private static Codec<Integer> cobblesafari$unlimitCodecRange(int minInclusive, int maxInclusive, Operation<Codec<Integer>> original) {
        return Codec.INT;
    }

    @ModifyConstant(method = "verifyRange", constant = @Constant(intValue = 128), require = 0)
    private static int cobblesafari$modifyMaxDistanceValidation(int value) {
        return 512;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 80), require = 0)
    private int cobblesafari$modifyDefaultMaxDistance(int value) {
        return 500;
    }
}
