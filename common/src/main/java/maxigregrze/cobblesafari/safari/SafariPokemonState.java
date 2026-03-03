package maxigregrze.cobblesafari.safari;

import maxigregrze.cobblesafari.config.SafariConfig;

import java.util.UUID;

public class SafariPokemonState {

    private final UUID pokemonEntityId;
    private boolean fleeing = false;
    private long fleeStartTick = 0;
    private int fleeToken = 0;
    private int moodLevel = 0;

    public SafariPokemonState(UUID pokemonEntityId) {
        this.pokemonEntityId = pokemonEntityId;
    }

    public UUID getPokemonEntityId() {
        return pokemonEntityId;
    }

    public int getMoodLevel() {
        return moodLevel;
    }

    public int applyMudBall() {
        int maxMood = SafariConfig.getMaxMoodLevel();
        int oldMood = moodLevel;
        moodLevel = Math.max(-maxMood, moodLevel - 1);
        return oldMood - moodLevel;
    }

    public int applyBait() {
        int maxMood = SafariConfig.getMaxMoodLevel();
        int oldMood = moodLevel;
        moodLevel = Math.min(maxMood, moodLevel + 1);
        return moodLevel - oldMood;
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
}
