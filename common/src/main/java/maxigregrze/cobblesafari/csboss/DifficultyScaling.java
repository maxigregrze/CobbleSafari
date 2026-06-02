package maxigregrze.cobblesafari.csboss;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.config.CsBossSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.List;

/**
 * Calcule la durée de survie (plan 100 § 9) à partir des niveaux d'équipe des participants.
 * Score 0 → maximumDuration ; 100 → minimumDuration ; interpolation linéaire ; clamp config.
 */
public final class DifficultyScaling {

    private static final int PARTY_SIZE = 6;
    private static final int MAX_LEVEL = 100;

    private DifficultyScaling() {}

    public static int computeDuration(CsBossDefinition def, List<ServerPlayer> participants) {
        double teamScore = participants.isEmpty() ? 0.0 : medianN(participants);

        double t = Mth.clamp(teamScore / 100.0, 0.0, 1.0);
        int ticks = (int) Math.round(def.maximumDuration() + (def.minimumDuration() - def.maximumDuration()) * t);

        CsBossSettings cfg = CsBossSettings.get();
        int min = cfg.getMinimumFightDuration() * 20;
        int max = cfg.getMaximumFightDuration() * 20;
        return Mth.clamp(ticks, min, max);
    }

    private static double medianN(List<ServerPlayer> participants) {
        double[] scores = new double[participants.size()];
        for (int i = 0; i < participants.size(); i++) {
            scores[i] = partyMedian(participants.get(i));
        }
        Arrays.sort(scores);
        return median(scores);
    }

    private static double partyMedian(ServerPlayer player) {
        double[] levels = new double[PARTY_SIZE];
        try {
            PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            for (int slot = 0; slot < PARTY_SIZE; slot++) {
                Pokemon p = party.get(slot);
                levels[slot] = p == null ? 0 : Mth.clamp(p.getLevel(), 0, MAX_LEVEL);
            }
        } catch (Exception e) {
            // En cas d'indisponibilité de la party, on retombe sur 0 (durée la plus longue).
            Arrays.fill(levels, 0);
        }
        Arrays.sort(levels);
        return median(levels);
    }

    /** Médiane d'un tableau déjà trié. */
    private static double median(double[] sorted) {
        int n = sorted.length;
        if (n == 0) {
            return 0;
        }
        if (n % 2 == 1) {
            return sorted[n / 2];
        }
        return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
    }
}
