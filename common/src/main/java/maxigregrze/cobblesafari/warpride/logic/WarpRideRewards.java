package maxigregrze.cobblesafari.warpride.logic;

import net.minecraft.util.Mth;

/**
 * What a completed Ultra Warp Dive pays (action plan 184, C1).
 *
 * <p>This class knows nothing about a Pokémon. It answers one question - <i>for this rank and this
 * distance, what value or what percentage?</i> - and {@code SubDungeonEncounter} does the composing.
 * The split is deliberate: a server owner who wants a richer Ultra Space edits one data file, and a
 * reader who wants to know what is put on the Pokémon reads one method.</p>
 *
 * <p>Every probabilistic reward has the shape {@code base[rank] + perStep[rank] * p}, where {@code p}
 * is the {@linkplain #performanceFactor(int) performance factor}. That is the shape the shiny
 * formula was specified in - {@code 1 + 2p} at rank 2 reaching 19% at {@code p = 9} - and the other
 * scales are calibrated so their stated figure is the value at maximum performance. {@code base} and
 * {@code perStep} are separate arrays precisely so that flattening a scale (all of the reward, none
 * of the distance) stays a data edit rather than a recompile.</p>
 *
 * <p>Level offset and perfect-IV count are <b>not</b> scaled: they are values, not odds.</p>
 */
public final class WarpRideRewards {

    /** Perfect-IV count is bounded by the number of permanent stats. */
    public static final int MAX_PERFECT_IVS = 6;

    private final int stepLy;
    private final int maxSteps;

    private final int[] levelOffset;
    private final int[] ivPerfectCount;

    private final double[] shinyPercentBase;
    private final double[] shinyPercentPerStep;
    private final double[] hiddenAbilityPercentBase;
    private final double[] hiddenAbilityPercentPerStep;
    private final double[] bonusMovePercentBase;
    private final double[] bonusMovePercentPerStep;
    private final double[] secondMovePercentBase;
    private final double[] secondMovePercentPerStep;
    private final double[] alphaPercentBase;
    private final double[] alphaPercentPerStep;

    @SuppressWarnings("java:S107")   // one immutable table; a builder here would only add indirection
    public WarpRideRewards(int stepLy, int maxSteps,
                           int[] levelOffset, int[] ivPerfectCount,
                           double[] shinyPercentBase, double[] shinyPercentPerStep,
                           double[] hiddenAbilityPercentBase, double[] hiddenAbilityPercentPerStep,
                           double[] bonusMovePercentBase, double[] bonusMovePercentPerStep,
                           double[] secondMovePercentBase, double[] secondMovePercentPerStep,
                           double[] alphaPercentBase, double[] alphaPercentPerStep) {
        this.stepLy = Math.max(1, stepLy);
        this.maxSteps = Math.max(0, maxSteps);
        this.levelOffset = levelOffset.clone();
        this.ivPerfectCount = ivPerfectCount.clone();
        this.shinyPercentBase = shinyPercentBase.clone();
        this.shinyPercentPerStep = shinyPercentPerStep.clone();
        this.hiddenAbilityPercentBase = hiddenAbilityPercentBase.clone();
        this.hiddenAbilityPercentPerStep = hiddenAbilityPercentPerStep.clone();
        this.bonusMovePercentBase = bonusMovePercentBase.clone();
        this.bonusMovePercentPerStep = bonusMovePercentPerStep.clone();
        this.secondMovePercentBase = secondMovePercentBase.clone();
        this.secondMovePercentPerStep = secondMovePercentPerStep.clone();
        this.alphaPercentBase = alphaPercentBase.clone();
        this.alphaPercentPerStep = alphaPercentPerStep.clone();
    }

    /**
     * The performance factor {@code p}: the ride's distance expressed as "how many
     * {@code performanceStepLy} steps past the first one", capped at {@code performanceMaxSteps}.
     *
     * <p>Continuous, not integer. {@code LY/500 - 1} reads either way, and the continuous reading
     * makes the reward grow with every light-year instead of jumping in blocks of 500 - the more
     * useful behaviour for a value the ride engine counts finely. Both readings agree at the two
     * ends, so the specified ceilings are unaffected.</p>
     */
    public double performanceFactor(int distanceLy) {
        return Mth.clamp((double) distanceLy / stepLy - 1.0, 0.0, (double) maxSteps);
    }

