package maxigregrze.cobblesafari.mixin;

import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.mark.PartnerRibbonTitles;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pokemon.class)
public abstract class PokemonMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void cobblesafari$partnerRibbonTitle(boolean showTitle, CallbackInfoReturnable<MutableComponent> cir) {
        if (!showTitle) {
            return;
        }
        Pokemon self = (Pokemon) (Object) this;
        Mark mark = self.getActiveMark();
        if (!PartnerRibbonTitles.isPartnerRibbon(mark)) {
            return;
        }
        MutableComponent baseName = self.getDisplayName(false);
        cir.setReturnValue(PartnerRibbonTitles.applyTitle(self, baseName, mark));
    }
}
