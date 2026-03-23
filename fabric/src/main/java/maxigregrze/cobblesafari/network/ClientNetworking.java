package maxigregrze.cobblesafari.network;

import maxigregrze.cobblesafari.client.screen.TpAcceptScreen;
import maxigregrze.cobblesafari.client.screen.DistortionStoneBricksRuneScreen;
import maxigregrze.cobblesafari.underground.UndergroundScreen;
import maxigregrze.cobblesafari.underground.logic.TreasureRegistry;
import maxigregrze.cobblesafari.underground.network.UndergroundPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;

public class ClientNetworking {

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
    }
}
