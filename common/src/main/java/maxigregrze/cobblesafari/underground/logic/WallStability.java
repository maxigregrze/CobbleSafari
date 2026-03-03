package maxigregrze.cobblesafari.underground.logic;

/**
 * Manages the wall stability system.
 * Stability starts at 50, decreases with each action, and the wall collapses at 0.
 * First hammer use permanently reduces max stability to 40.
 */
public class WallStability {
    
    public static final int INITIAL_MAX_STABILITY = 64;
    public static final int HAMMER_REDUCED_MAX = 54;
    
    public static final int PICKAXE_BASE_DAMAGE = 1;
    public static final int HAMMER_BASE_DAMAGE = 2;
    public static final int IRON_MULTIPLIER = 3;
    
    private int maxStability;
    private int currentStability;
    private boolean hammerUsedOnce;
    
    public WallStability() {
        this.maxStability = INITIAL_MAX_STABILITY;
        this.currentStability = INITIAL_MAX_STABILITY;
        this.hammerUsedOnce = false;
    }
    
    /**
     * Register a hit on the wall.
     * @param isHammer true if using hammer, false if using pickaxe
     * @param hitIron true if the hit touched an iron block
     * @return the amount of stability damage dealt
     */
    public int registerHit(boolean isHammer, boolean hitIron) {
        // First hammer use reduces max stability
        if (isHammer && !hammerUsedOnce) {
            hammerUsedOnce = true;
            if (currentStability > HAMMER_REDUCED_MAX) {
                currentStability = HAMMER_REDUCED_MAX;
            }
            maxStability = HAMMER_REDUCED_MAX;
        }
        
        // Calculate damage
        int baseDamage = isHammer ? HAMMER_BASE_DAMAGE : PICKAXE_BASE_DAMAGE;
        int totalDamage = hitIron ? baseDamage * IRON_MULTIPLIER : baseDamage;
        
        // Apply damage
        currentStability = Math.max(0, currentStability - totalDamage);
        
        return totalDamage;
    }
    
    /**
     * Check if the wall has collapsed.
     */
    public boolean hasCollapsed() {
        return currentStability <= 0;
    }
    
    /**
     * Get the stability percentage (0.0 to 1.0).
     */
    public float getStabilityPercentage() {
        if (maxStability <= 0) return 0f;
        return (float) currentStability / maxStability;
    }
    
    /**
     * Get the status bar level (1-5, where 1 is full and 5 is empty).
     */
    public int getStatusBarLevel() {
        float percentage = getStabilityPercentage();
        if (percentage > 0.75f) return 1;
        if (percentage > 0.50f) return 2;
        if (percentage > 0.25f) return 3;
        if (percentage > 0f) return 4;
        return 5;
    }
    
    // Getters
    public int getMaxStability() {
        return maxStability;
    }
    
    public int getCurrentStability() {
        return currentStability;
    }
    
    public boolean isHammerUsedOnce() {
        return hammerUsedOnce;
    }
    
    // For network sync
    public void setCurrentStability(int stability) {
        this.currentStability = stability;
    }
    
    public void setMaxStability(int max) {
        this.maxStability = max;
    }
    
    public void setHammerUsedOnce(boolean used) {
        this.hammerUsedOnce = used;
    }
}
