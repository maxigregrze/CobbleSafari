package maxigregrze.cobblesafari.warpride.logic;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.UltraWarpRideConfig;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Rolls the contents of each wave. Three negatives is a <b>geometric</b> limit, not a tuning value:
 * one input reaches {@code L-1}, {@code L+1} or {@code L^4} and standing still keeps {@code L}, so a
 * fourth negative could block all four reachable lanes at once. Clamped here whatever the datapack
 * asks for, and the clamp is logged the first time it bites.
 */
public final class WaveGenerator {

    public static final int LANE_COUNT = 8;

    private final RandomSource random;
    private final UltraWarpRideConfig cfg;
    private boolean clampWarned;

    public WaveGenerator(RandomSource random, UltraWarpRideConfig cfg) {
        this.random = random;
        this.cfg = cfg;
    }

    /**
     * @param wavesSinceLastPortal waves since the last portal, {@link Integer#MAX_VALUE} if never
     * @param forcePortal          the drift guarantee, which overrides the spacing minimum
     */
    public Wave generate(int index, double distance, int wavesSinceLastPortal, boolean forcePortal) {
        WarpRideDifficulty.Band band = WarpRideDifficultyLoader.get().bandFor(distance);
        List<WaveObject> objects = new ArrayList<>(4);

        boolean spacingAllows = wavesSinceLastPortal >= cfg.minPortalSpacingWaves;
        boolean placePortal = forcePortal || (spacingAllows && random.nextDouble() < band.portalChance);

        if (placePortal) {
            int rank = band.rollRank(random);
            if (forcePortal) {
                rank = Math.min(rank, cfg.forcedPortalMaxRank);
            }
            int typeId = WarpRideDestinationRegistry.rollTypeId(random);
            if (typeId >= 1) {
                objects.add(WaveObject.portal(rank, typeId));
            } else {
                // A portal with no world behind it would end the ride on nothing; place none instead.
                placePortal = false;
            }
        }

        boolean[] laneTaken = new boolean[LANE_COUNT];

        if (random.nextDouble() < band.bonusChance) {
            int lane = random.nextInt(LANE_COUNT);
            laneTaken[lane] = true;
            objects.add(WaveObject.positive(lane));
        }

        int requested = band.rollNegativeCount(random);
        int negatives = Math.min(requested, WarpRideDifficulty.MAX_NEGATIVES_PER_WAVE);
        if (requested > negatives && !clampWarned) {
            clampWarned = true;
            CobbleSafari.LOGGER.warn("[WarpRide] The difficulty table asked for {} negatives on one "
                    + "wave; clamped to {}. Above that, a wave can block every lane reachable in one "
                    + "input and become unavoidable (plan 180 § 3.5)",
                    requested, WarpRideDifficulty.MAX_NEGATIVES_PER_WAVE);
        }

        int placed = 0;
        int guard = 0;
        while (placed < negatives && guard < 64) {
            guard++;
            int lane = random.nextInt(LANE_COUNT);
            if (laneTaken[lane]) {
                continue;
            }
            laneTaken[lane] = true;
            objects.add(WaveObject.negative(lane));
            placed++;
        }

        return new Wave(index, index * cfg.waveSpacingLy, objects);
    }
}
