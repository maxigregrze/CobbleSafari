package maxigregrze.cobblesafari.warpride.logic;

import org.jetbrains.annotations.Nullable;

/**
 * One Ultra Space destination. {@code typeId} is stable and unique ({@code >= 1}) and is what the
 * sub-instance record stores; {@code weight} is its share of a portal roll; {@code tint} is ARGB for
 * the portal sprite.
 *
 * <p>The encounter comes from two pools (action plan 190, E1): {@code normalPool}, and
 * {@code rewardPool} - the world's Ultra Beast - drawn with the per-rank chance of
 * {@link #rewardPoolPercent(int, WarpRideRewards)}.</p>
 *
 * <p>{@code destinationId} is the human name, derived by the loader from the file name rather than
 * authored (action plan 184, Decision section 12.3): {@code typeId} is already the machine identity
 * and the file name is already the human one, so a {@code name} field would only be a second source
 * of truth to keep in sync. It exists for the server log, and reaches no client.</p>
 *
 * @param rewardPoolPercentOverride four per-rank percentages replacing the global
 *                                  {@code difficulty.json} row, or {@code null} to use it
 */
public record WarpRideDestination(
        String destinationId,
        int typeId,
        int weight,
        String structurePool,
        int tint,
        String dimension,
        String biome,
        WarpRideEncounterPool normalPool,
        WarpRideEncounterPool rewardPool,
        double @Nullable [] rewardPoolPercentOverride
) {

    /** Id of the registered sub-dungeon hosting this destination. */
    public String dungeonId() {
        return hasOwnDimension()
                ? dimension.substring(dimension.indexOf(':') + 1)
                : SHARED_DUNGEON_ID;
    }

    /** @return true when this destination generates in its own dimension rather than the shared one. */
    public boolean hasOwnDimension() {
        return dimension != null && !dimension.isBlank();
    }

    /**
     * Biome to stamp on the footprint, or {@code null}. Always {@code null} for a destination that
     * owns its dimension: there the biome comes from the dimension's own generator, and stamping
     * one over the structure would only fight it.
     */
    public String effectiveBiome() {
        return hasOwnDimension() ? null : biome;
    }

    /** Chance, in percent, that a rank-{@code rank} encounter is drawn from the reward pool. */
    public double rewardPoolPercent(int rank, WarpRideRewards rewards) {
        if (rewardPoolPercentOverride != null) {
            return rewardPoolPercentOverride[Math.clamp(rank, 0, rewardPoolPercentOverride.length - 1)];
        }
        return rewards.rewardPoolPercent(rank);
    }

    /** The shared sub-dimension, used by every destination that does not name one. */
    public static final String SHARED_DUNGEON_ID = "dungeon_ultraspace";
}
