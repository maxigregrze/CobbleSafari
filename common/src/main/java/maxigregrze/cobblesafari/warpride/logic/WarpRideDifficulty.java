package maxigregrze.cobblesafari.warpride.logic;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** The distance-banded difficulty table. Bands are step functions, applying until the next one. */
public final class WarpRideDifficulty {

    /** Hard geometric ceiling: see {@link WaveGenerator} for why this is not a tuning value. */
    public static final int MAX_NEGATIVES_PER_WAVE = 3;

    public static final int RANK_COUNT = 4;

    /**
     * One distance band. {@code bonusChance} and {@code negativeWeights} hold at the band's start;
     * their {@code ...To} twins hold at the next band's start, linearly in between (plan 191,
     * section 4.4). A band without {@code ...To} values is a plain step, as before.
     */
    public static final class Band {
        public final int fromDistance;
        /** Indexed by rank 0..3; must sum to 1.0. */
        public final double[] rankWeights;
        public final double portalChance;
        public final double bonusChance;
        public final double bonusChanceTo;
        /** Indexed by negative count 0..3; must sum to 1.0. */
        public final double[] negativeWeights;
        public final double[] negativeWeightsTo;

        public Band(int fromDistance, double[] rankWeights, double portalChance,
                    double bonusChance, double[] negativeWeights) {
            this(fromDistance, rankWeights, portalChance, bonusChance, bonusChance,
                    negativeWeights, negativeWeights);
        }

        @SuppressWarnings("java:S107")   // one immutable row of the table
        public Band(int fromDistance, double[] rankWeights, double portalChance,
                    double bonusChance, double bonusChanceTo,
                    double[] negativeWeights, double[] negativeWeightsTo) {
            this.fromDistance = fromDistance;
            this.rankWeights = rankWeights;
            this.portalChance = portalChance;
            this.bonusChance = bonusChance;
            this.bonusChanceTo = bonusChanceTo;
            this.negativeWeights = negativeWeights;
            this.negativeWeightsTo = negativeWeightsTo;
        }

        public int rollRank(RandomSource random) {
            return rollIndex(random, rankWeights);
        }

        /** @param t 0 at this band's start, 1 at the next band's */
        public double bonusChanceAt(double t) {
            return bonusChance + (bonusChanceTo - bonusChance) * t;
        }

        /** @param t 0 at this band's start, 1 at the next band's */
        public int rollNegativeCount(RandomSource random, double t) {
            if (negativeWeightsTo == negativeWeights || t <= 0.0) {
                return rollIndex(random, negativeWeights);
            }
            double[] mixed = new double[negativeWeights.length];
            for (int i = 0; i < mixed.length; i++) {
                mixed[i] = negativeWeights[i] + (negativeWeightsTo[i] - negativeWeights[i]) * t;
            }
            return rollIndex(random, mixed);
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
        return bands.get(indexFor(distance));
    }

    private int indexFor(double distance) {
        int chosen = 0;
        for (int i = 0; i < bands.size(); i++) {
            if (distance >= bands.get(i).fromDistance) {
                chosen = i;
            } else {
                break;
            }
        }
        return chosen;
    }

    /** 0 at the start of the band in force, 1 at the next band's start; 0 on the last band. */
    private double progressIn(int index, double distance) {
        if (index + 1 >= bands.size()) {
            return 0.0;
        }
        double from = bands.get(index).fromDistance;
        double to = bands.get(index + 1).fromDistance;
        return Math.max(0.0, Math.min(1.0, (distance - from) / Math.max(1.0, to - from)));
    }

    /** The bonus chance at {@code distance}, interpolated inside its band. */
    public double bonusChanceAt(double distance) {
        int index = indexFor(distance);
        return bands.get(index).bonusChanceAt(progressIn(index, distance));
    }

    /** A negative count rolled from the weights at {@code distance}, interpolated inside its band. */
    public int rollNegativeCount(RandomSource random, double distance) {
        int index = indexFor(distance);
        return bands.get(index).rollNegativeCount(random, progressIn(index, distance));
    }

    /**
     * The shipped table, used verbatim when the datapack file is missing or rejected. Converted from
     * the source minigame's spawn table at 1 ly = 100 units (plan 191, section 4.2): bonuses fade
     * out, negatives thicken, a two-negative "wall" every 500 ly from 2000, no bonus past 5100.
     */
    public static WarpRideDifficulty defaults() {
        double[] r0 = {1.00, 0.00, 0.00, 0.00};
        double[] r1000 = {0.78, 0.20, 0.02, 0.00};
        double[] r1500 = {0.66, 0.30, 0.04, 0.00};
        double[] r2000 = {0.52, 0.40, 0.08, 0.00};
        double[] r2500 = {0.33, 0.50, 0.16, 0.01};
        double[] r3000 = {0.32, 0.50, 0.16, 0.02};
        double[] r3500 = {0.31, 0.50, 0.16, 0.03};
        double[] r4000 = {0.30, 0.50, 0.16, 0.04};
        double[] r4500 = {0.29, 0.50, 0.16, 0.05};
        double[] wall = {0.00, 0.00, 1.00, 0.00};

        List<Band> b = new ArrayList<>();
        b.add(new Band(0, r0, 0.06, 0.80, single(0.00)));
        b.add(new Band(300, r0, 0.06, 0.80, 0.40, single(0.20), single(0.20)));
        b.add(new Band(600, r0, 0.06, 0.40, 0.30, single(0.25), single(0.30)));
        b.add(new Band(1000, r1000, 0.07, 0.30, single(0.35)));
        b.add(new Band(1500, r1500, 0.07, 0.25, single(0.40)));
        b.add(new Band(2000, r2000, 0.08, 0.20, wall));
        b.add(new Band(2100, r2000, 0.08, 0.20, single(0.45)));
        b.add(new Band(2500, r2500, 0.08, 0.20, wall));
        b.add(new Band(2600, r2500, 0.08, 0.15, single(0.50)));
        b.add(new Band(3000, r3000, 0.09, 0.12, wall));
        b.add(new Band(3100, r3000, 0.09, 0.12, single(0.55)));
        b.add(new Band(3500, r3500, 0.09, 0.10, wall));
        b.add(new Band(3600, r3500, 0.09, 0.10, single(0.60)));
        b.add(new Band(4000, r4000, 0.10, 0.08, wall));
        b.add(new Band(4100, r4000, 0.10, 0.08, single(0.65)));
        b.add(new Band(4500, r4500, 0.10, 0.05, wall));
        b.add(new Band(4600, r4500, 0.10, 0.05, single(0.70)));
        b.add(new Band(5000, r4500, 0.10, 0.05, wall));
        b.add(new Band(5100, r4500, 0.10, 0.00, single(0.75)));
        return new WarpRideDifficulty(5100, b, WarpRideRewards.defaults());
    }

    /** One attempt at probability {@code p}: no negative, or exactly one. */
    private static double[] single(double p) {
        return new double[]{1.0 - p, p, 0.0, 0.0};
    }
}
