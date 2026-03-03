package maxigregrze.cobblesafari.world;

import maxigregrze.cobblesafari.CobbleSafari;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class DimensionClearer {

    private DimensionClearer() {}

    public static boolean clearDimension(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        LevelStorageSource.LevelStorageAccess storage = server.storageSource;
        Path dimensionPath = storage.getDimensionPath(dimensionKey);
        if (!Files.exists(dimensionPath)) {
            CobbleSafari.LOGGER.info("Dimension folder does not exist: {}", dimensionPath);
            return true;
        }
        try (var pathStream = Files.walk(dimensionPath).sorted(Comparator.reverseOrder())) {
            pathStream.forEach(path -> {
                try {
                    Files.delete(path);
                } catch (Exception e) {
                    CobbleSafari.LOGGER.warn("Failed to delete {}: {}", path, e.getMessage());
                }
            });
            CobbleSafari.LOGGER.info("Deleted dimension folder: {}", dimensionPath);
            return true;
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("Failed to clear dimension {}: {}", dimensionKey.location(), e.getMessage());
            return false;
        }
    }
}
