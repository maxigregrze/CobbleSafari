package maxigregrze.cobblesafari;

import maxigregrze.cobblesafari.command.CobbleSafariCommand;
import maxigregrze.cobblesafari.command.SafariExitCommand;
import maxigregrze.cobblesafari.item.LuckyMiningHelmetItem;
import maxigregrze.cobblesafari.dungeon.DungeonTeleportCountdown;
import maxigregrze.cobblesafari.entity.BalloonSpawnHandler;
import maxigregrze.cobblesafari.init.ModEntities;
import maxigregrze.cobblesafari.event.DimensionEvents;
import maxigregrze.cobblesafari.event.DimensionTimerDeathHandler;
import maxigregrze.cobblesafari.event.DimensionalBanEventHandler;
import maxigregrze.cobblesafari.dungeon.DungeonTpAcceptHandler;
import maxigregrze.cobblesafari.network.CloseTpAcceptPayload;
import maxigregrze.cobblesafari.network.DimensionalBanSyncPayload;
import maxigregrze.cobblesafari.network.OpenTpAcceptPayload;
import maxigregrze.cobblesafari.network.OpenLostNoteBookPayload;
import maxigregrze.cobblesafari.network.OpenRuneEditorPayload;
import maxigregrze.cobblesafari.network.OpenLostItemConfigPayload;
import maxigregrze.cobblesafari.network.OpenAuspiciousPokeballConfigPayload;
import maxigregrze.cobblesafari.network.OpenAuspiciousPokeballGoldConfigPayload;
import maxigregrze.cobblesafari.network.SaveLostItemConfigPayload;
import maxigregrze.cobblesafari.network.SaveAuspiciousPokeballConfigPayload;
import maxigregrze.cobblesafari.network.SaveAuspiciousPokeballGoldConfigPayload;
import maxigregrze.cobblesafari.network.LostItemResetClaimsPayload;
import maxigregrze.cobblesafari.network.AuspiciousPokeballResetClaimsPayload;
import maxigregrze.cobblesafari.network.LostItemConfigServerHandler;
import maxigregrze.cobblesafari.network.AuspiciousPokeballConfigServerHandler;
import maxigregrze.cobblesafari.network.AuspiciousPokeballGoldConfigServerHandler;
import maxigregrze.cobblesafari.network.SaveRuneTextPayload;
import maxigregrze.cobblesafari.network.TimerSyncPayload;
import maxigregrze.cobblesafari.network.TpAcceptResponsePayload;
import maxigregrze.cobblesafari.block.distortion.DistortionStoneBricksRuneBlockEntity;
import maxigregrze.cobblesafari.teleporter.TeleporterTickHandler;
import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import maxigregrze.cobblesafari.underground.network.UndergroundNetworking;
import maxigregrze.cobblesafari.underground.network.UndergroundPayloads;
import maxigregrze.cobblesafari.underground.screen.UndergroundOpenData;
import maxigregrze.cobblesafari.underground.screen.UndergroundScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(CobbleSafari.MOD_ID)
public class CobbleSafariNeoForge {

    private boolean registriesInitialized = false;

