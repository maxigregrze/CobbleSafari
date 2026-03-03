package maxigregrze.cobblesafari.underground.logic;

/**
 * Represents a treasure that has been placed on the grid.
 */
public class PlacedTreasure {
    
    private final TreasureDefinition definition;
    private final int startX; // Grid column
    private final int startY; // Grid row
    private boolean revealed;
    
    public PlacedTreasure(TreasureDefinition definition, int startX, int startY) {
        this.definition = definition;
        this.startX = startX;
        this.startY = startY;
        this.revealed = false;
    }
    
    public TreasureDefinition getDefinition() {
        return definition;
    }
    
    public String getId() {
        return definition.getId();
    }
    
    public int getStartX() {
        return startX;
    }
    
    public int getStartY() {
        return startY;
    }
    
    public int getWidth() {
        return definition.getWidth();
    }
    
    public int getHeight() {
        return definition.getHeight();
    }
    
    public boolean isRevealed() {
        return revealed;
    }
    
    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }
    
    /**
     * Check if this treasure occupies the given cell.
     */
    public boolean occupiesCell(int x, int y) {
        int localX = x - startX;
        int localY = y - startY;
        return definition.isOccupied(localY, localX);
    }
    
    /**
     * Check if all cells of this treasure are revealed on the grid.
     */
    public boolean checkFullyRevealed(MiningGrid grid) {
        for (int dy = 0; dy < getHeight(); dy++) {
            for (int dx = 0; dx < getWidth(); dx++) {
                if (definition.isOccupied(dy, dx)) {
                    int gridX = startX + dx;
                    int gridY = startY + dy;
                    MiningCell cell = grid.getCell(gridX, gridY);
                    if (cell == null || !cell.isRevealed()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
