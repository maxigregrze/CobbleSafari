package maxigregrze.cobblesafari.underground.logic;

public class ShapeDefinition {

    private final String id;
    private final boolean[][] matrix;

    public ShapeDefinition(String id, boolean[][] matrix) {
        this.id = id;
        this.matrix = matrix;
    }

    public String getId() {
        return id;
    }

    public boolean[][] getMatrix() {
        return matrix;
    }

    public int getWidth() {
        if (matrix == null || matrix.length == 0) return 0;
        return matrix[0].length;
    }

    public int getHeight() {
        if (matrix == null) return 0;
        return matrix.length;
    }
}
