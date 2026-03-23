package maxigregrze.cobblesafari;

import maxigregrze.cobblesafari.client.hud.TimerHudOverlay;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.client.screen.DistortionStoneBricksRuneScreen;
import maxigregrze.cobblesafari.client.screen.TpAcceptScreen;
import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.network.CloseTpAcceptPayload;
import maxigregrze.cobblesafari.network.DimensionalBanSyncPayload;
import maxigregrze.cobblesafari.network.OpenTpAcceptPayload;
import maxigregrze.cobblesafari.network.OpenLostNoteBookPayload;
import maxigregrze.cobblesafari.network.OpenRuneEditorPayload;
import maxigregrze.cobblesafari.network.TimerSyncPayload;
import maxigregrze.cobblesafari.client.renderer.DungeonPortalBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.BalloonEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.BalloonSafariRenderer;
import maxigregrze.cobblesafari.client.renderer.CsTraderEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.HikerEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.HoopaRingPortalBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.GiratinaCoreBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.DistortionPortalBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.LostItemBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.VoidBlockRenderer;
import maxigregrze.cobblesafari.client.model.BalloonEntityModel;
import maxigregrze.cobblesafari.block.basepc.BasePCMenu;
import maxigregrze.cobblesafari.client.screen.BasePCScreen;
import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import maxigregrze.cobblesafari.underground.UndergroundScreen;
import maxigregrze.cobblesafari.underground.network.UndergroundPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;

