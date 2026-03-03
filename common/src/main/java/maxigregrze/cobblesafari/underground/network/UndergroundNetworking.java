package maxigregrze.cobblesafari.underground.network;

import maxigregrze.cobblesafari.platform.Services;
import maxigregrze.cobblesafari.underground.MiningSession;
import maxigregrze.cobblesafari.underground.UndergroundMinigame;
import maxigregrze.cobblesafari.underground.logic.MiningGrid;
import maxigregrze.cobblesafari.underground.logic.PlacedTreasure;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class UndergroundNetworking {

    public static void handleMineAction(ServerPlayer player, UndergroundPayloads.MineActionPayload payload) {
        MiningSession session = UndergroundMinigame.getSession(payload.sessionId());
        if (session == null || session.isComplete()) {
            return;
        }

        MiningGrid.MiningResult result = session.mine(payload.cellX(), payload.cellY());

        List<UndergroundPayloads.CellUpdateData> updateData = new ArrayList<>();
        for (MiningGrid.CellUpdate update : result.updates()) {
            var cell = session.getGrid().getCell(update.x(), update.y());
            if (cell != null) {
                updateData.add(new UndergroundPayloads.CellUpdateData(
                        update.x(), update.y(),
                        update.newTier(),
                        update.revealed(),
                        cell.getSecondLayerContent().ordinal(),
                        cell.getTreasureId()
                ));
            }
        }

        if (!updateData.isEmpty()) {
            Services.PLATFORM.sendPayloadToPlayer(player, new UndergroundPayloads.GridUpdatePayload(
                    payload.sessionId(), updateData
            ));
        }

        Services.PLATFORM.sendPayloadToPlayer(player, new UndergroundPayloads.StabilityUpdatePayload(
                payload.sessionId(),
                session.getGrid().getStability().getCurrentStability(),
                session.getGrid().getStability().getMaxStability()
        ));

        String soundType = result.hitIron() ? "iron_hit" : "stone_hit";
        Services.PLATFORM.sendPayloadToPlayer(player, new UndergroundPayloads.PlaySoundPayload(
                payload.sessionId(), soundType
        ));

        for (PlacedTreasure treasure : result.newlyRevealedTreasures()) {
            Services.PLATFORM.sendPayloadToPlayer(player, new UndergroundPayloads.TreasureRevealedPayload(
                    payload.sessionId(),
                    treasure.getId(),
                    treasure.getStartX(),
                    treasure.getStartY()
            ));

            Services.PLATFORM.sendPayloadToPlayer(player, new UndergroundPayloads.PlaySoundPayload(
                    payload.sessionId(), "treasure_found"
            ));
        }

        if (result.collapsed()) {
            session.setComplete(true);

            Services.PLATFORM.sendPayloadToPlayer(player, new UndergroundPayloads.GameEndPayload(
                    payload.sessionId(),
                    true,
                    session.getGrid().getRevealedTreasures().size(),
                    session.getGrid().getTreasureCount()
            ));

            Services.PLATFORM.sendPayloadToPlayer(player, new UndergroundPayloads.PlaySoundPayload(
                    payload.sessionId(), "wall_collapse"
            ));
        } else {
            List<PlacedTreasure> collected = session.getGrid().getRevealedTreasures();
            if (collected.size() == session.getGrid().getTreasureCount()) {
                session.setComplete(true);

                Services.PLATFORM.sendPayloadToPlayer(player, new UndergroundPayloads.GameEndPayload(
                        payload.sessionId(),
                        false,
                        collected.size(),
                        session.getGrid().getTreasureCount()
                ));

                Services.PLATFORM.sendPayloadToPlayer(player, new UndergroundPayloads.PlaySoundPayload(
                        payload.sessionId(), "perfect_clear"
                ));
            }
        }
    }

    public static void handleToolSwitch(ServerPlayer player, UndergroundPayloads.SwitchToolPayload payload) {
        MiningSession session = UndergroundMinigame.getSession(payload.sessionId());
        if (session == null || session.isComplete()) {
            return;
        }

        session.setUsingHammer(payload.toHammer());
    }
}
