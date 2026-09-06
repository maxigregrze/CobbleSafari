package maxigregrze.cobblesafari.safari;

import maxigregrze.cobblesafari.config.SafariConfig;

import java.util.UUID;

public class SafariPokemonState {

    private final UUID pokemonEntityId;
    private boolean fleeing = false;
    private long fleeStartTick = 0;
    private int fleeToken = 0;
    private long fleeDeadlineTick = 0;
    private boolean fleeSuspended = false;
    private int fleeRemainingGraceTicks = 0;
    private boolean fled = false;
    private int moodLevel = 0;
    private UUID lastInteractingPlayer = null;
    private boolean enraged = false;
    private long rageDeadlineTick = 0;

    public SafariPokemonState(UUID pokemonEntityId) {
        this.pokemonEntityId = pokemonEntityId;
    }

    public UUID getLastInteractingPlayer() {
        return lastInteractingPlayer;
    }

    public void setLastInteractingPlayer(UUID lastInteractingPlayer) {
        this.lastInteractingPlayer = lastInteractingPlayer;
    }

    public UUID getPokemonEntityId() {
        return pokemonEntityId;
    }

    public int getMoodLevel() {
        return moodLevel;
    }

    public int applyMudBall() {
        int maxMood = SafariConfig.MAX_MOOD_LEVEL;
        int oldMood = moodLevel;
        moodLevel = Math.max(-maxMood, moodLevel - 1);
        return oldMood - moodLevel;
    }

    public int applyBait() {
        int maxMood = SafariConfig.MAX_MOOD_LEVEL;
        int oldMood = moodLevel;
        moodLevel = Math.min(maxMood, moodLevel + 1);
        return moodLevel - oldMood;
    }

    /**
     * Whether this alpha is currently retaliating instead of fleeing.
     *
     * <p>Alphas never flee in the Safari: mud balls and failed captures push them into rage
     * instead. See {@code SafariStateManager#tryEnrage}.
     */
    public boolean isEnraged() {
        return enraged;
    }

    public void setEnraged(boolean enraged) {
        this.enraged = enraged;
    }

    /** Absolute {@code SafariStateManager} tick at which the rage lapses, or {@code 0} when calm. */
    public long getRageDeadlineTick() {
        return rageDeadlineTick;
    }

    public void setRageDeadlineTick(long rageDeadlineTick) {
        this.rageDeadlineTick = rageDeadlineTick;
    }

    /**
     * Pins the mood to its most placid end: least likely to flee, hardest to catch. This is the
     * price of calming an enraged alpha with bait rather than waiting the rage out.
     */
    public void forceMoodToCalmest() {
        this.moodLevel = SafariConfig.MAX_MOOD_LEVEL;
    }

    public float getCatchRateMultiplier() {
        int absLevel = Math.abs(moodLevel);
        if (moodLevel < 0) {
            return (3.0f + absLevel) / 3.0f;
        } else if (moodLevel > 0) {
            return 3.0f / (3.0f + absLevel);
        }
        return 1.0f;
    }

    public int getActualFleeRate() {
        int baseRate = SafariConfig.getBaseFleeRate();
        int absLevel = Math.abs(moodLevel);
        float multiplier;
        
        if (moodLevel < 0) {
            multiplier = (3.0f + absLevel) / 3.0f;
        } else if (moodLevel > 0) {
            multiplier = 3.0f / (3.0f + absLevel);
        } else {
            multiplier = 1.0f;
        }
        
        return Math.min(254, Math.round(baseRate * multiplier));
    }

    public boolean isFleeing() {
        return fleeing;
    }

    public void setFleeing(boolean fleeing) {
        this.fleeing = fleeing;
    }

    public long getFleeStartTick() {
        return fleeStartTick;
    }

    public void setFleeStartTick(long fleeStartTick) {
        this.fleeStartTick = fleeStartTick;
    }

    public int getFleeToken() {
        return fleeToken;
    }

    public int incrementFleeToken() {
        return ++fleeToken;
    }

    /**
     * Absolute {@code SafariStateManager} tick at which the currently armed flee sequence resolves,
     * or {@code 0} when no flee task is armed (never started, cancelled, or suspended).
     */
    public long getFleeDeadlineTick() {
        return fleeDeadlineTick;
    }

    public void setFleeDeadlineTick(long fleeDeadlineTick) {
        this.fleeDeadlineTick = fleeDeadlineTick;
    }

    public boolean isFleeSuspended() {
        return fleeSuspended;
    }

    public void setFleeSuspended(boolean fleeSuspended) {
        this.fleeSuspended = fleeSuspended;
    }

    public int getFleeRemainingGraceTicks() {
        return fleeRemainingGraceTicks;
    }

    public void setFleeRemainingGraceTicks(int fleeRemainingGraceTicks) {
        this.fleeRemainingGraceTicks = fleeRemainingGraceTicks;
    }

    /**
     * Whether the flee has already been committed, i.e. the uncatchable property was posted and the
     * despawn fade started. Past that point nothing can rescue the Pokémon anymore.
     */
    public boolean hasFled() {
        return fled;
    }

    public void markFled() {
        this.fled = true;
    }
}
