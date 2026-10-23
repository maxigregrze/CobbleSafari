package maxigregrze.cobblesafari.client;

import maxigregrze.cobblesafari.client.renderer.ReconElytraLayer;
import maxigregrze.cobblesafari.client.renderer.UltraArmorModels;
import maxigregrze.cobblesafari.init.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.LivingEntity;

/** Fabric client hooks of the Ultra Space loot armor (action plan 192, sections 9, 10.3 and 10.4). */
public final class UltraLootFabricClient {

    private UltraLootFabricClient() {}

    public static void register() {
        ArmorRenderer.register((pose, buffers, stack, entity, slot, light, contextModel) -> {
            HumanoidModel<LivingEntity> model = UltraArmorModels.reconModelFor(stack);
            if (model == null) {
                return;
            }
            contextModel.copyPropertiesTo(model);
            UltraArmorModels.prepare(model, slot);
            ArmorRenderer.renderPart(pose, buffers, light, stack, model, UltraArmorModels.RECON_TEXTURE);
        }, ModItems.RECON_ARMOR_HELMET, ModItems.RECON_ARMOR_CHESTPLATE);

        // ArmorRenderer#renderPart is cutout: the Nihilhat draws itself, translucent.
        ArmorRenderer.register((pose, buffers, stack, entity, slot, light, contextModel) ->
                UltraArmorModels.renderNihilegoHat(pose, buffers, light, stack, contextModel), ModItems.NIHILEGO_HAT);

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof PlayerRenderer playerRenderer) {
                helper.register(new ReconElytraLayer<>(playerRenderer, context.getModelSet()));
            }
        });
    }
}