    public int stepLy() {
        return stepLy;
    }

    public int maxSteps() {
        return maxSteps;
    }

    /** Level delta applied to the player's party median. Not scaled by distance: it is not an odd. */
    public int levelOffset(int rank) {
        return levelOffset[clampRank(rank)];
    }

    /** How many IVs are forced to 31, through {@code min_perfect_ivs}. Not scaled by distance. */
    public int ivPerfectCount(int rank) {
        return Mth.clamp(ivPerfectCount[clampRank(rank)], 0, MAX_PERFECT_IVS);
    }

    /**
     * Shiny chance in percent.
     *
     * <p>Floored at the rank-0 value: the specified rank-3 curve is {@code 4p} with no constant
     * term, so it would pay 0% below 500 light-years - strictly less than rank 0's flat 1%. Rank 3
     * is out of reach that early under today's bands, but the anomaly would survive a retune of
     * {@code difficulty.json}, which is exactly what that file exists to allow. One
     * {@code Math.max}, and a better rank can never pay less than a worse one.</p>
     */
    public double shinyPercent(int rank, double p) {
        return Math.max(at(shinyPercentBase, shinyPercentPerStep, rank, p), shinyPercentBase[0]);
    }

    /** Extra hidden-ability chance in percent, on top of whatever the mod's own boosts add later. */
    public double hiddenAbilityPercent(int rank, double p) {
        return at(hiddenAbilityPercentBase, hiddenAbilityPercentPerStep, rank, p);
    }

    /** Chance of a first bonus egg/TM move, in percent. */
    public double bonusMovePercent(int rank, double p) {
        return at(bonusMovePercentBase, bonusMovePercentPerStep, rank, p);
    }

    /** Chance of a second bonus move, in percent. Only rolled when the first one landed. */
    public double secondMovePercent(int rank, double p) {
        return at(secondMovePercentBase, secondMovePercentPerStep, rank, p);
    }

    /**
     * Chance for the reward to be an alpha, in percent. Live since Cobblemon 1.8, which is the
     * release that introduced the concept at all.
     */
    public double alphaPercent(int rank, double p) {
        return at(alphaPercentBase, alphaPercentPerStep, rank, p);
    }

    private double at(double[] base, double[] perStep, int rank, double p) {
        int r = clampRank(rank);
        return Mth.clamp(base[r] + perStep[r] * p, 0.0, 100.0);
    }

    private static int clampRank(int rank) {
        return Mth.clamp(rank, 0, WarpRideDifficulty.RANK_COUNT - 1);
    }

    /** The shipped scale, used verbatim whenever the {@code rewards} block is absent or rejected. */
    public static WarpRideRewards defaults() {
        return new WarpRideRewards(
                500, 9,
                new int[]{-10, -5, 0, 5},
                new int[]{3, 4, 5, 6},
                // shiny: 1 / 1+p / 1+2p / 4p  ->  1 / 10 / 19 / 36 at p = 9
                new double[]{1.0, 1.0, 1.0, 0.0}, new double[]{0.0, 1.0, 2.0, 4.0},
                // hidden ability: 0 / 10 / 20 / 40 at p = 9
                new double[]{0.0, 1.0, 2.0, 4.0}, new double[]{0.0, 1.0, 2.0, 4.0},
                // first bonus move: 0 / 33 / 66 / 100 at p = 9
                new double[]{0.0, 3.3, 6.6, 10.0}, new double[]{0.0, 3.3, 6.6, 10.0},
                // second bonus move: rank 3 only, 50 at p = 9
                new double[]{0.0, 0.0, 0.0, 5.0}, new double[]{0.0, 0.0, 0.0, 5.0},
                // alpha, never applied: 0 / 10 / 20 / 30 at p = 9
                new double[]{0.0, 1.0, 2.0, 3.0}, new double[]{0.0, 1.0, 2.0, 3.0});
    }
}
