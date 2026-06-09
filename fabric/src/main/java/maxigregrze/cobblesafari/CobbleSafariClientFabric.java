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
import maxigregrze.cobblesafari.client.renderer.AuspiciousPokeballBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.DistortionPortalBlockEntityRenderer;
import maxigregrze.cobblesafari.client.DungeonDistortionDimensionEffects;
import maxigregrze.cobblesafari.mixin.client.DimensionSpecialEffectsAccessor;
import maxigregrze.cobblesafari.client.renderer.LostItemBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.VoidBlockRenderer;
import maxigregrze.cobblesafari.client.model.BalloonEntityModel;
import maxigregrze.cobblesafari.block.basepc.BasePCMenu;
import maxigregrze.cobblesafari.client.screen.BasePCScreen;
import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import maxigregrze.cobblesafari.underground.UndergroundScreen;
import maxigregrze.cobblesafari.client.audio.CsMusicPlayer;
import maxigregrze.cobblesafari.client.DonutItemClientSetup;
import maxigregrze.cobblesafari.client.donut.DonutFlavorClientTooltip;
import maxigregrze.cobblesafari.item.donut.DonutTooltipPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
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
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof DonutTooltipPayload p) {
                return new DonutFlavorClientTooltip(p);
            }
            return null;
        });
        DimensionSpecialEffectsAccessor.getEffects().put(
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_distortion"),
                new DungeonDistortionDimensionEffects()
        );
        registerTooltips();
        DonutItemClientSetup.registerItemProperties();
        registerClientNetworking();
        registerHud();
        registerBlockRenderLayers();
        registerRenderers();
        registerScreens();
        registerDungeonMusic();
        maxigregrze.cobblesafari.client.RotomPhoneModelLoadingPlugin.register();
        KeyBindingHelper.registerKeyBinding(maxigregrze.cobblesafari.client.rotomphone.RotomPhoneKeybind.OPEN_PHONE);
        KeyBindingHelper.registerKeyBinding(maxigregrze.cobblesafari.client.objectives.ObjectivesKeybind.TOGGLE_HUD);
        maxigregrze.cobblesafari.client.hud.HudClientConfig.reload();

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("accessories")) {
            maxigregrze.cobblesafari.compat.accessories.AccessoriesClientCompat.init();
        }


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhonePcSession.tickCleanup(client);
            maxigregrze.cobblesafari.client.rotomphone.RotoGlideClient.tick(client);
            maxigregrze.cobblesafari.client.rotomphone.RotomPhoneKeybind.clientTick(client);
            maxigregrze.cobblesafari.client.objectives.ObjectivesKeybind.clientTick(client);
            maxigregrze.cobblesafari.client.objectives.ObjectivesHudController.clientTick(client);
        });
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

        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.SetCsMusicPayload.TYPE, (payload, context) -> {
                    context.client().execute(() -> CsMusicPlayer.accept(payload));
                });

        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.ObjectivesHudSyncPayload.TYPE, (payload, context) -> {
                    context.client().execute(() -> maxigregrze.cobblesafari.client.objectives.ObjectivesHudController.accept(payload));
                });

        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.HudConfigRefreshPayload.TYPE, (payload, context) -> {
                    context.client().execute(maxigregrze.cobblesafari.client.hud.HudClientConfig::reload);
                });

        ClientNetworking.registerFabricClientReceivers();
    }

    private void registerHud() {
        HudRenderCallback.EVENT.register(TimerHudOverlay::renderText);
        HudRenderCallback.EVENT.register(maxigregrze.cobblesafari.client.hud.ObjectivesHudOverlay::render);
    }

    private void registerBlockRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SAFARI_TELEPORTER, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SAFARI_EGG_NEST, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HOOPA_RING_PORTAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DUNGEON_PORTAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CREATIVE_DUNGEON_PORTAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DUNGEON_PORTAL_EFFECT, RenderType.translucent());
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
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_PORTAL_MOVING_BACK, RenderType.translucent());
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
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUSTED_IRON_BLOCK, RenderType.cutoutMipped());
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
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_WEED_SPAWN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SUSPICIOUS_DISTORTION_ROCK, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_BOULDER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_FLOWER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_FLOWER_CARPET, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.VANISHING_DISTORTION_FLOWER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.APPEARING_DISTORTION_FLOWER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_STONEBRICKS_DOOR, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTORTION_STONEBRICKS_RUBBLE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LOST_NOTES, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LOST_ITEM, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LOST_ITEM_VISUAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WHIRLWIND, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WHIRLWIND_DISPLAY, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CSBOSS_ELECTRICITY, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MUD_PILE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SLUDGE_PILE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KARATE_MANNEQUIN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PUNCHINGBAG, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PUNCHINGBAG_BAG_DISPLAY, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TOMBSTONE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TOMBSTONE_SMALL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AUSPICIOUS_POKEBALL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AUSPICIOUS_POKEBALL_DISPLAY, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AUSPICIOUS_POKEBALL_SMALL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AUSPICIOUS_POKEBALL_SMALL_DISPLAY, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AUSPICIOUS_POKEBALL_GOLD, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AUSPICIOUS_POKEBALL_GOLD_DISPLAY, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_PILLAR, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_CROWD, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_CROWD_DISPLAY, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_POKEBALL, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_POKEBALL_DISPLAY, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SPOT, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SPOT_DISPLAY, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_GLOBE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ONLINE_FEATURE_PC, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ONLINE_FEATURE_PC_UNION, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ONLINE_FEATURE_PC_GTS, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ONLINE_FEATURE_PC_WONDER, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_GLOBE_DISPLAY_MOVING, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SCREEN, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SCREEN_DISPLAY, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SCREEN_LEFT, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SCREEN_LEFT_DISPLAY, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SCREEN_RIGHT, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SCREEN_RIGHT_DISPLAY, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SPOTLIGHT_GREEN, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SPOTLIGHT_YELLOW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SPOTLIGHT_BLUE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SPOTLIGHT_RED, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_GREEN, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_YELLOW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_BLUE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_RED, RenderType.translucent());
    }

    private void registerRenderers() {
        BlockEntityRenderers.register(ModBlockEntities.HOOPA_RING_PORTAL, HoopaRingPortalBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DUNGEON_PORTAL, DungeonPortalBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.VOID_BLOCK, VoidBlockRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.GIRATINA_CORE, GiratinaCoreBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DISTORTION_PORTAL, DistortionPortalBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.LOST_ITEM, LostItemBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.AUSPICIOUS_POKEBALL, AuspiciousPokeballBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.AUSPICIOUS_POKEBALL_GOLD, AuspiciousPokeballBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.UNION_ROOM_DECOR, maxigregrze.cobblesafari.client.renderer.UnionRoomDecorBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.WHIRLWIND, maxigregrze.cobblesafari.client.renderer.WhirlwindBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.UNION_ROOM_GLOBE_UPPER, maxigregrze.cobblesafari.client.renderer.UnionRoomGlobeUpperBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.UNION_ROOM_SPOTLIGHT, maxigregrze.cobblesafari.client.renderer.UnionRoomSpotlightBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PUNCHINGBAG, maxigregrze.cobblesafari.client.renderer.PunchingBagBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.BALM_DISPENSER, maxigregrze.cobblesafari.client.renderer.BalmDispenserBlockEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.HIKER, HikerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CSTRADER_NPC, CsTraderEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BALLOON, BalloonEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BALLOON_SAFARI, BalloonSafariRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_MUD_BALL, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_BAIT, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_BALM, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.CSBOSS, maxigregrze.cobblesafari.client.renderer.CsBossEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CSBOSS_MINION, maxigregrze.cobblesafari.client.renderer.CsBossMinionEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CSBOSS_BULLET, maxigregrze.cobblesafari.client.renderer.CsBossBulletEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_SHADOW, maxigregrze.cobblesafari.client.renderer.AttackShadowEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_METEORITE, maxigregrze.cobblesafari.client.renderer.AttackMeteoriteEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_DISTORTION_STEM, maxigregrze.cobblesafari.client.renderer.AttackDistortionStemEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_DISTORTION_STEM_CORE, maxigregrze.cobblesafari.client.renderer.AttackDistortionStemCoreEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_WAVE, maxigregrze.cobblesafari.client.renderer.AttackWaveEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_DISTORTION_FLOWER, maxigregrze.cobblesafari.client.renderer.AttackDistortionFlowerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_GIRATINA_ORB, maxigregrze.cobblesafari.client.renderer.AttackGiratinaOrbEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_BEAM, maxigregrze.cobblesafari.client.renderer.AttackBeamEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_DIGDIRT, maxigregrze.cobblesafari.client.renderer.AttackDigdirtEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SAFARI_SHADOW_HAZARD, maxigregrze.cobblesafari.client.renderer.SafariShadowHazardEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SAFARI_BALLISTIC_METEOR, maxigregrze.cobblesafari.client.renderer.SafariBallisticMeteorEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(BalloonEntityModel.LAYER_LOCATION, BalloonEntityModel::createBodyLayer);
    }

    private void registerScreens() {
        MenuScreens.register(UndergroundMinigame.MENU_TYPE, UndergroundScreen::new);
        MenuScreens.register(BasePCMenu.MENU_TYPE, BasePCScreen::new);
    }

    private void registerDungeonMusic() {
        ClientTickEvents.END_CLIENT_TICK.register(CsMusicPlayer::onClientTick);
    }
}
