package maxigregrze.cobblesafari;

import maxigregrze.cobblesafari.underground.UndergroundScreen;
import maxigregrze.cobblesafari.underground.logic.TreasureRegistry;
import maxigregrze.cobblesafari.underground.network.UndergroundPayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NeoForgeClientPayloadHandlers {

    private NeoForgeClientPayloadHandlers() {
        // Utility class; not meant to be instantiated.
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

    public static void handleTreasureRegistrySync(UndergroundPayloads.TreasureRegistrySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            TreasureRegistry.applyClientSync(payload.entries());
        });
    }


    public static void handleWarpRideOpen(
            maxigregrze.cobblesafari.warpride.network.WarpRidePayloads.OpenPayload payload,
            IPayloadContext context) {
        context.enqueueWork(() ->
                maxigregrze.cobblesafari.client.screen.warpride.UltraWarpRideScreen.open(payload));
    }

    public static void handleWarpRideDestinationSync(
            maxigregrze.cobblesafari.warpride.network.WarpRidePayloads.DestinationSyncPayload payload,
            IPayloadContext context) {
        context.enqueueWork(() ->
                maxigregrze.cobblesafari.client.screen.warpride.UltraWarpRideScreen
                        .acceptTints(payload.tints()));
    }

    public static void handleWarpRideWaves(
            maxigregrze.cobblesafari.warpride.network.WarpRidePayloads.WavesPayload payload,
            IPayloadContext context) {
        context.enqueueWork(() -> {
            var screen = maxigregrze.cobblesafari.client.screen.warpride.UltraWarpRideScreen.active();
            if (screen != null) {
                screen.acceptWaves(payload);
            }
        });
    }

    public static void handleWarpRideState(
            maxigregrze.cobblesafari.warpride.network.WarpRidePayloads.StatePayload payload,
            IPayloadContext context) {
        context.enqueueWork(() -> {
            var screen = maxigregrze.cobblesafari.client.screen.warpride.UltraWarpRideScreen.active();
            if (screen != null) {
                screen.acceptState(payload);
            }
        });
    }

    public static void handleWarpRideEvent(
            maxigregrze.cobblesafari.warpride.network.WarpRidePayloads.EventPayload payload,
            IPayloadContext context) {
        context.enqueueWork(() -> {
            var screen = maxigregrze.cobblesafari.client.screen.warpride.UltraWarpRideScreen.active();
            if (screen != null) {
                screen.acceptEvent(payload);
            }
        });
    }
}
