package maxigregrze.cobblesafari.underground.logic;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The main mining grid containing cells with wall layers and hidden treasures/iron.
 */
public class MiningGrid {
    
    public static final int WIDTH = 13;
    public static final int HEIGHT = 10;
    public static final int CELL_SIZE = 16; // pixels
    
    public static final int MIN_TREASURES = 2;
    public static final int MAX_TREASURES = 5;
    public static final int MIN_IRON_CLUSTERS = 3;
    public static final int MAX_IRON_CLUSTERS = 5;
    public static final int MIN_IRON_PER_CLUSTER = 2;
    public static final int MAX_IRON_PER_CLUSTER = 8;
    
    private static final int PLACEMENT_ATTEMPTS = 100;
    private static final int IRON_PLACEMENT_ATTEMPTS = 100;
    
    /** Scale for Perlin sampling: smaller = larger smooth regions (tier blobs). */
    private static final double PERLIN_SCALE = 0.25;

    private final MiningCell[][] cells;
    private final List<PlacedTreasure> treasures;
    private final WallStability stability;
    private final Random random;
    private final PerlinNoise tierNoise;

    public MiningGrid(long seed) {
        this.cells = new MiningCell[HEIGHT][WIDTH];
        this.treasures = new ArrayList<>();
        this.stability = new WallStability();
        this.random = new Random(seed);
        this.tierNoise = new PerlinNoise(seed);
        initializeGrid();
    }