@EventBusSubscriber(modid = CobbleSafari.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CobbleSafariClientNeoForge {

    private static volatile Set<Item> unimplementedItems;

    private static Set<Item> getUnimplementedItems() {
        if (unimplementedItems == null) {
            unimplementedItems = Set.of(
                    ModItems.AURORA_DIAL,
                    ModItems.BLOOD_DIAL,
                    ModItems.BLUE_DIAL,
                    ModItems.HARVEST_DIAL,
                    ModItems.MOON_CALENDAR,
                    ModItems.PC
            );
        }
        return unimplementedItems;
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(UndergroundMinigame.MENU_TYPE, UndergroundScreen::new);
        event.register(BasePCMenu.MENU_TYPE, BasePCScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SAFARI_TELEPORTER, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SAFARI_EGG_NEST, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HOOPA_RING_PORTAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DUNGEON_PORTAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CREATIVE_DUNGEON_PORTAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GIRATINA_CORE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GIRATINA_CORE_N, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GIRATINA_CORE_NE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GIRATINA_CORE_E, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GIRATINA_CORE_SE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GIRATINA_CORE_S, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GIRATINA_CORE_SW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GIRATINA_CORE_W, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GIRATINA_CORE_NW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GIRATINA_CORE_MOVING, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_PORTAL, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_PORTAL_MOVING, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGNETIC_CRYSTAL, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MAGNETIC_CLUSTER, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNDERGROUND_SECRET, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_KELP, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_TUBE_CORAL_FAN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_BRAIN_CORAL_FAN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_BUBBLE_CORAL_FAN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_FIRE_CORAL_FAN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_HORN_CORAL_FAN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_TUBE_CORAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_BRAIN_CORAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_BUBBLE_CORAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_FIRE_CORAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AIR_HORN_CORAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ICICLE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNDERGROUND_PC, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SECRETBASE_PC, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.INCUBATOR, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_ROCK, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_ROCK_VERTICAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_ROCK_UPSIDEDOWN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_ROCK_DEEP, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_ROCK_DEEP_VERTICAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_ROCK_DEEP_UPSIDEDOWN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_ROCK_FLAT, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_ROCK_FLAT_VERTICAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_ROCK_FLAT_UPSIDEDOWN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_ROCK_FLOATING, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_WEED, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_WEED_SPAWN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SUSPICIOUS_DISTORTION_ROCK, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_BOULDER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_FLOWER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_FLOWER_CARPET, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VANISHING_DISTORTION_FLOWER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.APPEARING_DISTORTION_FLOWER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_STONEBRICKS_DOOR, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_STONEBRICKS_RUBBLE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LOST_NOTES, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LOST_ITEM, RenderType.cutout());

            BlockEntityRenderers.register(ModBlockEntities.HOOPA_RING_PORTAL, HoopaRingPortalBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.DUNGEON_PORTAL, DungeonPortalBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.VOID_BLOCK, VoidBlockRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.GIRATINA_CORE, GiratinaCoreBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.DISTORTION_PORTAL, DistortionPortalBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.LOST_ITEM, LostItemBlockEntityRenderer::new);
        });

        NeoForge.EVENT_BUS.addListener(CobbleSafariClientNeoForge::onRenderGuiLayer);
        NeoForge.EVENT_BUS.addListener(CobbleSafariClientNeoForge::onItemTooltip);
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HIKER, HikerEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CSTRADER_NPC, CsTraderEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.BALLOON, BalloonEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.BALLOON_SAFARI, BalloonSafariRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_MUD_BALL, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_BAIT, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BalloonEntityModel.LAYER_LOCATION, BalloonEntityModel::createBodyLayer);
    }

    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            TimerHudOverlay.renderText(event.getGuiGraphics(), event.getPartialTick());
        }
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (getUnimplementedItems().contains(event.getItemStack().getItem())) {
            event.getToolTip().add(Component.translatable("tooltip.cobblesafari.not_implemented").withStyle(ChatFormatting.RED));
        }
        if (event.getItemStack().is(ModBlocks.CREATIVE_DUNGEON_PORTAL.asItem())) {
            event.getToolTip().add(Component.translatable("tooltip.cobblesafari.creative_dungeon_portal.line1").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("tooltip.cobblesafari.creative_dungeon_portal.line2").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void handleTimerSync(TimerSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            TimerHudOverlay.updateDisplay(payload.dimensionId(), payload.remainingTicks(), payload.active(), payload.bypassed());
        });
    }

    public static void handleDimensionalBanSync(DimensionalBanSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            DimensionalBanConfig.applyClientSync(payload.dimensions());
        });
    }

    public static void handleOpenTpAccept(OpenTpAcceptPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new TpAcceptScreen(
                    payload.dimensionName(),
                    payload.dimensionId(),
                    payload.hasEntryFee(),
                    payload.isCobbledollarFee(),
                    payload.entryFeeAmount(),
                    payload.entryFeeItem(),
                    payload.source(),
                    payload.alreadyPaidToday()
            ));
        });
    }

    public static void handleCloseTpAccept(CloseTpAcceptPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof TpAcceptScreen tpScreen) {
                tpScreen.closeFromServer();
            }
        });
    }

    public static void handleOpenRuneEditor(OpenRuneEditorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new DistortionStoneBricksRuneScreen(payload.pos(), payload.text()));
        });
    }

    public static void handleOpenLostNoteBook(OpenLostNoteBookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BookViewScreen.BookAccess access = BookViewScreen.BookAccess.fromItem(payload.book());
            Minecraft.getInstance().setScreen(new BookViewScreen(access));
        });
    }

    public static void handleGridUpdate(UndergroundPayloads.GridUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof UndergroundScreen screen) {
                if (screen.getSessionId().equals(payload.sessionId())) {
                    screen.onGridUpdate(payload);
                }
            }
        });
    }

    public static void handleStabilityUpdate(UndergroundPayloads.StabilityUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof UndergroundScreen screen) {
                if (screen.getSessionId().equals(payload.sessionId())) {
                    screen.onStabilityUpdate(payload);
                }
            }
        });
    }

    public static void handleTreasureRevealed(UndergroundPayloads.TreasureRevealedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof UndergroundScreen screen) {
                if (screen.getSessionId().equals(payload.sessionId())) {
                    screen.onTreasureRevealed(payload);
                }
            }
        });
    }

    public static void handleGameEnd(UndergroundPayloads.GameEndPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof UndergroundScreen screen) {
                if (screen.getSessionId().equals(payload.sessionId())) {
                    screen.onGameEnd(payload);
                }
            }
        });
    }

    public static void handlePlaySound(UndergroundPayloads.PlaySoundPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof UndergroundScreen screen) {
                if (screen.getSessionId().equals(payload.sessionId())) {
                    screen.playSound(payload.soundType());
                }
            }
        });
    }
}
