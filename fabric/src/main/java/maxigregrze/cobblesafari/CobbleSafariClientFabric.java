package maxigregrze.cobblesafari;

import maxigregrze.cobblesafari.client.hud.TimerHudOverlay;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.network.ClientNetworking;
import maxigregrze.cobblesafari.network.DimensionalBanSyncPayload;
import maxigregrze.cobblesafari.network.TimerSyncPayload;
import maxigregrze.cobblesafari.client.renderer.DungeonPortalBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.BalloonEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.BalloonSafariRenderer;
import maxigregrze.cobblesafari.client.renderer.CsTraderEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.HikerEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.HoopaRingPortalBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.GiratinaCoreBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.DistortionPortalBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.VoidBlockRenderer;
import maxigregrze.cobblesafari.client.model.BalloonEntityModel;
import maxigregrze.cobblesafari.block.basepc.BasePCMenu;
import maxigregrze.cobblesafari.client.screen.BasePCScreen;
import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import maxigregrze.cobblesafari.underground.UndergroundScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.Set;

public class CobbleSafariClientFabric implements ClientModInitializer {

    private static final Set<Item> UNIMPLEMENTED_ITEMS = Set.of(
            ModItems.AURORA_DIAL,
            ModItems.BLOOD_DIAL,
            ModItems.BLUE_DIAL,
            ModItems.HARVEST_DIAL,
            ModItems.MOON_CALENDAR,
            ModItems.PC
    );

    @Override
    public void onInitializeClient() {
        registerTooltips();
        registerClientNetworking();
        registerHud();
        registerBlockRenderLayers();
        registerRenderers();
        registerScreens();
    }

    private void registerTooltips() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (UNIMPLEMENTED_ITEMS.contains(stack.getItem())) {
                lines.add(Component.translatable("tooltip.cobblesafari.not_implemented").withStyle(ChatFormatting.RED));
            }
            if (stack.is(ModBlocks.CREATIVE_DUNGEON_PORTAL.asItem())) {
                lines.add(Component.translatable("tooltip.cobblesafari.creative_dungeon_portal.line1").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("tooltip.cobblesafari.creative_dungeon_portal.line2").withStyle(ChatFormatting.GRAY));
            }
        });
    }

    private void registerClientNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(TimerSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                TimerHudOverlay.updateDisplay(payload.dimensionId(), payload.remainingTicks(), payload.active(), payload.bypassed());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(DimensionalBanSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                DimensionalBanConfig.applyClientSync(payload.dimensions());
            });
        });

        ClientNetworking.registerFabricClientReceivers();
    }

    private void registerHud() {
        HudRenderCallback.EVENT.register(TimerHudOverlay::renderText);
    }

    private void registerBlockRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SAFARI_TELEPORTER, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SAFARI_EGG_NEST, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HOOPA_RING_PORTAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DUNGEON_PORTAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CREATIVE_DUNGEON_PORTAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIRATINA_CORE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIRATINA_CORE_N, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIRATINA_CORE_NE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIRATINA_CORE_E, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIRATINA_CORE_SE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIRATINA_CORE_S, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIRATINA_CORE_SW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIRATINA_CORE_W, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIRATINA_CORE_NW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GIRATINA_CORE_MOVING, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_PORTAL, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_PORTAL_MOVING, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MAGNETIC_CRYSTAL, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MAGNETIC_CLUSTER, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNDERGROUND_SECRET, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_KELP, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_TUBE_CORAL_FAN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_BRAIN_CORAL_FAN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_BUBBLE_CORAL_FAN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_FIRE_CORAL_FAN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_HORN_CORAL_FAN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_TUBE_CORAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_BRAIN_CORAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_BUBBLE_CORAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_FIRE_CORAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AIR_HORN_CORAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ICICLE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNDERGROUND_PC, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SECRETBASE_PC, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INCUBATOR, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_ROCK, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_ROCK_VERTICAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_ROCK_UPSIDEDOWN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_ROCK_DEEP, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_ROCK_DEEP_VERTICAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_ROCK_DEEP_UPSIDEDOWN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_ROCK_FLAT, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_ROCK_FLAT_VERTICAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_ROCK_FLAT_UPSIDEDOWN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_ROCK_FLOATING, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_WEED, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SUSPICIOUS_DISTORTION_ROCK, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_BOULDER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_FLOWER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.VANISHING_DISTORTION_FLOWER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.APPEARING_DISTORTION_FLOWER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_STONEBRICKS_DOOR, RenderType.cutout());
    }

    private void registerRenderers() {
        BlockEntityRenderers.register(ModBlockEntities.HOOPA_RING_PORTAL, HoopaRingPortalBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DUNGEON_PORTAL, DungeonPortalBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.VOID_BLOCK, VoidBlockRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.GIRATINA_CORE, GiratinaCoreBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DISTORTION_PORTAL, DistortionPortalBlockEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.HIKER, HikerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CSTRADER_NPC, CsTraderEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BALLOON, BalloonEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BALLOON_SAFARI, BalloonSafariRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_MUD_BALL, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_BAIT, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(BalloonEntityModel.LAYER_LOCATION, BalloonEntityModel::createBodyLayer);
    }

    private void registerScreens() {
        MenuScreens.register(UndergroundMinigame.MENU_TYPE, UndergroundScreen::new);
        MenuScreens.register(BasePCMenu.MENU_TYPE, BasePCScreen::new);
    }
}