    /**
     * Initialize the grid with wall tiers from Perlin noise.
     * 1–2 "peaks" (zones tier 6) are found, then each cell gets a tier from its distance to the nearest peak:
     * at the peak = tier 6, then a gradient of a few cells down to tier 1.
     */
    private void initializeGrid() {
        double[][] values = new double[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                values[y][x] = tierNoise.noise2dNormalized(x * PERLIN_SCALE, y * PERLIN_SCALE);
            }
        }
        List<int[]> peaks = findPeaks(values);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double distFromPeak = distanceToNearestPeak(x, y, values, peaks);
                MiningCell.WallTier tier = distanceToTier(distFromPeak);
                cells[y][x] = new MiningCell(tier);
            }
        }
    }

    /** Find 1 or 2 peaks (local maxima) in the value grid. */
    private List<int[]> findPeaks(double[][] values) {
        List<int[]> peaks = new ArrayList<>();
        double globalMax = -1;
        int maxX = 0;
        int maxY = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (values[y][x] > globalMax) {
                    globalMax = values[y][x];
                    maxX = x;
                    maxY = y;
                }
            }
        }
        peaks.add(new int[]{maxX, maxY});
        int secondX = -1;
        int secondY = -1;
        double secondVal = -1;
        int minDistFromFirst = 4;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (Math.abs(x - maxX) + Math.abs(y - maxY) < minDistFromFirst) {
                    continue;
                }
                if (!isLocalMax(values, x, y)) {
                    continue;
                }
                if (values[y][x] > secondVal && values[y][x] > 0.4) {
                    secondVal = values[y][x];
                    secondX = x;
                    secondY = y;
                }
            }
        }
        if (secondX >= 0 && secondY >= 0) {
            peaks.add(new int[]{secondX, secondY});
        }
        return peaks;
    }

    private boolean isLocalMax(double[][] values, int x, int y) {
        double v = values[y][x];
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < WIDTH && ny >= 0 && ny < HEIGHT && values[ny][nx] > v) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Distance in value space from cell (x,y) to the nearest peak (0 at peak, up to ~1). */
    private double distanceToNearestPeak(int x, int y, double[][] values, List<int[]> peaks) {
        double v = values[y][x];
        double minDist = 1.0;
        for (int[] p : peaks) {
            double pVal = values[p[1]][p[0]];
            double dist = Math.max(0, pVal - v);
            if (dist < minDist) minDist = dist;
        }
        return minDist;
    }

    /** Map distance from peak [0, ~1] to tier: short gradient so T1/T2 visible. */
    private static MiningCell.WallTier distanceToTier(double distFromPeak) {
        if (distFromPeak <= 0.08) return MiningCell.WallTier.TIER_6;
        if (distFromPeak <= 0.15) return MiningCell.WallTier.TIER_5;
        if (distFromPeak <= 0.20) return MiningCell.WallTier.TIER_4;
        if (distFromPeak <= 0.25) return MiningCell.WallTier.TIER_3;
        if (distFromPeak <= 0.45) return MiningCell.WallTier.TIER_2;
        return MiningCell.WallTier.TIER_1;
    }
    
    /**
     * Generate treasures and iron blocks on the grid.
     */
    public void generateContent() {
        // Generate treasures
        int treasureCount = MIN_TREASURES + random.nextInt(MAX_TREASURES - MIN_TREASURES + 1);
        for (int i = 0; i < treasureCount; i++) {
            placeTreasure();
        }
        
        // Generate iron clusters
        int clusterCount = MIN_IRON_CLUSTERS + random.nextInt(MAX_IRON_CLUSTERS - MIN_IRON_CLUSTERS + 1);
        for (int i = 0; i < clusterCount; i++) {
            placeIronCluster();
        }
    }
    
    /**
     * Attempt to place a random treasure on the grid.
     */
    private void placeTreasure() {
        TreasureDefinition treasure = TreasureRegistry.getRandomTreasure(random);
        if (treasure == null) return;

        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            int x = random.nextInt(WIDTH - treasure.getWidth() + 1);
            int y = random.nextInt(HEIGHT - treasure.getHeight() + 1);
            
            if (canPlaceTreasure(treasure, x, y)) {
                doPlaceTreasure(treasure, x, y);
                return;
            }
        }
        // Failed to place after max attempts - skip this treasure
    }
    
    /**
     * Check if a treasure can be placed at the given position.
     */
    private boolean canPlaceTreasure(TreasureDefinition treasure, int startX, int startY) {
        // Check the treasure cells and 1-cell spacing around them
        for (int dy = -1; dy <= treasure.getHeight(); dy++) {
            for (int dx = -1; dx <= treasure.getWidth(); dx++) {
                int x = startX + dx;
                int y = startY + dy;
                
                // Skip cells outside the grid (edge spacing is OK)
                if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
                    continue;
                }
                
                MiningCell cell = cells[y][x];
                
                // For cells that would be occupied by the treasure
                boolean isInTreasure = dx >= 0 && dx < treasure.getWidth() && 
                                       dy >= 0 && dy < treasure.getHeight() &&
                                       treasure.isOccupied(dy, dx);
                
                if (isInTreasure) {
                    // Cell must be empty (no other treasure or iron)
                    if (cell.getSecondLayerContent() != MiningCell.SecondLayerContent.EMPTY) {
                        return false;
                    }
                } else {
                    // Spacing cell - must not be another treasure
                    if (cell.getSecondLayerContent() == MiningCell.SecondLayerContent.TREASURE) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Place a treasure at the given position.
     */
    private void doPlaceTreasure(TreasureDefinition treasure, int startX, int startY) {
        PlacedTreasure placed = new PlacedTreasure(treasure, startX, startY);
        treasures.add(placed);
        
        for (int dy = 0; dy < treasure.getHeight(); dy++) {
            for (int dx = 0; dx < treasure.getWidth(); dx++) {
                if (treasure.isOccupied(dy, dx)) {
                    cells[startY + dy][startX + dx].setAsTreasure(treasure.getId());
                }
            }
        }
    }
    
    /**
     * Place a cluster of connected iron blocks.
     * Only places iron in cells that are empty and not within any treasure's bounding box.
     */
    private void placeIronCluster() {
        int clusterSize = MIN_IRON_PER_CLUSTER + random.nextInt(MAX_IRON_PER_CLUSTER - MIN_IRON_PER_CLUSTER + 1);
        
        for (int attempt = 0; attempt < IRON_PLACEMENT_ATTEMPTS; attempt++) {
            int startX = random.nextInt(WIDTH);
            int startY = random.nextInt(HEIGHT);
            
            // Check if starting position is valid (empty and not in treasure bounding box)
            if (isValidIronPosition(startX, startY, new ArrayList<>())) {
                List<int[]> cluster = growIronCluster(startX, startY, clusterSize);
                if (cluster.size() >= MIN_IRON_PER_CLUSTER) {
                    for (int[] pos : cluster) {
                        cells[pos[1]][pos[0]].setAsIronBlock();
                    }
                    return;
                }
            }
        }
    }
    
    /**
     * Grow an iron cluster from a starting position.
     */
    private List<int[]> growIronCluster(int startX, int startY, int targetSize) {
        List<int[]> cluster = new ArrayList<>();
        cluster.add(new int[]{startX, startY});
        
        // Cardinal directions only (no diagonal)
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        
        while (cluster.size() < targetSize) {
            // Find valid expansion candidates
            List<int[]> candidates = new ArrayList<>();
            
            for (int[] pos : cluster) {
                for (int[] dir : directions) {
                    int nx = pos[0] + dir[0];
                    int ny = pos[1] + dir[1];
                    
                    if (isValidIronPosition(nx, ny, cluster)) {
                        candidates.add(new int[]{nx, ny});
                    }
                }
            }
            
            if (candidates.isEmpty()) {
                break; // Can't grow anymore
            }
            
            // Pick a random candidate
            int[] chosen = candidates.get(random.nextInt(candidates.size()));
            cluster.add(chosen);
        }
        
        return cluster;
    }
    
    /**
     * Check if a position is valid for iron placement.
     * A position is invalid if:
     * - It's outside the grid
     * - It's already in the cluster being built
     * - The cell is not empty (already has treasure or iron)
     * - It's within the bounding box of any existing treasure
     */
    private boolean isValidIronPosition(int x, int y, List<int[]> existingCluster) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return false;
        }
        
        // Check if already in cluster
        for (int[] pos : existingCluster) {
            if (pos[0] == x && pos[1] == y) {
                return false;
            }
        }
        
        // Cell must be empty (no treasure or iron already placed)
        if (cells[y][x].getSecondLayerContent() != MiningCell.SecondLayerContent.EMPTY) {
            return false;
        }
        
        // Check if this position is within the bounding box of any existing treasure
        // This prevents iron from being placed where a treasure texture would be displayed
        for (PlacedTreasure treasure : treasures) {
            int treasureStartX = treasure.getStartX();
            int treasureStartY = treasure.getStartY();
            int treasureWidth = treasure.getWidth();
            int treasureHeight = treasure.getHeight();
            
            // Check if (x, y) is within the treasure's bounding box
            if (x >= treasureStartX && x < treasureStartX + treasureWidth &&
                y >= treasureStartY && y < treasureStartY + treasureHeight) {
                return false; // Position is within a treasure's bounding box
            }
        }
        
        return true;
    }
    
    /**
     * Get the iron connection directions for a cell (for texture selection).
     * Returns a string in NWSE order (North, West, South, East) to match texture filenames.
     */
    public String getIronConnections(int x, int y) {
        if (!cells[y][x].isIronBlock()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        if (y > 0 && cells[y - 1][x].isIronBlock()) sb.append("n");
        if (x > 0 && cells[y][x - 1].isIronBlock()) sb.append("w");
        if (y < HEIGHT - 1 && cells[y + 1][x].isIronBlock()) sb.append("s");
        if (x < WIDTH - 1 && cells[y][x + 1].isIronBlock()) sb.append("e");
        return sb.isEmpty() ? "none" : sb.toString();
    }
    
    /**
     * Apply mining action at a position.
     * @return true if any cell was affected
     */
    public MiningResult mine(int centerX, int centerY, boolean useHammer) {
        List<int[]> affectedCells = getAffectedCells(centerX, centerY, useHammer);
        List<CellUpdate> updates = new ArrayList<>();
        boolean hitIron = false;
        
        for (int[] pos : affectedCells) {
            int x = pos[0];
            int y = pos[1];
            int damage = pos[2];
            
            if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
                continue;
            }
            
            MiningCell cell = cells[y][x];
            
            // Check for iron hit (only counts if cell is revealed or will be revealed)
            if (cell.isRevealed() && cell.isIronBlock()) {
                hitIron = true;
                continue; // Iron blocks can't be mined further
            }
            
            // Apply damage
            boolean wasRevealed = cell.applyDamage(damage);
            
            // Check if we just revealed an iron block
            if (wasRevealed && cell.isIronBlock()) {
                hitIron = true;
            }
            
            updates.add(new CellUpdate(x, y, cell.getCurrentTier().getValue(), cell.isRevealed()));
        }
        
        // Register stability hit
        int stabilityDamage = 0;
        if (!updates.isEmpty()) {
            stabilityDamage = stability.registerHit(useHammer, hitIron);
        }
        
        // Check for newly revealed treasures
        List<PlacedTreasure> newlyRevealed = checkTreasureRevelations();
        
        return new MiningResult(updates, stabilityDamage, hitIron, newlyRevealed, stability.hasCollapsed());
    }
    
    /**
     * Get the cells affected by a mining action.
     */
    private List<int[]> getAffectedCells(int centerX, int centerY, boolean useHammer) {
        List<int[]> cells = new ArrayList<>();
        
        if (useHammer) {
            // 3x3 pattern
            // Corners get 1 damage, center and cardinals get 2
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    boolean isCorner = (dx != 0 && dy != 0);
                    int damage = isCorner ? 1 : 2;
                    cells.add(new int[]{centerX + dx, centerY + dy, damage});
                }
            }
        } else {
            // + pattern (pickaxe)
            // Center gets 2 damage, cardinals get 1
            cells.add(new int[]{centerX, centerY, 2}); // Center
            cells.add(new int[]{centerX, centerY - 1, 1}); // North
            cells.add(new int[]{centerX, centerY + 1, 1}); // South
            cells.add(new int[]{centerX - 1, centerY, 1}); // West
            cells.add(new int[]{centerX + 1, centerY, 1}); // East
        }
        
        return cells;
    }
    
    /**
     * Check all treasures to see if any are now fully revealed.
     */
    private List<PlacedTreasure> checkTreasureRevelations() {
        List<PlacedTreasure> newlyRevealed = new ArrayList<>();
        
        for (PlacedTreasure treasure : treasures) {
            if (!treasure.isRevealed() && treasure.checkFullyRevealed(this)) {
                treasure.setRevealed(true);
                newlyRevealed.add(treasure);
            }
        }
        
        return newlyRevealed;
    }
    
    // Getters
    public MiningCell getCell(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return null;
        }
        return cells[y][x];
    }
    
    public List<PlacedTreasure> getTreasures() {
        return treasures;
    }
    
    public List<PlacedTreasure> getRevealedTreasures() {
        List<PlacedTreasure> revealed = new ArrayList<>();
        for (PlacedTreasure t : treasures) {
            if (t.isRevealed()) revealed.add(t);
        }
        return revealed;
    }
    
    public int getTreasureCount() {
        return treasures.size();
    }
    
    public WallStability getStability() {
        return stability;
    }
    
    /**
     * Serialize grid state for network transmission.
     * Format: [grid: tier+content per cell] [placed_treasure_count: short] [for each: startX, startY, treasureId utf]
     * startX, startY = top-left of the shape matrix (even if that cell is false).
     */
    public byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    MiningCell cell = cells[y][x];
                    out.writeByte(cell.getCurrentTier().getValue());
                    out.writeByte(cell.getSecondLayerContent().ordinal());
                }
            }
            
            out.writeShort(treasures.size());
            for (PlacedTreasure t : treasures) {
                out.writeByte(t.getStartX());
                out.writeByte(t.getStartY());
                writeUtf(out, t.getId());
            }
            
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize grid", e);
        }
    }
    
    private static void writeUtf(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }
    
    /**
     * Deserialize grid state from network data.
     * Legacy format (exactly WIDTH*HEIGHT*2 bytes): grid only, no treasure IDs.
     * New format: grid + treasure_count + treasure entries.
     */
    public void deserialize(byte[] data) {
        if (data.length < WIDTH * HEIGHT * 2) {
            return;
        }
        
        int index = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int tier = data[index++] & 0xFF;
                int content = data[index++] & 0xFF;
                
                cells[y][x].setCurrentTier(MiningCell.WallTier.fromValue(tier));
                cells[y][x].setSecondLayerContent(MiningCell.SecondLayerContent.values()[content]);
                cells[y][x].setTreasureId(null); // Clear, will be set from trailer if present
            }
        }
        
        // New format: read placed treasures (startX, startY, treasureId) = matrix top-left
        if (index >= data.length) return;
        try {
            treasures.clear();
            int count = ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
            index += 2;
            for (int i = 0; i < count && index < data.length; i++) {
                int startX = data[index++] & 0xFF;
                int startY = data[index++] & 0xFF;
                if (index + 2 <= data.length) {
                    int utfLen = ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
                    index += 2;
                    if (index + utfLen <= data.length && utfLen > 0) {
                        String treasureId = new String(data, index, utfLen, StandardCharsets.UTF_8);
                        index += utfLen;
                        TreasureDefinition def = TreasureRegistry.getById(treasureId);
                        if (def != null && startX < WIDTH && startY < HEIGHT) {
                            treasures.add(new PlacedTreasure(def, startX, startY));
                            boolean[][] shape = def.getShapeMatrix();
                            for (int row = 0; row < shape.length; row++) {
                                for (int col = 0; col < shape[row].length; col++) {
                                    if (shape[row][col]) {
                                        int gx = startX + col;
                                        int gy = startY + row;
                                        if (gx < WIDTH && gy < HEIGHT) {
                                            cells[gy][gx].setTreasureId(treasureId);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed trailer
        }
    }
    
    // Result classes
    public record CellUpdate(int x, int y, int newTier, boolean revealed) {}
    
    public record MiningResult(
        List<CellUpdate> updates,
        int stabilityDamage,
        boolean hitIron,
        List<PlacedTreasure> newlyRevealedTreasures,
        boolean collapsed
    ) {}
}
