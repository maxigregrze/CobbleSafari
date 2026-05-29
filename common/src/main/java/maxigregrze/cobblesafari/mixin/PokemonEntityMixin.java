package maxigregrze.cobblesafari.mixin;

import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.mark.PartnerRibbonTitles;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PokemonEntity.class)
public abstract class PokemonEntityMixin {

    @Inject(method = "getTitledName", at = @At("RETURN"), cancellable = true)
    private void cobblesafari$partnerRibbonTitledName(CallbackInfoReturnable<MutableComponent> cir) {
        PokemonEntity self = (PokemonEntity) (Object) this;
        Mark mark = PartnerRibbonTitles.resolveEntityMark(self);
        if (!PartnerRibbonTitles.isPartnerRibbon(mark)) {
            return;
        }
        Pokemon pokemon = self.getPokemon();
        MutableComponent baseName = self.getName().copy();
        cir.setReturnValue(PartnerRibbonTitles.applyTitle(pokemon, baseName, mark));
    }
}
