package maxigregrze.cobblesafari;

import maxigregrze.cobblesafari.client.effect.ClientShackleTracker;
import maxigregrze.cobblesafari.client.hud.TimerHudOverlay;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.client.screen.AuspiciousPokeballConfigScreen;
import maxigregrze.cobblesafari.client.screen.AuspiciousPokeballGoldConfigScreen;
import maxigregrze.cobblesafari.client.screen.LostItemConfigScreen;
import maxigregrze.cobblesafari.client.screen.PortalSpawnerConfigScreen;
import maxigregrze.cobblesafari.network.OpenPortalSpawnerConfigPayload;
import maxigregrze.cobblesafari.network.OpenAuspiciousPokeballConfigPayload;
import maxigregrze.cobblesafari.network.OpenAuspiciousPokeballGoldConfigPayload;
import maxigregrze.cobblesafari.network.OpenLostItemConfigPayload;
import maxigregrze.cobblesafari.client.screen.TpAcceptScreen;
import maxigregrze.cobblesafari.client.screen.DistortionStoneBricksRuneScreen;
import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.network.CloseTpAcceptPayload;
import maxigregrze.cobblesafari.network.DimensionalBanSyncPayload;
import maxigregrze.cobblesafari.network.OpenTpAcceptPayload;
import maxigregrze.cobblesafari.network.OpenLostNoteBookPayload;
import maxigregrze.cobblesafari.network.OpenRuneEditorPayload;
import maxigregrze.cobblesafari.network.ShackleSyncPayload;
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
import maxigregrze.cobblesafari.client.renderer.LightningSpotBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.LostItemBlockEntityRenderer;
import maxigregrze.cobblesafari.client.renderer.VoidBlockRenderer;
import maxigregrze.cobblesafari.client.model.BalloonEntityModel;
import maxigregrze.cobblesafari.block.basepc.BasePCMenu;
import maxigregrze.cobblesafari.client.screen.BasePCScreen;
import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import maxigregrze.cobblesafari.underground.UndergroundScreen;
import maxigregrze.cobblesafari.underground.network.UndergroundPayloads;
import maxigregrze.cobblesafari.client.DungeonDistortionDimensionEffects;
import maxigregrze.cobblesafari.client.UltraCraterDimensionEffects;
import maxigregrze.cobblesafari.client.renderer.HyperspaceSkyRenderer;
import maxigregrze.cobblesafari.client.renderer.HyperspaceSkyMapTexture;
import maxigregrze.cobblesafari.client.HyperspaceDimensionEffects;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import java.io.IOException;
import maxigregrze.cobblesafari.client.audio.CsMusicPlayer;
import maxigregrze.cobblesafari.client.particle.MapleLeafParticle;
import maxigregrze.cobblesafari.init.ModParticles;
import maxigregrze.cobblesafari.client.DonutItemClientSetup;
import maxigregrze.cobblesafari.client.donut.DonutFlavorClientTooltip;
import maxigregrze.cobblesafari.item.donut.DonutTooltipPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
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
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(maxigregrze.cobblesafari.client.rotomphone.RotomPhoneKeybind.OPEN_PHONE);
        event.register(maxigregrze.cobblesafari.client.objectives.ObjectivesKeybind.TOGGLE_HUD);
        maxigregrze.cobblesafari.client.hud.HudClientConfig.reload();
    }

    @SubscribeEvent
    public static void onRegisterTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(DonutTooltipPayload.class, DonutFlavorClientTooltip::new);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(UndergroundMinigame.MENU_TYPE, UndergroundScreen::new);
        event.register(BasePCMenu.MENU_TYPE, BasePCScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_distortion"),
                new DungeonDistortionDimensionEffects()
        );
        event.register(
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_ultra_crater"),
                new UltraCraterDimensionEffects()
        );
        event.register(
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_paris"),
                new HyperspaceDimensionEffects()
        );
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), HyperspaceSkyRenderer.SHADER_ID, DefaultVertexFormat.POSITION),
                HyperspaceSkyRenderer::setShader
        );
    }

    /** Drops the derived greyscale statue textures on a resource reload, releasing their GL handles. */
    @SubscribeEvent
    public static void onRegisterClientReloadListeners(
            net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                        manager -> {
                            maxigregrze.cobblesafari.client.renderer.GreyscaleTextureCache.clear();
                            // Same hook, same reason: a derived texture that outlived a reload
                            // would point at a freed image (action plan 183, section 4.2.1).
                            maxigregrze.cobblesafari.client.renderer.UltraWormholeTintCache.clear();
                        });
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CSBOSS_PLATFORM, RenderType.translucent());
            // Translucent, not cutout: ultra_entrance_portal.png carries a graded alpha channel,
            // and cutout would round every texel to fully opaque or fully gone (plan 183, B2).
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRA_ENTRANCE_PORTAL, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.TINKAGEAR_BLOCK, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SAFARI_TELEPORTER, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.TELEPORT_PAD, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SAFARI_EGG_NEST, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BELLTOWER_RAMP, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BELLTOWER_BEAM, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BELLTOWER_BELL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BELLTOWER_BELL_MOVING, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BELLTOWER_TOPPER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BELLTOWER_ROD, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BELLTOWER_ROOFTILES, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BELLTOWER_BRIDGE, RenderType.cutout());
            // Ultra Space set (plan 185)
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRASPACE_BONSAI_STEM, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRASPACE_BONSAI, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRASPACE_BUSH, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BUZZWOLE_PLANT, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PHEROMOSA_PLANT, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BUZZWOLE_PLANT_GROWN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PHEROMOSA_PLANT_GROWN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRASPACE_LEAVES, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRASPACE_CRYSTAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRASPACE_CABLE_STRAP, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRASPACE_CABLE_ZIP, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRASPACE_BULB, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRASPACE_STEM, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MEGAPOLIS_STAIRS, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.POKEMON_STATUE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HOOPA_RING_PORTAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DUNGEON_PORTAL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CREATIVE_DUNGEON_PORTAL, RenderType.cutout());
            // Graded-alpha shadow texture (action plan 188, S1).
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PORTAL_SPAWNER, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DUNGEON_PORTAL_EFFECT, RenderType.translucent());
            // Ultra Warp Dive props (plan 189): the positive's aura has graded alpha; the rocks are
            // drawn by their block entity renderer only.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRAWARPDIVE_POSITIVE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ULTRAWARPDIVE_NEGATIVE, RenderType.cutout());
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
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DISTORTION_PORTAL_MOVING_BACK, RenderType.translucent());
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
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HAUNTED_MONITOR, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RUSTED_IRON_BLOCK, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNDERGROUND_PC, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SECRETBASE_PC, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.INCUBATOR, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.EMPTYPHONE, RenderType.cutout());
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
            // Translucent, not cutout: the lost-item skins share a ground-shadow texture with a
            // graded alpha channel, which cutout would slam to fully opaque or fully gone.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LOST_ITEM, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LOST_ITEM_VISUAL, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WHIRLWIND, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WHIRLWIND_DISPLAY, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CSBOSS_ELECTRICITY, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CSBOSS_SCORCHED, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CSBOSS_UPDRAFT, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CSBOSS_PUDDLE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MUD_PILE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SLUDGE_PILE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KARATE_MANNEQUIN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PUNCHINGBAG, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.PUNCHINGBAG_BAG_DISPLAY, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.TOMBSTONE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.TOMBSTONE_SMALL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AUSPICIOUS_POKEBALL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AUSPICIOUS_POKEBALL_DISPLAY, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AUSPICIOUS_POKEBALL_SMALL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AUSPICIOUS_POKEBALL_SMALL_DISPLAY, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AUSPICIOUS_POKEBALL_GOLD, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AUSPICIOUS_POKEBALL_GOLD_DISPLAY, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_PILLAR, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_CROWD, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_CROWD_DISPLAY, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_POKEBALL, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_POKEBALL_DISPLAY, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SPOT, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SPOT_DISPLAY, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_GLOBE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ONLINE_FEATURE_PC, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ONLINE_FEATURE_PC_UNION, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ONLINE_FEATURE_PC_GTS, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ONLINE_FEATURE_PC_WONDER, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_GLOBE_DISPLAY_MOVING, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SCREEN, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SCREEN_DISPLAY, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SCREEN_LEFT, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SCREEN_LEFT_DISPLAY, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SCREEN_RIGHT, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SCREEN_RIGHT_DISPLAY, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SPOTLIGHT_GREEN, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SPOTLIGHT_YELLOW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SPOTLIGHT_BLUE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SPOTLIGHT_RED, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_GREEN, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_YELLOW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_BLUE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.UNION_ROOM_SPOTLIGHT_DISPLAY_LIGHT_RED, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_ROAD, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_ROAD_PLAIN, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_BARRIER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_CONE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_IRONRAILS, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_SCAFFOLDING_PLATFORM, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_SCAFFOLDING, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_PLATFORM, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_SHUTTERS, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_SLAB, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_FLOWERPOT, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_CHAIR, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_TABLE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_LADDER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_RAMP, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_SCAFFOLDING_STAIRS, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_TRASHCAN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_TRASHCAN_LID_DISPLAY, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_LAMPPOST, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_DOOR, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_WINDOW_TOPPER, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_WINDOW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_BRICK_WINDOW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_BRICK2_WINDOW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_PLAIN_WINDOW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_CANOPY_STAIRS, RenderType.translucent());
            // Plan 194
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_HANDRAIL, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_FENCE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_BENCH, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_POTTEDPLANT, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MEGAPOLIS_BARRICADE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_CHIMNEY, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_CHIMNEY, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_PLANTER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_PLANTER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_POSTERS, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_BUSH_CUBE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_BUSH_CONE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_FOUNTAIN, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.OBSTACLE_BOULDER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.OBSTACLE_BRAMBLES, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.OBSTACLE_SLUDGE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.OBSTACLE_SHARDCLUSTER, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_FOUNTAIN, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_CANOPY_RIDGE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_CANOPY_RIDGE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_CANOPY_SLAB, RenderType.translucent());

            // Kalos family (plan 163)
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_ROAD, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_ROAD_STAIRS, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_ROAD_SLAB, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_ROAD_PLAIN, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_ROAD_PLAIN_STAIRS, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_ROAD_PLAIN_SLAB, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_WINDOW_TOPPER, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_CANOPY_STAIRS, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_CANOPY_SLAB, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_WINDOW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_BRICK_WINDOW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_BRICK2_WINDOW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_PLAIN_WINDOW, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_WINDOW_TRANS, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_BRICK_WINDOW_TRANS, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_BRICK2_WINDOW_TRANS, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_PLAIN_WINDOW_TRANS, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_CANOPY_STAIRS_TRANS, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_CANOPY_SLAB_TRANS, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_WINDOW_TOPPER_TRANS, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_FLAG_SMALL, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_FLAG_LARGE, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_FLOWERS, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_BUSH, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_BUSH_FLOWERED, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_SCAFFOLDING_TUBE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_CRATE, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_LEAVES, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_SAPLING, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_SAPLING_FLOWERED, RenderType.cutout());

            // Maples: leaves and leaf piles are cutoutMipped like every other foliage - the floor
            // texture of the pile has transparency, so solid() would fill the gaps in. Saplings are
            // plain cutout, matching every other cross-model plant.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RED_MAPLE_LEAVES, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ORANGE_MAPLE_LEAVES, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.YELLOW_MAPLE_LEAVES, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RED_MAPLE_SAPLING, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ORANGE_MAPLE_SAPLING, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.YELLOW_MAPLE_SAPLING, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.RED_MAPLE_LEAVES_PILE, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ORANGE_MAPLE_LEAVES_PILE, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.YELLOW_MAPLE_LEAVES_PILE, RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_WOOD_DOOR, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.HYPERSPACE_WOOD_TRAPDOOR, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.KALOS_MANHOLE, RenderType.cutout());

            BlockEntityRenderers.register(ModBlockEntities.HOOPA_RING_PORTAL, HoopaRingPortalBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ULTRA_WORMHOLE,
                    maxigregrze.cobblesafari.client.renderer.UltraWormholeBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ULTRA_ENTRANCE_PORTAL,
                    maxigregrze.cobblesafari.client.renderer.UltraEntrancePortalBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.DUNGEON_PORTAL, DungeonPortalBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.VOID_BLOCK, VoidBlockRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.GIRATINA_CORE, GiratinaCoreBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ULTRAWARPDIVE,
                    maxigregrze.cobblesafari.client.renderer.UltraWarpDiveBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.DISTORTION_PORTAL, DistortionPortalBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.LOST_ITEM, LostItemBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.LIGHTNINGSPOT, LightningSpotBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.AUSPICIOUS_POKEBALL, AuspiciousPokeballBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.AUSPICIOUS_POKEBALL_GOLD, AuspiciousPokeballBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.UNION_ROOM_DECOR, maxigregrze.cobblesafari.client.renderer.UnionRoomDecorBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.WHIRLWIND, maxigregrze.cobblesafari.client.renderer.WhirlwindBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CSBOSS_UPDRAFT, maxigregrze.cobblesafari.client.renderer.CsBossUpdraftTileRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.TRAP, maxigregrze.cobblesafari.client.renderer.TrapBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.UNION_ROOM_GLOBE_UPPER, maxigregrze.cobblesafari.client.renderer.UnionRoomGlobeUpperBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.POKEMON_STATUE, maxigregrze.cobblesafari.client.renderer.PokeStatueBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CREATIVE_STATUE, maxigregrze.cobblesafari.client.renderer.PokeStatueBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.UNION_ROOM_SPOTLIGHT, maxigregrze.cobblesafari.client.renderer.UnionRoomSpotlightBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PUNCHINGBAG, maxigregrze.cobblesafari.client.renderer.PunchingBagBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.BELLTOWER_BELL, maxigregrze.cobblesafari.client.renderer.BelltowerBellBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.BALM_DISPENSER, maxigregrze.cobblesafari.client.renderer.BalmDispenserBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CSBOSS_TRIGGER, maxigregrze.cobblesafari.client.renderer.CsBossTriggerBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.TELEPORT_PAD, maxigregrze.cobblesafari.client.renderer.TeleportPadBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CSBOSS_MIMIC, maxigregrze.cobblesafari.client.renderer.CsBossMimicBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.HYPERSPACE_TRASHCAN, maxigregrze.cobblesafari.client.renderer.HyperspaceTrashcanBlockEntityRenderer::new);
            // Sheets builds SIGN_MATERIALS / HANGING_SIGN_MATERIALS from WoodType.values() at class load,
            // which happens before common setup registers HYPERSPACE - without this the sign material is null.
            net.minecraft.client.renderer.Sheets.addWoodType(maxigregrze.cobblesafari.init.ModWoodTypes.HYPERSPACE);
            BlockEntityRenderers.register(ModBlockEntities.HYPERSPACE_SIGN, net.minecraft.client.renderer.blockentity.SignRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.HYPERSPACE_HANGING_SIGN, net.minecraft.client.renderer.blockentity.HangingSignRenderer::new);
        });

        event.enqueueWork(DonutItemClientSetup::registerItemProperties);
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(
                maxigregrze.cobblesafari.init.ModItems.STAKATAKA_SHIELD,
                ResourceLocation.withDefaultNamespace("blocking"),
                maxigregrze.cobblesafari.client.renderer.StakatakaShieldRenderer::blockingProperty));
        event.enqueueWork(() -> {
            for (net.minecraft.world.item.Item canon : new net.minecraft.world.item.Item[] {
                    maxigregrze.cobblesafari.init.ModItems.CELESTEELA_CANON, maxigregrze.cobblesafari.init.ModItems.KARTANA_CANON}) {
                net.minecraft.client.renderer.item.ItemProperties.register(canon,
                        ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, maxigregrze.cobblesafari.client.UltraLootItemProperties.CANON_EMPTY),
                        maxigregrze.cobblesafari.client.UltraLootItemProperties::canonEmpty);
            }
        });
        event.enqueueWork(maxigregrze.cobblesafari.client.ClientBlockHooks::init);

        NeoForge.EVENT_BUS.addListener(CobbleSafariClientNeoForge::onRenderGuiLayer);
        NeoForge.EVENT_BUS.addListener(CobbleSafariClientNeoForge::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(CobbleSafariClientNeoForge::onClientTick);
        NeoForge.EVENT_BUS.addListener(CobbleSafariClientNeoForge::onRenderBlockHighlight);

        if (net.neoforged.fml.ModList.get().isLoaded("accessories")) {
            maxigregrze.cobblesafari.compat.accessories.AccessoriesClientCompat.init();
        }
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(
            net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.RED_MAPLE_LEAVES, MapleLeafParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ORANGE_MAPLE_LEAVES, MapleLeafParticle.Provider::new);
        event.registerSpriteSet(ModParticles.YELLOW_MAPLE_LEAVES, MapleLeafParticle.Provider::new);
    }

    /** Vanilla's default water colour (the plains / ocean value), for items and colourless contexts. */
    private static final int HS_DEFAULT_WATER_COLOR = 0x3F76E4;

    @SubscribeEvent
    public static void onRegisterBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> (level != null && pos != null)
                        ? net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(level, pos)
                        : net.minecraft.world.level.GrassColor.getDefaultColor(),
                ModBlocks.HYPERSPACE_FLOWERPOT, ModBlocks.HYPERSPACE_PLANTER, ModBlocks.KALOS_PLANTER);
        // Fountain water (tintindex 0): the biome's water colour, like the vanilla fluid renderer.
        event.register(
                (state, level, pos, tintIndex) -> tintIndex != 0 ? -1
                        : (level != null && pos != null)
                                ? net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(level, pos)
                                : HS_DEFAULT_WATER_COLOR,
                ModBlocks.HYPERSPACE_FOUNTAIN, ModBlocks.KALOS_FOUNTAIN, ModBlocks.CSBOSS_PUDDLE);
        // Foliage-tinted nature blocks (tintindex 0 only): flowers (colored layer) and bushes.
        // The flower's untinted petal layer is tintindex 1, so leave any non-zero index untinted.
        event.register(
                (state, level, pos, tintIndex) -> tintIndex != 0 ? -1
                        : (level != null && pos != null)
                                ? net.minecraft.client.renderer.BiomeColors.getAverageFoliageColor(level, pos)
                                : net.minecraft.world.level.FoliageColor.getDefaultColor(),
                ModBlocks.HYPERSPACE_FLOWERS, ModBlocks.HYPERSPACE_BUSH, ModBlocks.HYPERSPACE_BUSH_FLOWERED,
                ModBlocks.HYPERSPACE_BUSH_CUBE, ModBlocks.HYPERSPACE_BUSH_CONE);
        // Dyeable Hyperspace flags: tint the colored layer (tintindex 0) to the applied DyeColor
        // (banner / leather / collar / beacon colour). The overlay layer and any undyed flag stay untinted.
        event.register(
                (state, level, pos, tintIndex) -> (tintIndex != 0 || state == null
                        || !state.getValue(maxigregrze.cobblesafari.block.hyperspace.HyperspaceFlagDye.DYED))
                        ? -1
                        : state.getValue(maxigregrze.cobblesafari.block.hyperspace.HyperspaceFlagDye.COLOR)
                                .getTextureDiffuseColor() & 0xFFFFFF,
                ModBlocks.HYPERSPACE_FLAG_SMALL, ModBlocks.HYPERSPACE_FLAG_LARGE);
        // Pokemon statue: the pedestal overlay layer (tintindex 0) takes the colour configured in the
        // block entity. Baking the overlay into the model and tinting it here - rather than drawing it
        // as a separate translucent pass - is what keeps it from z-fighting with the base layer.
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) {
                        return -1;
                    }
                    return level.getBlockEntity(pos) instanceof maxigregrze.cobblesafari.block.pokestatue.PokeStatueBlockEntity be
                            && be.hasColor() ? be.getColor() : -1;
                },
                ModBlocks.POKEMON_STATUE);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> net.minecraft.world.level.GrassColor.get(0.5D, 1.0D),
                ModBlocks.HYPERSPACE_FLOWERPOT, ModBlocks.HYPERSPACE_PLANTER, ModBlocks.KALOS_PLANTER);
        event.register(
                (stack, tintIndex) -> tintIndex != 0 ? -1 : HS_DEFAULT_WATER_COLOR,
                ModBlocks.HYPERSPACE_FOUNTAIN, ModBlocks.KALOS_FOUNTAIN);
        event.register(
                (stack, tintIndex) -> tintIndex != 0 ? -1 : net.minecraft.world.level.FoliageColor.getDefaultColor(),
                ModBlocks.HYPERSPACE_FLOWERS, ModBlocks.HYPERSPACE_BUSH, ModBlocks.HYPERSPACE_BUSH_FLOWERED,
                ModBlocks.HYPERSPACE_BUSH_CUBE, ModBlocks.HYPERSPACE_BUSH_CONE);
        // Dynamic skin-unlock disc: layer0 tinted to the target skin's color.
        event.register(
                (stack, tintIndex) -> maxigregrze.cobblesafari.item.RotomSkinUnlockItem.computeTint(stack, tintIndex),
                maxigregrze.cobblesafari.init.ModItems.ROTOM_SKIN_UNLOCK);
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HYPERSPACE_BOAT,
                ctx -> new maxigregrze.cobblesafari.client.renderer.HyperspaceBoatRenderer(ctx, false));
        event.registerEntityRenderer(ModEntities.HYPERSPACE_CHEST_BOAT,
                ctx -> new maxigregrze.cobblesafari.client.renderer.HyperspaceBoatRenderer(ctx, true));
        event.registerEntityRenderer(ModEntities.HIKER, HikerEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CSTRADER_NPC, CsTraderEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.BALLOON, BalloonEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.BALLOON_SAFARI, BalloonSafariRenderer::new);
        event.registerEntityRenderer(ModEntities.SAFARI_DRONE, maxigregrze.cobblesafari.client.renderer.SafariDroneRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_MUD_BALL, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAT, net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.CARRIER, net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_BAIT, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_BALM, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_RED_CHAIN, maxigregrze.cobblesafari.client.renderer.ThrownRedChainEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.POIPOLE_ARROW, maxigregrze.cobblesafari.client.renderer.NeedleDartRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACEPHALON_BOMB, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.XURKITREE_BOMB, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.CELESTEELA_PROJECTILE, ctx -> new maxigregrze.cobblesafari.client.renderer.BakedModelProjectileRenderer<>(
                ctx, maxigregrze.cobblesafari.client.renderer.UltraLootModels.CELESTEELA_PROJECTILE, 2.0f));
        event.registerEntityRenderer(ModEntities.PAPER_PLANE, ctx -> new maxigregrze.cobblesafari.client.renderer.BakedModelProjectileRenderer<>(
                ctx, maxigregrze.cobblesafari.client.renderer.UltraLootModels.PAPER_PLANE, 1.5f, 180.0f));
        event.registerEntityRenderer(ModEntities.CSBOSS, maxigregrze.cobblesafari.client.renderer.CsBossEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CSBOSS_MINION, maxigregrze.cobblesafari.client.renderer.CsBossMinionEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CSBOSS_BULLET, maxigregrze.cobblesafari.client.renderer.CsBossBulletEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_SHADOW, maxigregrze.cobblesafari.client.renderer.AttackShadowEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_METEORITE, maxigregrze.cobblesafari.client.renderer.AttackMeteoriteEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_DISTORTION_STEM, maxigregrze.cobblesafari.client.renderer.AttackDistortionStemEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_DISTORTION_STEM_CORE, maxigregrze.cobblesafari.client.renderer.AttackDistortionStemCoreEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_WAVE, maxigregrze.cobblesafari.client.renderer.AttackWaveEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_SHOCKWAVE, maxigregrze.cobblesafari.client.renderer.AttackShockwaveEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_BELL, maxigregrze.cobblesafari.client.renderer.AttackBellEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_TORNADO, maxigregrze.cobblesafari.client.renderer.AttackTornadoEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_AIR_SLASH, maxigregrze.cobblesafari.client.renderer.AttackAirSlashEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_ZONE_RING, maxigregrze.cobblesafari.client.renderer.AttackZoneRingEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_LANE_SHADOW, maxigregrze.cobblesafari.client.renderer.AttackLaneShadowEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_DISTORTION_FLOWER, maxigregrze.cobblesafari.client.renderer.AttackDistortionFlowerEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_GIRATINA_ORB, maxigregrze.cobblesafari.client.renderer.AttackGiratinaOrbEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_BEAM, maxigregrze.cobblesafari.client.renderer.AttackBeamEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_DIGDIRT, maxigregrze.cobblesafari.client.renderer.AttackDigdirtEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_PILE_PROJECTILE, maxigregrze.cobblesafari.client.renderer.AttackPileProjectileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_SHADOWBALL, maxigregrze.cobblesafari.client.renderer.AttackShadowballEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ATTACK_RED_CHAIN, maxigregrze.cobblesafari.client.renderer.AttackRedChainEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CSBOSS_SPAWN_PROJECTILE, maxigregrze.cobblesafari.client.renderer.CsBossSpawnProjectileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CSBOSS_PORTAL, maxigregrze.cobblesafari.client.renderer.CsBossPortalEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.SAFARI_SHADOW_HAZARD, maxigregrze.cobblesafari.client.renderer.SafariShadowHazardEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.SAFARI_BALLISTIC_METEOR, maxigregrze.cobblesafari.client.renderer.SafariBallisticMeteorEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BalloonEntityModel.LAYER_LOCATION, BalloonEntityModel::createBodyLayer);
        event.registerLayerDefinition(maxigregrze.cobblesafari.client.model.AttackBellModel.LAYER_LOCATION, maxigregrze.cobblesafari.client.model.AttackBellModel::createBodyLayer);
        event.registerLayerDefinition(CsTraderEntityRenderer.LAYER_LOCATION, CsTraderEntityRenderer::createBodyLayer);
    }

    /** Lost Items outside creative: no selection box, the block stays clickable. */
    public static void onRenderBlockHighlight(net.neoforged.neoforge.client.event.RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && maxigregrze.cobblesafari.client.ClientBlockHooks.hidesBlockOutline(
                mc.level.getBlockState(event.getTarget().getBlockPos()))) {
            event.setCanceled(true);
        }
    }

    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        CsMusicPlayer.onClientTick(mc);
        maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhonePcSession.tickCleanup(mc);
        maxigregrze.cobblesafari.client.rotomphone.RotoGlideClient.tick(mc);
        maxigregrze.cobblesafari.client.teleporter.TeleportPadClient.tick(mc);
        maxigregrze.cobblesafari.client.obstacle.ShardClusterClient.tick(mc);
        maxigregrze.cobblesafari.client.rotomphone.RotomPhoneKeybind.clientTick(mc);
        maxigregrze.cobblesafari.client.objectives.ObjectivesKeybind.clientTick(mc);
        maxigregrze.cobblesafari.client.objectives.ObjectivesHudController.clientTick(mc);
        maxigregrze.cobblesafari.client.UltraLootClientTicker.onClientTick(mc);
    }

    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            TimerHudOverlay.renderText(event.getGuiGraphics(), event.getPartialTick());
            maxigregrze.cobblesafari.client.hud.ObjectivesHudOverlay.render(event.getGuiGraphics(), event.getPartialTick());
        }
    }

    public static void handleObjectivesHudSync(
            maxigregrze.cobblesafari.network.ObjectivesHudSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                maxigregrze.cobblesafari.client.objectives.ObjectivesHudController.accept(payload));
    }

    public static void handleHyperspaceSkyMap(
            maxigregrze.cobblesafari.network.HyperspaceSkyMapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> HyperspaceSkyMapTexture.accept(payload));
    }

    public static void handleHudConfigRefresh(
            maxigregrze.cobblesafari.network.HudConfigRefreshPayload payload, IPayloadContext context) {
        context.enqueueWork(maxigregrze.cobblesafari.client.hud.HudClientConfig::reload);
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

    public static void handleShackleSync(ShackleSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientShackleTracker.accept(payload.entityId(), payload.shackled());
        });
    }

    public static void handleRainbowFeatherUsed(
            maxigregrze.cobblesafari.network.RainbowFeatherUsedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            maxigregrze.cobblesafari.client.effect.ClientRainbowFeatherFx.accept(payload.entityId());
        });
    }

    public static void handleRotomPhoneActivation(
            maxigregrze.cobblesafari.network.RotomPhoneActivationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            maxigregrze.cobblesafari.client.effect.ClientRotomPhoneFx.accept(payload.device());
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

    public static void handleOpenLostItemConfig(OpenLostItemConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new LostItemConfigScreen(payload));
        });
    }

    public static void handleOpenPortalSpawnerConfig(OpenPortalSpawnerConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new PortalSpawnerConfigScreen(payload)));
    }

    public static void handleOpenOnboarding(maxigregrze.cobblesafari.network.OpenOnboardingPayload payload,
                                            IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new maxigregrze.cobblesafari.client.screen.OnboardingScreen(payload.initial())));
    }

    public static void handleOpenAuspiciousPokeballConfig(OpenAuspiciousPokeballConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new AuspiciousPokeballConfigScreen(payload));
        });
    }

    public static void handleOpenCsBossTriggerConfig(
            maxigregrze.cobblesafari.network.OpenCsBossTriggerConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                Minecraft.getInstance().setScreen(
                        new maxigregrze.cobblesafari.client.screen.CsBossTriggerConfigScreen(payload)));
    }

    public static void handleOpenCsBossMimicConfig(
            maxigregrze.cobblesafari.network.OpenCsBossMimicConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                Minecraft.getInstance().setScreen(
                        new maxigregrze.cobblesafari.client.screen.CsBossMimicConfigScreen(payload)));
    }

    public static void handleOpenPokeStatueConfig(
            maxigregrze.cobblesafari.network.OpenPokeStatueConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                Minecraft.getInstance().setScreen(
                        new maxigregrze.cobblesafari.client.screen.PokeStatueConfigScreen(payload)));
    }

    public static void handleOpenTeleportPadConfig(
            maxigregrze.cobblesafari.network.OpenTeleportPadConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                Minecraft.getInstance().setScreen(
                        new maxigregrze.cobblesafari.client.screen.TeleportPadConfigScreen(payload)));
    }

    public static void handleTeleportPadResult(
            maxigregrze.cobblesafari.network.TeleportPadResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen
                    instanceof maxigregrze.cobblesafari.client.screen.TeleportPadConfigScreen screen) {
                screen.applyResult(payload);
            }
        });
    }

    public static void handleSetCsMusic(
            maxigregrze.cobblesafari.network.SetCsMusicPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CsMusicPlayer.accept(payload));
    }

    public static void handleOpenAuspiciousPokeballGoldConfig(OpenAuspiciousPokeballGoldConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new AuspiciousPokeballGoldConfigScreen(payload));
        });
    }

    public static void handleOpenLostNoteBook(OpenLostNoteBookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BookViewScreen.BookAccess access = BookViewScreen.BookAccess.fromItem(payload.book());
            Minecraft.getInstance().setScreen(new BookViewScreen(access));
        });
    }

    public static void handleOpenRotomPhone(maxigregrze.cobblesafari.network.OpenRotomPhonePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            maxigregrze.cobblesafari.rotomphone.RotomPhoneClientCache
                    .setCurrentWallpaperEnabled(payload.customWallpaper());
            Minecraft.getInstance().setScreen(new maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneMenuScreen(
                    payload.rotomName(), payload.shinyStatus(), payload.currentSkin(), payload.safetyMode(), payload.rotoGlide()));
        });
    }

    public static void handleOpenEmptyPhoneConfirm(maxigregrze.cobblesafari.network.OpenEmptyPhoneConfirmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new maxigregrze.cobblesafari.client.screen.rotomphone.EmptyPhoneConfirmScreen(
                    payload.rotomName(), payload.rotomLevel(), payload.rotomIsShiny(),
                    payload.speciesId(), payload.aspects()));
        });
    }

    public static void handleCloseEmptyPhoneConfirm(maxigregrze.cobblesafari.network.CloseEmptyPhoneConfirmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen
                    instanceof maxigregrze.cobblesafari.client.screen.rotomphone.EmptyPhoneConfirmScreen confirmScreen) {
                confirmScreen.closeFromServer();
            }
        });
    }

    public static void handleRotomPhoneConfigSync(maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            maxigregrze.cobblesafari.rotomphone.RotomPhoneClientCache.applySyncData(payload);
        });
    }

    public static void handleUnionAppResult(maxigregrze.cobblesafari.network.UnionAppResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.subscreen() == maxigregrze.cobblesafari.network.UnionAppResultPayload.SUB_CLOSE_GUI) {
                Minecraft.getInstance().setScreen(null);
                return;
            }
            if (Minecraft.getInstance().screen
                    instanceof maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneUnionScreen ru) {
                ru.applyServerSnapshot(payload);
            }
        });
    }

    public static void handleWonderAppResult(maxigregrze.cobblesafari.network.WonderAppResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen
                    instanceof maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneWonderScreen rw) {
                rw.applyServerSnapshot(payload);
            }
        });
    }

    public static void handleGtsAppResult(maxigregrze.cobblesafari.network.GtsAppResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen
                    instanceof maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneGTSScreen rg) {
                rg.applyServerSnapshot(payload);
            }
        });
    }

    public static void handleChatConversationSync(maxigregrze.cobblesafari.network.ChatConversationSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                maxigregrze.cobblesafari.rotomphone.ChatConversationClientCache.setConversations(payload.conversations()));
    }

    public static void handleRotomPhoneNotification(maxigregrze.cobblesafari.network.RotomPhoneNotificationPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                maxigregrze.cobblesafari.rotomphone.RotomPhoneNotificationCache.apply(payload));
    }

    public static void handleChatAppResult(maxigregrze.cobblesafari.network.ChatAppResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen
                    instanceof maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneChatScreen rc) {
                rc.applyServerSnapshot(payload);
            }
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