    public CobbleSafariNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onRegisterPayloads);
        modEventBus.addListener(this::onEntityAttributeCreation);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);

        CobbleSafari.LOGGER.info("CobbleSafari NeoForge module loaded!");
    }

    private void onRegister(RegisterEvent event) {
        if (!registriesInitialized) {
            registriesInitialized = true;
            CobbleSafari.initRegistries();
            registerMenuType();
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CobbleSafari::initLogic);
        if (net.neoforged.fml.ModList.get().isLoaded("accessories")) {
            event.enqueueWork(maxigregrze.cobblesafari.compat.accessories.AccessoriesCompat::registerAccessoryItem);
        }
    }

    private void registerMenuType() {
        UndergroundMinigame.MENU_TYPE = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "underground_mining"),
                IMenuTypeExtension.create((syncId, inv, buf) -> {
                    UndergroundOpenData data = UndergroundOpenData.STREAM_CODEC.decode(buf);
                    return new UndergroundScreenHandler(syncId, inv, data);
                })
        );

        maxigregrze.cobblesafari.block.basepc.BasePCMenu.MENU_TYPE = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(CobbleSafari.MOD_ID, "base_pc"),
                IMenuTypeExtension.create((syncId, inv, buf) ->
                        new maxigregrze.cobblesafari.block.basepc.BasePCMenu(syncId, inv))
        );
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.HIKER, ModEntities.getHikerAttributes().build());
        event.put(ModEntities.CSTRADER_NPC, ModEntities.getCsTraderAttributes().build());
        event.put(ModEntities.BALLOON, ModEntities.getBalloonAttributes().build());
        event.put(ModEntities.BALLOON_SAFARI, ModEntities.getBalloonSafariAttributes().build());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CobbleSafariCommand.registerCommands(event.getDispatcher(), event.getBuildContext());
        SafariExitCommand.registerCommands(event.getDispatcher(), event.getBuildContext());
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CobbleSafari.MOD_ID);

        registrar.playToClient(TimerSyncPayload.TYPE, TimerSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        maxigregrze.cobblesafari.client.hud.TimerHudOverlay.updateDisplay(
                                payload.dimensionId(), payload.remainingTicks(), payload.active())
                ));

        registrar.playToClient(DimensionalBanSyncPayload.TYPE, DimensionalBanSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        maxigregrze.cobblesafari.config.DimensionalBanConfig.applyClientSync(payload.dimensions())
                ));

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            registrar.playToClient(OpenTpAcceptPayload.TYPE, OpenTpAcceptPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleOpenTpAccept);
            registrar.playToClient(CloseTpAcceptPayload.TYPE, CloseTpAcceptPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleCloseTpAccept);
            registrar.playToClient(OpenRuneEditorPayload.TYPE, OpenRuneEditorPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleOpenRuneEditor);
            registrar.playToClient(OpenLostNoteBookPayload.TYPE, OpenLostNoteBookPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleOpenLostNoteBook);
            registrar.playToClient(OpenLostItemConfigPayload.TYPE, OpenLostItemConfigPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleOpenLostItemConfig);
            registrar.playToClient(OpenAuspiciousPokeballConfigPayload.TYPE, OpenAuspiciousPokeballConfigPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleOpenAuspiciousPokeballConfig);
            registrar.playToClient(OpenAuspiciousPokeballGoldConfigPayload.TYPE, OpenAuspiciousPokeballGoldConfigPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleOpenAuspiciousPokeballGoldConfig);
        } else {
            registrar.playToClient(OpenTpAcceptPayload.TYPE, OpenTpAcceptPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(CloseTpAcceptPayload.TYPE, CloseTpAcceptPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(OpenRuneEditorPayload.TYPE, OpenRuneEditorPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(OpenLostNoteBookPayload.TYPE, OpenLostNoteBookPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(OpenLostItemConfigPayload.TYPE, OpenLostItemConfigPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(OpenAuspiciousPokeballConfigPayload.TYPE, OpenAuspiciousPokeballConfigPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(OpenAuspiciousPokeballGoldConfigPayload.TYPE, OpenAuspiciousPokeballGoldConfigPayload.STREAM_CODEC,
                    (payload, context) -> {});
        }

        registrar.playToServer(TpAcceptResponsePayload.TYPE, TpAcceptResponsePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            if ("dungeon".equals(payload.source())) {
                                DungeonTpAcceptHandler.handleAcceptResponse(sp, payload.accepted());
                            } else {
                                TeleporterTickHandler.handleAcceptResponse(sp, payload.accepted());
                            }
                        }
                    });
                });

        registrar.playToServer(SaveRuneTextPayload.TYPE, SaveRuneTextPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer sp)) {
                            return;
                        }
                        if (!sp.isCreative()) {
                            return;
                        }
                        if (!sp.blockPosition().closerThan(payload.pos(), 8.0D)) {
                            return;
                        }
                        if (sp.level().getBlockEntity(payload.pos()) instanceof DistortionStoneBricksRuneBlockEntity runeBlockEntity) {
                            String text = payload.text();
                            if (text.length() > 1024) {
                                text = text.substring(0, 1024);
                            }
                            runeBlockEntity.setRuneText(text);
                            sp.displayClientMessage(Component.translatable("cobblesafari.distortion_rune.saved"), true);
                        }
                    });
                });

        registrar.playToServer(SaveLostItemConfigPayload.TYPE, SaveLostItemConfigPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            LostItemConfigServerHandler.handleSave(sp, payload);
                        }
                    });
                });

        registrar.playToServer(LostItemResetClaimsPayload.TYPE, LostItemResetClaimsPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            LostItemConfigServerHandler.handleReset(sp, payload);
                        }
                    });
                });

        registrar.playToServer(SaveAuspiciousPokeballConfigPayload.TYPE, SaveAuspiciousPokeballConfigPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            AuspiciousPokeballConfigServerHandler.handleSave(sp, payload);
                        }
                    });
                });

        registrar.playToServer(SaveAuspiciousPokeballGoldConfigPayload.TYPE, SaveAuspiciousPokeballGoldConfigPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            AuspiciousPokeballGoldConfigServerHandler.handleSave(sp, payload);
                        }
                    });
                });

        registrar.playToServer(AuspiciousPokeballResetClaimsPayload.TYPE, AuspiciousPokeballResetClaimsPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            AuspiciousPokeballConfigServerHandler.handleReset(sp, payload);
                        }
                    });
                });

        registrar.playToServer(UndergroundPayloads.MineActionPayload.TYPE,
                UndergroundPayloads.MineActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            UndergroundNetworking.handleMineAction(sp, payload);
                        }
                    });
                });

        registrar.playToServer(UndergroundPayloads.SwitchToolPayload.TYPE,
                UndergroundPayloads.SwitchToolPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            UndergroundNetworking.handleToolSwitch(sp, payload);
                        }
                    });
                });

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            registrar.playToClient(maxigregrze.cobblesafari.network.OpenRotomPhonePayload.TYPE,
                    maxigregrze.cobblesafari.network.OpenRotomPhonePayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleOpenRotomPhone);
            registrar.playToClient(maxigregrze.cobblesafari.network.OpenEmptyPhoneConfirmPayload.TYPE,
                    maxigregrze.cobblesafari.network.OpenEmptyPhoneConfirmPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleOpenEmptyPhoneConfirm);
            registrar.playToClient(maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload.TYPE,
                    maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleRotomPhoneConfigSync);
            registrar.playToClient(maxigregrze.cobblesafari.network.UnionAppResultPayload.TYPE,
                    maxigregrze.cobblesafari.network.UnionAppResultPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleUnionAppResult);
            registrar.playToClient(maxigregrze.cobblesafari.network.WonderAppResultPayload.TYPE,
                    maxigregrze.cobblesafari.network.WonderAppResultPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleWonderAppResult);
            registrar.playToClient(maxigregrze.cobblesafari.network.GtsAppResultPayload.TYPE,
                    maxigregrze.cobblesafari.network.GtsAppResultPayload.STREAM_CODEC,
                    CobbleSafariClientNeoForge::handleGtsAppResult);
        } else {
            registrar.playToClient(maxigregrze.cobblesafari.network.OpenRotomPhonePayload.TYPE,
                    maxigregrze.cobblesafari.network.OpenRotomPhonePayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(maxigregrze.cobblesafari.network.OpenEmptyPhoneConfirmPayload.TYPE,
                    maxigregrze.cobblesafari.network.OpenEmptyPhoneConfirmPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload.TYPE,
                    maxigregrze.cobblesafari.network.RotomPhoneConfigSyncPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(maxigregrze.cobblesafari.network.UnionAppResultPayload.TYPE,
                    maxigregrze.cobblesafari.network.UnionAppResultPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(maxigregrze.cobblesafari.network.WonderAppResultPayload.TYPE,
                    maxigregrze.cobblesafari.network.WonderAppResultPayload.STREAM_CODEC,
                    (payload, context) -> {});
            registrar.playToClient(maxigregrze.cobblesafari.network.GtsAppResultPayload.TYPE,
                    maxigregrze.cobblesafari.network.GtsAppResultPayload.STREAM_CODEC,
                    (payload, context) -> {});
        }

        registrar.playToServer(maxigregrze.cobblesafari.network.RotomPhoneActionPayload.TYPE,
                maxigregrze.cobblesafari.network.RotomPhoneActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            maxigregrze.cobblesafari.rotomphone.RotomPhoneServerHandler.handleAction(sp, payload);
                        }
                    });
                });

        registrar.playToServer(maxigregrze.cobblesafari.network.UnionAppPayload.TYPE,
                maxigregrze.cobblesafari.network.UnionAppPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            maxigregrze.cobblesafari.network.UnionAppServerHandler.handle(sp, payload);
                        }
                    });
                });

        registrar.playToServer(maxigregrze.cobblesafari.network.WonderAppPayload.TYPE,
                maxigregrze.cobblesafari.network.WonderAppPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            maxigregrze.cobblesafari.network.WonderAppServerHandler.handle(sp, payload);
                        }
                    });
                });

        registrar.playToServer(maxigregrze.cobblesafari.network.GtsAppPayload.TYPE,
                maxigregrze.cobblesafari.network.GtsAppPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            maxigregrze.cobblesafari.network.GtsAppServerHandler.handle(sp, payload);
                        }
                    });
                });

        registrar.playToServer(maxigregrze.cobblesafari.network.RotomPhoneRotoGlideRequestPayload.TYPE,
                maxigregrze.cobblesafari.network.RotomPhoneRotoGlideRequestPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            maxigregrze.cobblesafari.rotomphone.RotoGlideServerLogic.onRotoGlideRequest(
                                    sp, payload.horizontalMoveX(), payload.horizontalMoveZ());
                        }
                    });
                });

        registrar.playToServer(maxigregrze.cobblesafari.network.EmptyPhoneConfirmPayload.TYPE,
                maxigregrze.cobblesafari.network.EmptyPhoneConfirmPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer sp) {
                            maxigregrze.cobblesafari.rotomphone.EmptyPhoneServerHandler.handleConfirm(sp, payload.confirmed());
                        }
                    });
                });

        registrar.playToClient(UndergroundPayloads.GridUpdatePayload.TYPE,
                UndergroundPayloads.GridUpdatePayload.STREAM_CODEC,
                NeoForgeClientPayloadHandlers::handleGridUpdate);

        registrar.playToClient(UndergroundPayloads.StabilityUpdatePayload.TYPE,
                UndergroundPayloads.StabilityUpdatePayload.STREAM_CODEC,
                NeoForgeClientPayloadHandlers::handleStabilityUpdate);

        registrar.playToClient(UndergroundPayloads.TreasureRevealedPayload.TYPE,
                UndergroundPayloads.TreasureRevealedPayload.STREAM_CODEC,
                NeoForgeClientPayloadHandlers::handleTreasureRevealed);

        registrar.playToClient(UndergroundPayloads.GameEndPayload.TYPE,
                UndergroundPayloads.GameEndPayload.STREAM_CODEC,
                NeoForgeClientPayloadHandlers::handleGameEnd);

        registrar.playToClient(UndergroundPayloads.PlaySoundPayload.TYPE,
                UndergroundPayloads.PlaySoundPayload.STREAM_CODEC,
                NeoForgeClientPayloadHandlers::handlePlaySound);

        registrar.playToClient(UndergroundPayloads.TreasureRegistrySyncPayload.TYPE,
                UndergroundPayloads.TreasureRegistrySyncPayload.STREAM_CODEC,
                NeoForgeClientPayloadHandlers::handleTreasureRegistrySync);
    }

    private void onServerStarted(ServerStartedEvent event) {
        DimensionEvents.onServerStarted(event.getServer());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        DimensionEvents.onServerStopping(event.getServer());
    }

    private void onServerTick(ServerTickEvent.Post event) {
        DimensionEvents.onServerTick(event.getServer());
        TeleporterTickHandler.onServerTick(event.getServer());
        DungeonTeleportCountdown.onServerTick(event.getServer());
        BalloonSpawnHandler.onServerTick(event.getServer());
        event.getServer().getPlayerList().getPlayers().forEach(LuckyMiningHelmetItem::tickEffect);
        maxigregrze.cobblesafari.rotomphone.RotoGlideServerLogic.tickAll(event.getServer());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            DimensionEvents.onPlayerJoin(sp);
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            DimensionEvents.onPlayerDisconnect(sp);
        }
    }

    private void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        var result = DimensionalBanEventHandler.onUseItem(event.getEntity(), event.getLevel(), event.getHand());
        if (result.getResult() != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result.getResult());
        }
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var cauldronResult = maxigregrze.cobblesafari.safari.MudBallCauldronHandler.onUseBlock(
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (cauldronResult != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(cauldronResult);
            return;
        }
        
        var result = DimensionalBanEventHandler.onUseBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    private void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        var result = DimensionalBanEventHandler.onAttackBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getPos(), event.getFace());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
        }
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level) {
            if (!DimensionalBanEventHandler.onBlockBreakTry(level.dimension(), event.getPlayer(), event.getPos())) {
                event.setCanceled(true);
            }
        }
    }

    private void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!DimensionalBanEventHandler.onLivingHurt(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (!DimensionTimerDeathHandler.allowVanillaDeath(sp)) {
            event.setCanceled(true);
        }
    }
}
