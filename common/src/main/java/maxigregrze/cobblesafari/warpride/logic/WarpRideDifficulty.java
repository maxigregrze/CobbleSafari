package maxigregrze.cobblesafari.warpride.logic;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** The distance-banded difficulty table. Bands are step functions, applying until the next one. */
public final class WarpRideDifficulty {

    /** Hard geometric ceiling: see {@link WaveGenerator} for why this is not a tuning value. */
    public static final int MAX_NEGATIVES_PER_WAVE = 3;

    public static final int RANK_COUNT = 4;

    public static final class Band {
        public final int fromDistance;
        /** Indexed by rank 0..3; must sum to 1.0. */
        public final double[] rankWeights;
        public final double portalChance;
        public final double bonusChance;
        /** Indexed by negative count 0..3; must sum to 1.0. */
        public final double[] negativeWeights;

        public Band(int fromDistance, double[] rankWeights, double portalChance,
                    double bonusChance, double[] negativeWeights) {
            this.fromDistance = fromDistance;
            this.rankWeights = rankWeights;
            this.portalChance = portalChance;
            this.bonusChance = bonusChance;
            this.negativeWeights = negativeWeights;
        }

        public int rollRank(RandomSource random) {
            return rollIndex(random, rankWeights);
        }

        public int rollNegativeCount(RandomSource random) {
            return rollIndex(random, negativeWeights);
        }

        private static int rollIndex(RandomSource random, double[] weights) {
            double total = 0.0;
            for (double w : weights) {
                total += w;
            }
            if (total <= 0.0) {
                return 0;
            }
            double roll = random.nextDouble() * total;
            double acc = 0.0;
            for (int i = 0; i < weights.length; i++) {
                acc += weights[i];
                if (roll < acc) {
                    return i;
                }
            }
            return weights.length - 1;
        }
    }

    private final int noBonusDistance;
    private final List<Band> bands;
    private final WarpRideRewards rewards;

    public WarpRideDifficulty(int noBonusDistance, List<Band> bands, WarpRideRewards rewards) {
        this.noBonusDistance = noBonusDistance;
        this.bands = List.copyOf(bands);
        this.rewards = rewards == null ? WarpRideRewards.defaults() : rewards;
    }

    public int noBonusDistance() {
        return noBonusDistance;
    }

    public List<Band> bands() {
        return bands;
    }

    /**
     * What a ride pays out (action plan 184). Never null: a file with no {@code rewards} block -
     * which is every file written before that plan - falls back to the shipped scale field by
     * field, because a wrong reward scale cannot break a ride the way a wrong band can.
     */
    public WarpRideRewards rewards() {
        return rewards;
    }

    /** The band in force at {@code distance}; never null as long as a band starts at 0. */
    public Band bandFor(double distance) {
        Band chosen = bands.get(0);
        for (Band b : bands) {
            if (distance >= b.fromDistance) {
                chosen = b;
            } else {
                break;
            }
        }
        return chosen;
    }

    /** The shipped table, used verbatim when the datapack file is missing or rejected. */
    public static WarpRideDifficulty defaults() {
        List<Band> b = new ArrayList<>();
        b.add(new Band(0, new double[]{1.00, 0.00, 0.00, 0.00}, 0.06, 0.90,
                new double[]{0.30, 0.55, 0.15, 0.00}));
        b.add(new Band(1000, new double[]{0.78, 0.20, 0.02, 0.00}, 0.07, 0.80,
                new double[]{0.20, 0.55, 0.25, 0.00}));
        b.add(new Band(1500, new double[]{0.66, 0.30, 0.04, 0.00}, 0.07, 0.70,
                new double[]{0.15, 0.50, 0.35, 0.00}));
        b.add(new Band(2000, new double[]{0.52, 0.40, 0.08, 0.00}, 0.08, 0.55,
                new double[]{0.10, 0.45, 0.40, 0.05}));
        b.add(new Band(2500, new double[]{0.33, 0.50, 0.16, 0.01}, 0.08, 0.40,
                new double[]{0.05, 0.40, 0.45, 0.10}));
        b.add(new Band(3000, new double[]{0.32, 0.50, 0.16, 0.02}, 0.09, 0.30,
                new double[]{0.05, 0.35, 0.45, 0.15}));
        b.add(new Band(3500, new double[]{0.31, 0.50, 0.16, 0.03}, 0.09, 0.15,
                new double[]{0.00, 0.30, 0.50, 0.20}));
        b.add(new Band(4000, new double[]{0.30, 0.50, 0.16, 0.04}, 0.10, 0.00,
                new double[]{0.00, 0.20, 0.50, 0.30}));
        b.add(new Band(4500, new double[]{0.29, 0.50, 0.16, 0.05}, 0.10, 0.00,
                new double[]{0.00, 0.10, 0.45, 0.45}));
        return new WarpRideDifficulty(4000, b, WarpRideRewards.defaults());
    }
}
