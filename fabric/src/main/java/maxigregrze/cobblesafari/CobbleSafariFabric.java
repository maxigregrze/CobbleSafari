package maxigregrze.cobblesafari;

import maxigregrze.cobblesafari.command.CobbleSafariCommand;
import maxigregrze.cobblesafari.command.SafariExitCommand;
import maxigregrze.cobblesafari.dungeon.DungeonTeleportCountdown;
import maxigregrze.cobblesafari.entity.BalloonSpawnHandler;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.event.DimensionEvents;
import maxigregrze.cobblesafari.event.DimensionalBanEventHandler;
import maxigregrze.cobblesafari.dungeon.DungeonTpAcceptHandler;
import maxigregrze.cobblesafari.network.CloseTpAcceptPayload;
import maxigregrze.cobblesafari.network.DimensionalBanSyncPayload;
import maxigregrze.cobblesafari.network.OpenTpAcceptPayload;
import maxigregrze.cobblesafari.network.TimerSyncPayload;
import maxigregrze.cobblesafari.network.TpAcceptResponsePayload;
import maxigregrze.cobblesafari.teleporter.TeleporterTickHandler;
import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import maxigregrze.cobblesafari.underground.network.UndergroundNetworking;
import maxigregrze.cobblesafari.underground.network.UndergroundPayloads;
import maxigregrze.cobblesafari.underground.screen.UndergroundOpenData;
import maxigregrze.cobblesafari.underground.screen.UndergroundScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;

public class CobbleSafariFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CobbleSafari.init();

        registerMenuType();
        registerEntityAttributes();
        registerNetworking();
        registerCommands();
        registerEvents();

        CobbleSafari.LOGGER.info("CobbleSafari Fabric module loaded!");
    }

    private void registerMenuType() {
        UndergroundMinigame.MENU_TYPE = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "underground_mining"),
                new ExtendedScreenHandlerType<>(UndergroundScreenHandler::new, UndergroundOpenData.STREAM_CODEC)
        );

        maxigregrze.cobblesafari.block.basepc.BasePCMenu.MENU_TYPE = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "base_pc"),
                new net.minecraft.world.inventory.MenuType<>(maxigregrze.cobblesafari.block.basepc.BasePCMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET)
        );
    }

    private void registerEntityAttributes() {
        FabricDefaultAttributeRegistry.register(ModEntities.HIKER, ModEntities.getHikerAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.BALLOON, ModEntities.getBalloonAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.BALLOON_SAFARI, ModEntities.getBalloonSafariAttributes());
    }

    private void registerNetworking() {
        PayloadTypeRegistry.playS2C().register(TimerSyncPayload.TYPE, TimerSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenTpAcceptPayload.TYPE, OpenTpAcceptPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CloseTpAcceptPayload.TYPE, CloseTpAcceptPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(DimensionalBanSyncPayload.TYPE, DimensionalBanSyncPayload.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(
                TpAcceptResponsePayload.TYPE,
                TpAcceptResponsePayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                UndergroundPayloads.MineActionPayload.TYPE,
                UndergroundPayloads.MineActionPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                UndergroundPayloads.SwitchToolPayload.TYPE,
                UndergroundPayloads.SwitchToolPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                UndergroundPayloads.GridUpdatePayload.TYPE,
                UndergroundPayloads.GridUpdatePayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                UndergroundPayloads.StabilityUpdatePayload.TYPE,
                UndergroundPayloads.StabilityUpdatePayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                UndergroundPayloads.TreasureRevealedPayload.TYPE,
                UndergroundPayloads.TreasureRevealedPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                UndergroundPayloads.GameEndPayload.TYPE,
                UndergroundPayloads.GameEndPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                UndergroundPayloads.PlaySoundPayload.TYPE,
                UndergroundPayloads.PlaySoundPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                UndergroundPayloads.TreasureRegistrySyncPayload.TYPE,
                UndergroundPayloads.TreasureRegistrySyncPayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                TpAcceptResponsePayload.TYPE,
                (payload, context) -> {
                    context.server().execute(() -> {
                        if ("dungeon".equals(payload.source())) {
                            DungeonTpAcceptHandler.handleAcceptResponse(context.player(), payload.accepted());
                        } else {
                            TeleporterTickHandler.handleAcceptResponse(context.player(), payload.accepted());
                        }
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                UndergroundPayloads.MineActionPayload.TYPE,
                (payload, context) -> {
                    context.server().execute(() -> {
                        UndergroundNetworking.handleMineAction(context.player(), payload);
                    });
                }
        );
        ServerPlayNetworking.registerGlobalReceiver(
                UndergroundPayloads.SwitchToolPayload.TYPE,
                (payload, context) -> {
                    context.server().execute(() -> {
                        UndergroundNetworking.handleToolSwitch(context.player(), payload);
                    });
                }
        );
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CobbleSafariCommand.registerCommands(dispatcher, registryAccess);
            SafariExitCommand.registerCommands(dispatcher, registryAccess);
        });
    }

    private void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(DimensionEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(DimensionEvents::onServerStopping);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                UndergroundMinigame.loadDatapacks(server);
                UndergroundMinigame.syncRegistryToAllPlayers(server);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            DimensionEvents.onServerTick(server);
            TeleporterTickHandler.onServerTick(server);
            DungeonTeleportCountdown.onServerTick(server);
            BalloonSpawnHandler.onServerTick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            DimensionEvents.onPlayerJoin(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            DimensionEvents.onPlayerDisconnect(handler.getPlayer());
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            return DimensionalBanEventHandler.onUseItem(player, world, hand);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            InteractionResult cauldronResult = maxigregrze.cobblesafari.safari.MudBallCauldronHandler.onUseBlock(player, world, hand, hitResult);
            if (cauldronResult != InteractionResult.PASS) {
                return cauldronResult;
            }
            return DimensionalBanEventHandler.onUseBlock(player, world, hand, hitResult);
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            return DimensionalBanEventHandler.onAttackBlock(player, world, hand, pos, direction);
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            return DimensionalBanEventHandler.onBlockBreakTry(world.dimension(), player, pos);
        });
    }
}
