package maxigregrze.cobblesafari.underground;

import maxigregrze.cobblesafari.underground.logic.MiningGrid;
import maxigregrze.cobblesafari.underground.screen.UndergroundOpenData;

import java.util.UUID;

/**
 * Represents an active mining session for a player.
 */
public class MiningSession {
    
    private final UUID sessionId;
    private final MiningGrid grid;
    private boolean usingHammer;
    private boolean complete;
    
    public MiningSession(UUID sessionId, long seed) {
        this.sessionId = sessionId;
        this.grid = new MiningGrid(seed);
        this.grid.generateContent();
        this.usingHammer = false;
        this.complete = false;
    }
    
    /**
     * Perform a mining action at the specified position.
     */
    public MiningGrid.MiningResult mine(int x, int y) {
        if (complete) {
            return new MiningGrid.MiningResult(
                    java.util.Collections.emptyList(), 0, false, 
                    java.util.Collections.emptyList(), false
            );
        }
        
        return grid.mine(x, y, usingHammer);
    }
    
    /**
     * Create the open data for sending to the client.
     */
    public UndergroundOpenData createOpenData() {
        return new UndergroundOpenData(
                sessionId,
                grid.getTreasureCount(),
                grid.serialize(),
                grid.getStability().getCurrentStability(),
                grid.getStability().getMaxStability()
        );
    }
    
    // Getters and setters
    public UUID getSessionId() {
        return sessionId;
    }
    
    public MiningGrid getGrid() {
        return grid;
    }
    
    public boolean isUsingHammer() {
        return usingHammer;
    }
    
    public void setUsingHammer(boolean usingHammer) {
        this.usingHammer = usingHammer;
    }
    
    public boolean isComplete() {
        return complete;
    }
    
    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
