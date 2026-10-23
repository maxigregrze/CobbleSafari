package maxigregrze.cobblesafari.client;

import com.mojang.blaze3d.vertex.PoseStack;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.client.renderer.NihilegoHatLayer;
import maxigregrze.cobblesafari.client.renderer.ReconElytraLayer;
import maxigregrze.cobblesafari.client.renderer.StakatakaShieldRenderer;
import maxigregrze.cobblesafari.client.renderer.UltraArmorModels;
import maxigregrze.cobblesafari.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** NeoForge client hooks of the Ultra Space loot (action plan 192, section 2.1). */
@EventBusSubscriber(modid = CobbleSafari.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UltraLootClientExtensions {

    private UltraLootClientExtensions() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                        @Override
                        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                                 MultiBufferSource buffers, int packedLight, int packedOverlay) {
                            StakatakaShieldRenderer.render(stack, pose, buffers, packedLight, packedOverlay);
                        }
                    };
                }
                return renderer;
            }
        }, ModItems.STAKATAKA_SHIELD);

        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                                                          HumanoidModel<?> original) {
                if (stack.is(ModItems.NIHILEGO_HAT)) {
                    // The armor layer is cutout-only: NihilegoHatLayer draws the hat translucent instead.
                    return UltraArmorModels.empty();
                }
                HumanoidModel<LivingEntity> model = UltraArmorModels.reconModelFor(stack);
                // NeoForge then copies the pose and part visibility of the original onto it.
                return model != null ? model : original;
            }
        }, ModItems.RECON_ARMOR_HELMET, ModItems.RECON_ARMOR_CHESTPLATE, ModItems.NIHILEGO_HAT);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new ReconElytraLayer<>(renderer, event.getEntityModels()));
                renderer.addLayer(new NihilegoHatLayer<>(renderer));
            }
        }
        for (EntityType<?> type : event.getEntityTypes()) {
            addHatLayer(event.getRenderer(type));
        }
    }

    /** Every renderer with a humanoid model can wear the Nihilhat (players, armor stands, zombies...). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addHatLayer(EntityRenderer<?> renderer) {
        if (renderer instanceof LivingEntityRenderer living && living.getModel() instanceof HumanoidModel) {
            living.addLayer(new NihilegoHatLayer(living));
        }
    }
}
