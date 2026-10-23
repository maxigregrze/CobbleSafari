package maxigregrze.cobblesafari.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a worn Nihilhat translucent (action plan 192, section 9). Used on NeoForge only, whose
 * armor layer forces a cutout render type: there the armor layer gets an empty model for the hat and
 * this layer, added to every humanoid renderer, draws it. Fabric draws it from its ArmorRenderer.
 */
public class NihilegoHatLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    public NihilegoHatLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(ModItems.NIHILEGO_HAT)) {
            UltraArmorModels.renderNihilegoHat(pose, buffers, packedLight, head, getParentModel());
        }
    }
}
