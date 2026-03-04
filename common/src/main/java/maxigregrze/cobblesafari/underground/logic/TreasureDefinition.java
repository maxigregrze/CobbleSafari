package maxigregrze.cobblesafari.underground.logic;

import net.minecraft.world.item.Item;

import java.util.concurrent.ThreadLocalRandom;

public class TreasureDefinition {

    private final String id;
    private final String textureId;
    private final boolean[][] shapeMatrix;
    private final Item rewardItem;
    private final int weight;
    private final int minQty;
    private final int maxQty;

    private final boolean isDisabled;

    public TreasureDefinition(String id, String textureId, boolean[][] shapeMatrix,
                              Item rewardItem, int weight, int minQty, int maxQty) {
        this(id, textureId, shapeMatrix, rewardItem, weight, minQty, maxQty, false);
    }

    public TreasureDefinition(String id, String textureId, boolean[][] shapeMatrix,
                              Item rewardItem, int weight, int minQty, int maxQty, boolean isDisabled) {
        this.id = id;
        this.textureId = textureId;
        this.shapeMatrix = shapeMatrix;
        this.rewardItem = rewardItem;
        this.weight = weight;
        this.minQty = minQty;
        this.maxQty = maxQty;
        this.isDisabled = isDisabled;
    }

    public String getId() {
        return id;
    }

    public String getTextureId() {
        return textureId;
    }

    public boolean[][] getShapeMatrix() {
        return shapeMatrix;
    }

    public Item getRewardItem() {
        return rewardItem;
    }

    public int getRewardCount() {
        if (minQty >= maxQty) return minQty;
        return ThreadLocalRandom.current().nextInt(maxQty - minQty + 1) + minQty;
    }

    public int getMinQty() {
        return minQty;
    }

    public int getMaxQty() {
        return maxQty;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public int getRarity() {
        return getWeight();
    }

    public int getWidth() {
        if (shapeMatrix == null || shapeMatrix.length == 0) return 0;
        return shapeMatrix[0].length;
    }

    public int getHeight() {
        if (shapeMatrix == null) return 0;
        return shapeMatrix.length;
    }

    public boolean isOccupied(int row, int col) {
        if (row < 0 || row >= getHeight() || col < 0 || col >= getWidth()) {
            return false;
        }
        return shapeMatrix[row][col];
    }

    public int getCellCount() {
        int count = 0;
        for (boolean[] row : shapeMatrix) {
            for (boolean cell : row) {
                if (cell) count++;
            }
        }
        return count;
    }
}
