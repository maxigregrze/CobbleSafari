package maxigregrze.cobblesafari.underground.logic;

/**
 * Represents a single cell in the mining grid.
 * Each cell has two layers: the wall layer (tier 0-6) and the content layer underneath.
 */
public class MiningCell {
    
    /** Wall tier levels - higher number means more hits required to break */
    public enum WallTier {
        TIER_0(0), // Fully mined
        TIER_1(1),
        TIER_2(2),
        TIER_3(3),
        TIER_4(4),
        TIER_5(5),
        TIER_6(6);
        
        private final int value;
        
        WallTier(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static WallTier fromValue(int value) {
            for (WallTier tier : values()) {
                if (tier.value == value) return tier;
            }
            return TIER_0;
        }
        
        public WallTier applyDamage(int damage) {
            int newValue = Math.max(0, this.value - damage);
            return fromValue(newValue);
        }
        
        public boolean isRevealed() {
            return this == TIER_0;
        }
    }
    
    /** Content types for the second (hidden) layer */
    public enum SecondLayerContent {
        EMPTY,      // Nothing underneath
        TREASURE,   // Part of a treasure
        IRON_BLOCK  // Iron block obstacle
    }
    
    private WallTier currentTier;
    private SecondLayerContent secondLayerContent;
    private String treasureId; // Only set if secondLayerContent == TREASURE
    
    public MiningCell() {
        this.currentTier = WallTier.TIER_1;
        this.secondLayerContent = SecondLayerContent.EMPTY;
        this.treasureId = null;
    }
    
    public MiningCell(WallTier tier) {
        this.currentTier = tier;
        this.secondLayerContent = SecondLayerContent.EMPTY;
        this.treasureId = null;
    }
    
    /**
     * Apply damage to this cell's wall tier.
     * @param damage Amount of damage to apply
     * @return true if the wall was destroyed (revealed the second layer)
     */
    public boolean applyDamage(int damage) {
        if (currentTier == WallTier.TIER_0) {
            return false; // Already revealed
        }
        
        WallTier oldTier = currentTier;
        currentTier = currentTier.applyDamage(damage);
        
        return currentTier == WallTier.TIER_0 && oldTier != WallTier.TIER_0;
    }
    
    public boolean isRevealed() {
        return currentTier.isRevealed();
    }
    
    public boolean isIronBlock() {
        return secondLayerContent == SecondLayerContent.IRON_BLOCK;
    }
    
    public boolean isTreasure() {
        return secondLayerContent == SecondLayerContent.TREASURE;
    }
    
    // Getters and setters
    public WallTier getCurrentTier() {
        return currentTier;
    }
    
    public void setCurrentTier(WallTier tier) {
        this.currentTier = tier;
    }
    
    public SecondLayerContent getSecondLayerContent() {
        return secondLayerContent;
    }
    
    public void setSecondLayerContent(SecondLayerContent content) {
        this.secondLayerContent = content;
    }
    
    public String getTreasureId() {
        return treasureId;
    }
    
    public void setTreasureId(String treasureId) {
        this.treasureId = treasureId;
    }
    
    public void setAsTreasure(String treasureId) {
        this.secondLayerContent = SecondLayerContent.TREASURE;
        this.treasureId = treasureId;
    }
    
    public void setAsIronBlock() {
        this.secondLayerContent = SecondLayerContent.IRON_BLOCK;
        this.treasureId = null;
    }
}
