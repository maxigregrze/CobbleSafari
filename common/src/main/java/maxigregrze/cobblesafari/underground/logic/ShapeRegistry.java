package maxigregrze.cobblesafari.underground.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ShapeRegistry {

    private ShapeRegistry() {}

    private static final Map<String, ShapeDefinition> SHAPES = new HashMap<>();
    private static final String DATA_DIR = "underground_shape";

    public static void load(MinecraftServer server) {
        SHAPES.clear();
        ResourceManager manager = server.getResourceManager();
        Map<ResourceLocation, Resource> resources = manager.listResources(DATA_DIR,
                id -> id.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                loadSingleShape(fileId, reader);
            } catch (Exception e) {
                CobbleSafari.LOGGER.error("[ShapeRegistry] Failed to load shape file {}", fileId, e);
            }
        }

        CobbleSafari.LOGGER.info("Loaded {} underground shapes", SHAPES.size());
    }

    private static void loadSingleShape(ResourceLocation fileId, InputStreamReader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject() || !root.getAsJsonObject().has("matrix")) {
            CobbleSafari.LOGGER.warn("[ShapeRegistry] Shape file {} missing 'matrix', skipping", fileId);
            return;
        }
        JsonArray matrixArray = root.getAsJsonObject().getAsJsonArray("matrix");
        boolean[][] matrix = parseMatrix(matrixArray);
        if (matrix.length == 0) {
            CobbleSafari.LOGGER.warn("[ShapeRegistry] Shape file {} has invalid matrix, skipping", fileId);
            return;
        }

        String path = fileId.getPath();
        String name = path.substring(DATA_DIR.length() + 1, path.length() - 5);
        String shapeId = fileId.getNamespace() + ":" + name;

        SHAPES.put(shapeId, new ShapeDefinition(shapeId, matrix));
    }

    public static ShapeDefinition getShape(String id) {
        return SHAPES.get(id);
    }

    public static Map<String, ShapeDefinition> getAllShapes() {
        return new HashMap<>(SHAPES);
    }

    public static void registerClientShape(String id, boolean[][] matrix) {
        SHAPES.put(id, new ShapeDefinition(id, matrix));
    }

    public static void clear() {
        SHAPES.clear();
    }

    private static boolean[][] parseMatrix(JsonArray matrixArray) {
        List<boolean[]> rows = new ArrayList<>();
        for (JsonElement rowElement : matrixArray) {
            if (!rowElement.isJsonArray()) return new boolean[0][];
            JsonArray rowArray = rowElement.getAsJsonArray();
            boolean[] row = new boolean[rowArray.size()];
            for (int i = 0; i < rowArray.size(); i++) {
                row[i] = rowArray.get(i).getAsBoolean();
            }
            rows.add(row);
        }
        return rows.toArray(new boolean[0][]);
    }
}
