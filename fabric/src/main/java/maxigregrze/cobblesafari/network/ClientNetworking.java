package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.client.screen.AuspiciousPokeballConfigScreen;
import maxigregrze.cobblesafari.client.screen.AuspiciousPokeballGoldConfigScreen;
import maxigregrze.cobblesafari.client.screen.LostItemConfigScreen;
import maxigregrze.cobblesafari.client.screen.TpAcceptScreen;
import maxigregrze.cobblesafari.client.screen.DistortionStoneBricksRuneScreen;
import maxigregrze.cobblesafari.underground.UndergroundScreen;
import maxigregrze.cobblesafari.underground.logic.TreasureRegistry;
import maxigregrze.cobblesafari.underground.network.UndergroundPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;

public class ClientNetworking {

    private ClientNetworking() {
        // Utility class; not meant to be instantiated.
    }

    public static void registerFabricClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(
                OpenTpAcceptPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
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
        );

        ClientPlayNetworking.registerGlobalReceiver(
                CloseTpAcceptPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (Minecraft.getInstance().screen instanceof TpAcceptScreen tpScreen) {
                            tpScreen.closeFromServer();
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenRuneEditorPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        Minecraft.getInstance().setScreen(new DistortionStoneBricksRuneScreen(payload.pos(), payload.text()));
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenLostItemConfigPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        Minecraft.getInstance().setScreen(new LostItemConfigScreen(payload));
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenAuspiciousPokeballGoldConfigPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        Minecraft.getInstance().setScreen(new AuspiciousPokeballGoldConfigScreen(payload));
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenAuspiciousPokeballConfigPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        Minecraft.getInstance().setScreen(new AuspiciousPokeballConfigScreen(payload));
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.OpenCsBossTriggerConfigPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() ->
                            Minecraft.getInstance().setScreen(
                                    new maxigregrze.cobblesafari.client.screen.CsBossTriggerConfigScreen(payload)));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.OpenCsBossMimicConfigPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() ->
                            Minecraft.getInstance().setScreen(
                                    new maxigregrze.cobblesafari.client.screen.CsBossMimicConfigScreen(payload)));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.OpenTeleportPadConfigPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        Minecraft.getInstance().setScreen(
                                new maxigregrze.cobblesafari.client.screen.TeleportPadConfigScreen(payload)))
        );

        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.TeleportPadResultPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (Minecraft.getInstance().screen
                            instanceof maxigregrze.cobblesafari.client.screen.TeleportPadConfigScreen screen) {
                        screen.applyResult(payload);
                    }
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenLostNoteBookPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        BookViewScreen.BookAccess access = BookViewScreen.BookAccess.fromItem(payload.book());
                        Minecraft.getInstance().setScreen(new BookViewScreen(access));
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                UndergroundPayloads.GridUpdatePayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (Minecraft.getInstance().screen instanceof UndergroundScreen screen) {
                            if (screen.getSessionId().equals(payload.sessionId())) {
                                screen.onGridUpdate(payload);
                            }
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                UndergroundPayloads.StabilityUpdatePayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (Minecraft.getInstance().screen instanceof UndergroundScreen screen) {
                            if (screen.getSessionId().equals(payload.sessionId())) {
                                screen.onStabilityUpdate(payload);
                            }
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                UndergroundPayloads.TreasureRevealedPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (Minecraft.getInstance().screen instanceof UndergroundScreen screen) {
                            if (screen.getSessionId().equals(payload.sessionId())) {
                                screen.onTreasureRevealed(payload);
                            }
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                UndergroundPayloads.GameEndPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (Minecraft.getInstance().screen instanceof UndergroundScreen screen) {
                            if (screen.getSessionId().equals(payload.sessionId())) {
                                screen.onGameEnd(payload);
                            }
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                UndergroundPayloads.PlaySoundPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (Minecraft.getInstance().screen instanceof UndergroundScreen screen) {
                            if (screen.getSessionId().equals(payload.sessionId())) {
                                screen.playSound(payload.soundType());
                            }
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                UndergroundPayloads.TreasureRegistrySyncPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        TreasureRegistry.applyClientSync(payload.entries());
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenRotomPhonePayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        Minecraft.getInstance().setScreen(
                                new maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneMenuScreen(
                                        payload.rotomName(), payload.shinyStatus(),
                                        payload.currentSkin(), payload.safetyMode(), payload.rotoGlide()));
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                OpenEmptyPhoneConfirmPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        Minecraft.getInstance().setScreen(
                                new maxigregrze.cobblesafari.client.screen.rotomphone.EmptyPhoneConfirmScreen(
                                        payload.rotomName(), payload.rotomLevel(), payload.rotomIsShiny()));
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                RotomPhoneConfigSyncPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        maxigregrze.cobblesafari.rotomphone.RotomPhoneClientCache.applySyncData(payload);
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.UnionAppResultPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (payload.subscreen() == maxigregrze.cobblesafari.network.UnionAppResultPayload.SUB_CLOSE_GUI) {
                            net.minecraft.client.Minecraft.getInstance().setScreen(null);
                            return;
                        }
                        if (net.minecraft.client.Minecraft.getInstance().screen
                                instanceof maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneUnionScreen ru) {
                            ru.applyServerSnapshot(payload);
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.WonderAppResultPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen
                                instanceof maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneWonderScreen rw) {
                            rw.applyServerSnapshot(payload);
                        }
                    });
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.GtsAppResultPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen
                                instanceof maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneGTSScreen rg) {
                            rg.applyServerSnapshot(payload);
                        }
                    });
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.ChatConversationSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        maxigregrze.cobblesafari.rotomphone.ChatConversationClientCache.setConversations(payload.conversations()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                maxigregrze.cobblesafari.network.ChatAppResultPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (net.minecraft.client.Minecraft.getInstance().screen
                                instanceof maxigregrze.cobblesafari.client.screen.rotomphone.RotomPhoneChatScreen rc) {
                            rc.applyServerSnapshot(payload);
                        }
                    });
                }
        );
    }
}
