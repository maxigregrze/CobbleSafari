package maxigregrze.cobblesafari;

import maxigregrze.cobblesafari.client.effect.ClientShackleTracker;
import maxigregrze.cobblesafari.client.hud.TimerHudOverlay;
import maxigregrze.cobblesafari.init.ModBlockEntities;
import maxigregrze.cobblesafari.init.ModBlocks;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.init.ModItems;
import maxigregrze.cobblesafari.config.DimensionalBanConfig;
import maxigregrze.cobblesafari.network.ClientNetworking;
import maxigregrze.cobblesafari.network.DimensionalBanSyncPayload;
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
import maxigregrze.cobblesafari.client.DungeonDistortionDimensionEffects;
import maxigregrze.cobblesafari.client.UltraCraterDimensionEffects;
import maxigregrze.cobblesafari.mixin.client.DimensionSpecialEffectsAccessor;
import maxigregrze.cobblesafari.client.renderer.LightningSpotBlockEntityRenderer;
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
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import maxigregrze.cobblesafari.client.particle.MapleLeafParticle;
import maxigregrze.cobblesafari.init.ModParticles;
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
        // Drop the derived greyscale statue textures on a resource reload, releasing their GL handles.
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                    @Override
                    public net.minecraft.resources.ResourceLocation getFabricId() {
                        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                maxigregrze.cobblesafari.CobbleSafari.MOD_ID, "greyscale_texture_cache");
                    }

                    @Override
                    public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
                        maxigregrze.cobblesafari.client.renderer.GreyscaleTextureCache.clear();
                        // Same hook, same reason: a derived texture that outlived a reload would
                        // point at a freed image (action plan 183, section 4.2.1).
                        maxigregrze.cobblesafari.client.renderer.UltraWormholeTintCache.clear();
                    }
                });
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
        DimensionSpecialEffectsAccessor.getEffects().put(
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "dungeon_ultra_crater"),
                new UltraCraterDimensionEffects()
        );
        registerTooltips();
        DonutItemClientSetup.registerItemProperties();
        net.minecraft.client.renderer.item.ItemProperties.register(maxigregrze.cobblesafari.init.ModItems.STAKATAKA_SHIELD,
                ResourceLocation.withDefaultNamespace("blocking"),
                maxigregrze.cobblesafari.client.renderer.StakatakaShieldRenderer::blockingProperty);
        for (net.minecraft.world.item.Item canon : new net.minecraft.world.item.Item[] {
                maxigregrze.cobblesafari.init.ModItems.CELESTEELA_CANON, maxigregrze.cobblesafari.init.ModItems.KARTANA_CANON}) {
            net.minecraft.client.renderer.item.ItemProperties.register(canon,
                    ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, maxigregrze.cobblesafari.client.UltraLootItemProperties.CANON_EMPTY),
                    maxigregrze.cobblesafari.client.UltraLootItemProperties::canonEmpty);
        }
        net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
                maxigregrze.cobblesafari.init.ModItems.STAKATAKA_SHIELD,
                (stack, mode, pose, buffers, light, overlay) ->
                        maxigregrze.cobblesafari.client.renderer.StakatakaShieldRenderer.render(stack, pose, buffers, light, overlay));
        registerClientNetworking();
        registerHud();
        registerBlockRenderLayers();
        registerRenderers();
        registerColors();
        registerScreens();
        registerDungeonMusic();
        maxigregrze.cobblesafari.client.ClientBlockHooks.init();
        // Returning false skips vanilla's outline for that block (Lost Items outside creative).
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.BLOCK_OUTLINE.register((context, outline) ->
                !maxigregrze.cobblesafari.client.ClientBlockHooks.hidesBlockOutline(outline.blockState()));
        maxigregrze.cobblesafari.client.RotomPhoneModelLoadingPlugin.register();
        maxigregrze.cobblesafari.client.UltraWarpDiveModelLoadingPlugin.register();
        maxigregrze.cobblesafari.client.UltraLootModelLoadingPlugin.register();
        maxigregrze.cobblesafari.client.LostItemModelLoadingPlugin.register();
        maxigregrze.cobblesafari.client.UltraLootFabricClient.register();
        KeyBindingHelper.registerKeyBinding(maxigregrze.cobblesafari.client.rotomphone.RotomPhoneKeybind.OPEN_PHONE);
        KeyBindingHelper.registerKeyBinding(maxigregrze.cobblesafari.client.objectives.ObjectivesKeybind.TOGGLE_HUD);
        maxigregrze.cobblesafari.client.hud.HudClientConfig.reload();

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("accessories")) {
            maxigregrze.cobblesafari.compat.accessories.AccessoriesClientCompat.init();
        }


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhonePcSession.tickCleanup(client);
            maxigregrze.cobblesafari.client.rotomphone.RotoGlideClient.tick(client);
            maxigregrze.cobblesafari.client.teleporter.TeleportPadClient.tick(client);
            maxigregrze.cobblesafari.client.rotomphone.RotomPhoneKeybind.clientTick(client);
            maxigregrze.cobblesafari.client.objectives.ObjectivesKeybind.clientTick(client);
            maxigregrze.cobblesafari.client.objectives.ObjectivesHudController.clientTick(client);
            maxigregrze.cobblesafari.client.UltraLootClientTicker.onClientTick(client);
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

        ClientPlayNetworking.registerGlobalReceiver(ShackleSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientShackleTracker.accept(payload.entityId(), payload.shackled());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.RainbowFeatherUsedPayload.TYPE, (payload, context) -> {
                    context.client().execute(() ->
                            maxigregrze.cobblesafari.client.effect.ClientRainbowFeatherFx.accept(payload.entityId()));
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
                maxigregrze.cobblesafari.network.UltraLootConfigSyncPayload.TYPE,
                (payload, context) -> context.client().execute(payload::apply));

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
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CSBOSS_PLATFORM, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TINKAGEAR_BLOCK, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SAFARI_TELEPORTER, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TELEPORT_PAD, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SAFARI_EGG_NEST, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BELLTOWER_RAMP, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BELLTOWER_BEAM, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BELLTOWER_BELL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BELLTOWER_BELL_MOVING, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BELLTOWER_TOPPER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BELLTOWER_ROD, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BELLTOWER_ROOFTILES, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BELLTOWER_BRIDGE, RenderType.cutout());
        // Ultra Space set (plan 185)
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRASPACE_BONSAI_STEM, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRASPACE_BONSAI, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRASPACE_BUSH, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), ModBlocks.BUZZWOLE_PLANT, ModBlocks.PHEROMOSA_PLANT,
                ModBlocks.BUZZWOLE_PLANT_GROWN, ModBlocks.PHEROMOSA_PLANT_GROWN);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRASPACE_LEAVES, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRASPACE_CRYSTAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRASPACE_CABLE_STRAP, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRASPACE_CABLE_ZIP, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRASPACE_BULB, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRASPACE_STEM, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MEGAPOLIS_STAIRS, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POKEMON_STATUE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HOOPA_RING_PORTAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DUNGEON_PORTAL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CREATIVE_DUNGEON_PORTAL, RenderType.cutout());
        // Graded-alpha shadow texture (action plan 188, S1).
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PORTAL_SPAWNER, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DUNGEON_PORTAL_EFFECT, RenderType.translucent());
        // Translucent, not cutout: ultra_entrance_portal.png carries a graded alpha channel, and
        // cutout would round every texel to fully opaque or fully gone (action plan 183, B2).
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRA_ENTRANCE_PORTAL, RenderType.translucent());
        // Ultra Warp Dive props (plan 189): the positive's aura has graded alpha; the rocks are drawn
        // by their block entity renderer only.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRAWARPDIVE_POSITIVE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRAWARPDIVE_NEGATIVE, RenderType.cutout());
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
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.EMPTYPHONE, RenderType.cutout());
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
        // Translucent, not cutout: the lost-item skins share a ground-shadow texture with a graded
        // alpha channel, which cutout would slam to fully opaque or fully gone.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LOST_ITEM, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LOST_ITEM_VISUAL, RenderType.translucent());
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
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_ROAD, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_ROAD_PLAIN, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_BARRIER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_CONE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_IRONRAILS, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_SCAFFOLDING_PLATFORM, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_SCAFFOLDING, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_PLATFORM, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_SHUTTERS, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_SLAB, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_FLOWERPOT, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_CHAIR, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_TABLE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_LADDER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_RAMP, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_SCAFFOLDING_STAIRS, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_TRASHCAN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_TRASHCAN_LID_DISPLAY, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_LAMPPOST, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_DOOR, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_WINDOW_TOPPER, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_WINDOW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_BRICK_WINDOW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_BRICK2_WINDOW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_PLAIN_WINDOW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_CANOPY_STAIRS, RenderType.translucent());
        // Plan 194
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_HANDRAIL, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_FENCE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_BENCH, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_POTTEDPLANT, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MEGAPOLIS_BARRICADE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_CHIMNEY, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_CHIMNEY, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_PLANTER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_PLANTER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_POSTERS, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_BUSH_CUBE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_BUSH_CONE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_FOUNTAIN, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.OBSTACLE_BOULDER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.OBSTACLE_BRAMBLES, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.OBSTACLE_SLUDGE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_FOUNTAIN, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_CANOPY_RIDGE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_CANOPY_RIDGE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_CANOPY_SLAB, RenderType.translucent());

        // Kalos family (plan 163)
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_ROAD, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_ROAD_STAIRS, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_ROAD_SLAB, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_ROAD_PLAIN, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_ROAD_PLAIN_STAIRS, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_ROAD_PLAIN_SLAB, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_WINDOW_TOPPER, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_CANOPY_STAIRS, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_CANOPY_SLAB, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_WINDOW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_BRICK_WINDOW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_BRICK2_WINDOW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_PLAIN_WINDOW, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_WINDOW_TRANS, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_BRICK_WINDOW_TRANS, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_BRICK2_WINDOW_TRANS, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_PLAIN_WINDOW_TRANS, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_CANOPY_STAIRS_TRANS, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_CANOPY_SLAB_TRANS, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_WINDOW_TOPPER_TRANS, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_FLAG_SMALL, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_FLAG_LARGE, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_FLOWERS, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_BUSH, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_BUSH_FLOWERED, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_SCAFFOLDING_TUBE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_CRATE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_LEAVES, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_SAPLING, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_SAPLING_FLOWERED, RenderType.cutout());

        // Maples: leaves and leaf piles are cutoutMipped like every other foliage - the floor
        // texture of the pile has transparency, so solid() would fill the gaps in. Saplings are
        // plain cutout, matching every other cross-model plant.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RED_MAPLE_LEAVES, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ORANGE_MAPLE_LEAVES, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.YELLOW_MAPLE_LEAVES, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RED_MAPLE_SAPLING, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ORANGE_MAPLE_SAPLING, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.YELLOW_MAPLE_SAPLING, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RED_MAPLE_LEAVES_PILE, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ORANGE_MAPLE_LEAVES_PILE, RenderType.cutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.YELLOW_MAPLE_LEAVES_PILE, RenderType.cutoutMipped());

        ParticleFactoryRegistry.getInstance().register(
                ModParticles.RED_MAPLE_LEAVES, MapleLeafParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(
                ModParticles.ORANGE_MAPLE_LEAVES, MapleLeafParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(
                ModParticles.YELLOW_MAPLE_LEAVES, MapleLeafParticle.Provider::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_WOOD_DOOR, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HYPERSPACE_WOOD_TRAPDOOR, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.KALOS_MANHOLE, RenderType.cutout());
    }

    /** Vanilla's default water colour (the plains / ocean value), for items and colourless contexts. */
    private static final int HS_DEFAULT_WATER_COLOR = 0x3F76E4;

    private void registerColors() {
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                (state, level, pos, tintIndex) -> (level != null && pos != null)
                        ? net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(level, pos)
                        : net.minecraft.world.level.GrassColor.getDefaultColor(),
                ModBlocks.HYPERSPACE_FLOWERPOT, ModBlocks.HYPERSPACE_PLANTER, ModBlocks.KALOS_PLANTER);
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> net.minecraft.world.level.GrassColor.get(0.5D, 1.0D),
                ModBlocks.HYPERSPACE_FLOWERPOT, ModBlocks.HYPERSPACE_PLANTER, ModBlocks.KALOS_PLANTER);
        // Fountain water (tintindex 0): the biome's water colour, like the vanilla fluid renderer.
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                (state, level, pos, tintIndex) -> tintIndex != 0 ? -1
                        : (level != null && pos != null)
                                ? net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(level, pos)
                                : HS_DEFAULT_WATER_COLOR,
                ModBlocks.HYPERSPACE_FOUNTAIN, ModBlocks.KALOS_FOUNTAIN);
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> tintIndex != 0 ? -1 : HS_DEFAULT_WATER_COLOR,
                ModBlocks.HYPERSPACE_FOUNTAIN, ModBlocks.KALOS_FOUNTAIN);
        // Foliage-tinted nature blocks (tintindex 0 only): flowers (colored layer) and bushes.
        // The flower's untinted petal layer is tintindex 1, so leave any non-zero index untinted.
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                (state, level, pos, tintIndex) -> tintIndex != 0 ? -1
                        : (level != null && pos != null)
                                ? net.minecraft.client.renderer.BiomeColors.getAverageFoliageColor(level, pos)
                                : net.minecraft.world.level.FoliageColor.getDefaultColor(),
                ModBlocks.HYPERSPACE_FLOWERS, ModBlocks.HYPERSPACE_BUSH, ModBlocks.HYPERSPACE_BUSH_FLOWERED,
                ModBlocks.HYPERSPACE_BUSH_CUBE, ModBlocks.HYPERSPACE_BUSH_CONE);
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> tintIndex != 0 ? -1 : net.minecraft.world.level.FoliageColor.getDefaultColor(),
                ModBlocks.HYPERSPACE_FLOWERS, ModBlocks.HYPERSPACE_BUSH, ModBlocks.HYPERSPACE_BUSH_FLOWERED,
                ModBlocks.HYPERSPACE_BUSH_CUBE, ModBlocks.HYPERSPACE_BUSH_CONE);
        // Dyeable Hyperspace flags: tint the colored layer (tintindex 0) to the applied DyeColor
        // (banner / leather / collar / beacon colour). The overlay layer and any undyed flag stay untinted.
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                (state, level, pos, tintIndex) -> (tintIndex != 0 || state == null
                        || !state.getValue(maxigregrze.cobblesafari.block.hyperspace.HyperspaceFlagDye.DYED))
                        ? -1
                        : state.getValue(maxigregrze.cobblesafari.block.hyperspace.HyperspaceFlagDye.COLOR)
                                .getTextureDiffuseColor() & 0xFFFFFF,
                ModBlocks.HYPERSPACE_FLAG_SMALL, ModBlocks.HYPERSPACE_FLAG_LARGE);
        // Pokemon statue: the pedestal overlay layer (tintindex 0) takes the colour configured in the
        // block entity. Baking the overlay into the model and tinting it here - rather than drawing it
        // as a separate translucent pass - is what keeps it from z-fighting with the base layer.
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) {
                        return -1;
                    }
                    return level.getBlockEntity(pos) instanceof maxigregrze.cobblesafari.block.pokestatue.PokeStatueBlockEntity be
                            && be.hasColor() ? be.getColor() : -1;
                },
                ModBlocks.POKEMON_STATUE);
        // Dynamic skin-unlock disc: layer0 tinted to the target skin's color.
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> maxigregrze.cobblesafari.item.RotomSkinUnlockItem.computeTint(stack, tintIndex),
                maxigregrze.cobblesafari.init.ModItems.ROTOM_SKIN_UNLOCK);
    }

    private void registerRenderers() {
        BlockEntityRenderers.register(ModBlockEntities.HOOPA_RING_PORTAL, HoopaRingPortalBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.ULTRA_WORMHOLE,
                maxigregrze.cobblesafari.client.renderer.UltraWormholeBlockEntityRenderer::new);
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
        BlockEntityRenderers.register(ModBlockEntities.TRAP, maxigregrze.cobblesafari.client.renderer.TrapBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.UNION_ROOM_GLOBE_UPPER, maxigregrze.cobblesafari.client.renderer.UnionRoomGlobeUpperBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.POKEMON_STATUE, maxigregrze.cobblesafari.client.renderer.PokeStatueBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CREATIVE_STATUE, maxigregrze.cobblesafari.client.renderer.PokeStatueBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.UNION_ROOM_SPOTLIGHT, maxigregrze.cobblesafari.client.renderer.UnionRoomSpotlightBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PUNCHINGBAG, maxigregrze.cobblesafari.client.renderer.PunchingBagBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.BELLTOWER_BELL, maxigregrze.cobblesafari.client.renderer.BelltowerBellBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.HYPERSPACE_TRASHCAN, maxigregrze.cobblesafari.client.renderer.HyperspaceTrashcanBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.BALM_DISPENSER, maxigregrze.cobblesafari.client.renderer.BalmDispenserBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CSBOSS_TRIGGER, maxigregrze.cobblesafari.client.renderer.CsBossTriggerBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TELEPORT_PAD, maxigregrze.cobblesafari.client.renderer.TeleportPadBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CSBOSS_MIMIC, maxigregrze.cobblesafari.client.renderer.CsBossMimicBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.HYPERSPACE_SIGN, net.minecraft.client.renderer.blockentity.SignRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.HYPERSPACE_HANGING_SIGN, net.minecraft.client.renderer.blockentity.HangingSignRenderer::new);
        EntityRendererRegistry.register(ModEntities.HIKER, HikerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CSTRADER_NPC, CsTraderEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BALLOON, BalloonEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BALLOON_SAFARI, BalloonSafariRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_MUD_BALL, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.SEAT, net.minecraft.client.renderer.entity.NoopRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_BAIT, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_BALM, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_RED_CHAIN, maxigregrze.cobblesafari.client.renderer.ThrownRedChainEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.POIPOLE_ARROW, maxigregrze.cobblesafari.client.renderer.NeedleDartRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLACEPHALON_BOMB, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.XURKITREE_BOMB, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModEntities.CELESTEELA_PROJECTILE, ctx -> new maxigregrze.cobblesafari.client.renderer.BakedModelProjectileRenderer<>(
                ctx, maxigregrze.cobblesafari.client.renderer.UltraLootModels.CELESTEELA_PROJECTILE, 2.0f));
        EntityRendererRegistry.register(ModEntities.PAPER_PLANE, ctx -> new maxigregrze.cobblesafari.client.renderer.BakedModelProjectileRenderer<>(
                ctx, maxigregrze.cobblesafari.client.renderer.UltraLootModels.PAPER_PLANE, 1.5f, 180.0f));
        EntityRendererRegistry.register(ModEntities.CSBOSS, maxigregrze.cobblesafari.client.renderer.CsBossEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CSBOSS_MINION, maxigregrze.cobblesafari.client.renderer.CsBossMinionEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CSBOSS_BULLET, maxigregrze.cobblesafari.client.renderer.CsBossBulletEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_SHADOW, maxigregrze.cobblesafari.client.renderer.AttackShadowEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_METEORITE, maxigregrze.cobblesafari.client.renderer.AttackMeteoriteEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_DISTORTION_STEM, maxigregrze.cobblesafari.client.renderer.AttackDistortionStemEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_DISTORTION_STEM_CORE, maxigregrze.cobblesafari.client.renderer.AttackDistortionStemCoreEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_WAVE, maxigregrze.cobblesafari.client.renderer.AttackWaveEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_SHOCKWAVE, maxigregrze.cobblesafari.client.renderer.AttackShockwaveEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_DISTORTION_FLOWER, maxigregrze.cobblesafari.client.renderer.AttackDistortionFlowerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_GIRATINA_ORB, maxigregrze.cobblesafari.client.renderer.AttackGiratinaOrbEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_BEAM, maxigregrze.cobblesafari.client.renderer.AttackBeamEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_DIGDIRT, maxigregrze.cobblesafari.client.renderer.AttackDigdirtEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_PILE_PROJECTILE, maxigregrze.cobblesafari.client.renderer.AttackPileProjectileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_SHADOWBALL, maxigregrze.cobblesafari.client.renderer.AttackShadowballEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.ATTACK_RED_CHAIN, maxigregrze.cobblesafari.client.renderer.AttackRedChainEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CSBOSS_SPAWN_PROJECTILE, maxigregrze.cobblesafari.client.renderer.CsBossSpawnProjectileEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.CSBOSS_PORTAL, maxigregrze.cobblesafari.client.renderer.CsBossPortalEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SAFARI_SHADOW_HAZARD, maxigregrze.cobblesafari.client.renderer.SafariShadowHazardEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SAFARI_BALLISTIC_METEOR, maxigregrze.cobblesafari.client.renderer.SafariBallisticMeteorEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.HYPERSPACE_BOAT,
                ctx -> new maxigregrze.cobblesafari.client.renderer.HyperspaceBoatRenderer(ctx, false));
        EntityRendererRegistry.register(ModEntities.HYPERSPACE_CHEST_BOAT,
                ctx -> new maxigregrze.cobblesafari.client.renderer.HyperspaceBoatRenderer(ctx, true));
        EntityModelLayerRegistry.registerModelLayer(BalloonEntityModel.LAYER_LOCATION, BalloonEntityModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(CsTraderEntityRenderer.LAYER_LOCATION, CsTraderEntityRenderer::createBodyLayer);
    }

    private void registerScreens() {
        MenuScreens.register(UndergroundMinigame.MENU_TYPE, UndergroundScreen::new);
        MenuScreens.register(BasePCMenu.MENU_TYPE, BasePCScreen::new);
    }

    private void registerDungeonMusic() {
        ClientTickEvents.END_CLIENT_TICK.register(CsMusicPlayer::onClientTick);
    }
}
