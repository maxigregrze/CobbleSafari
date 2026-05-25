package maxigregrze.cobblesafari.mixin.client;

import com.cobblemon.mod.common.api.mark.Mark;
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.marks.MarksWidget;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import maxigregrze.cobblesafari.mark.PartnerRibbonTitles;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MarksWidget.class)
public abstract class MarksWidgetMixin {

    @Shadow
    public Pokemon pokemon;

    @Shadow
    public Mark selectedMark;

    @WrapOperation(
            method = "renderWidget",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/cobblemon/mod/common/api/mark/Mark;getTitle(Lnet/minecraft/network/chat/MutableComponent;)Lnet/minecraft/network/chat/MutableComponent;"
            )
    )
    private MutableComponent cobblesafari$partnerRibbonGetTitle(
            Mark mark,
            MutableComponent name,
            Operation<MutableComponent> original
    ) {
        if (PartnerRibbonTitles.isPartnerRibbon(mark)) {
            return PartnerRibbonTitles.applyTitle(pokemon, name, mark);
        }
        return original.call(mark, name);
    }
}
