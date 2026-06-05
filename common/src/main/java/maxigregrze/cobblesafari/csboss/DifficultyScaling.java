package maxigregrze.cobblesafari.csboss;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import maxigregrze.cobblesafari.CobbleSafari;
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
        // Score par joueur (médiane des niveaux de la party) + journalisation.
        double[] scores = new double[participants.size()];
        CobbleSafari.LOGGER.info("[CSBoss] '{}' — calcul de durée pour {} participant(s) :",
                def.bossId(), participants.size());
        for (int i = 0; i < participants.size(); i++) {
            ServerPlayer p = participants.get(i);
            scores[i] = partyMedian(p);
            CobbleSafari.LOGGER.info("[CSBoss]   joueur {} : score = {}",
                    p.getGameProfile().getName(), String.format("%.1f", scores[i]));
        }
        Arrays.sort(scores);
        double teamScore = participants.isEmpty() ? 0.0 : median(scores);

        double t = Mth.clamp(teamScore / 100.0, 0.0, 1.0);
        // maximumDuration / minimumDuration sont en SECONDES (cohérent avec la config) → ×20 en ticks.
        double rawSeconds = def.maximumDuration() + (def.minimumDuration() - def.maximumDuration()) * t;
        int rawTicks = (int) Math.round(rawSeconds * 20.0);

        CsBossSettings cfg = CsBossSettings.get();
        int maxTicks = cfg.getMaximumFightDuration() * 20; // plafond dur global ; pas de plancher config
        int clamped = Mth.clamp(rawTicks, 1, maxTicks);

        CobbleSafari.LOGGER.info(
                "[CSBoss]   score du lobby (médiane) = {} → durée {}s ({} ticks ; brut {}s, plafond {}s)",
                String.format("%.1f", teamScore), clamped / 20, clamped,
                String.format("%.0f", rawSeconds), cfg.getMaximumFightDuration());
        return clamped;
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
