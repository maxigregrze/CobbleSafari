package maxigregrze.cobblesafari.csmusic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.config.DimensionalMusicConfig;
import maxigregrze.cobblesafari.config.DimensionalMusicData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class CsMusicAreaStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "csmusic_areas.json";
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9._-]+");

    private CsMusicAreaStorage() {}

    static boolean loadOne(MinecraftServer server, ServerLevel level, Map<String, CsMusicArea> target) {
        Path file = areaFile(server, level);
        if (!Files.isRegularFile(file)) {
            target.clear();
            return true;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            CsMusicAreaFileData data = GSON.fromJson(reader, CsMusicAreaFileData.class);
            Map<String, CsMusicArea> parsed = new HashMap<>();
            if (data != null && data.areas != null) {
                for (CsMusicAreaFileData.AreaEntry entry : data.areas) {
                    CsMusicArea area = fromEntry(entry, file);
                    if (area != null) {
                        parsed.put(area.id(), area);
                    }
                }
            }
            target.clear();
            target.putAll(parsed);
            return true;
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[CSMusic] Failed to read {} — keeping existing in-memory areas", file, e);
            return false;
        }
    }

    static void save(MinecraftServer server, ServerLevel level, Map<String, CsMusicArea> areas) {
        Path file = areaFile(server, level);
        try {
            Files.createDirectories(file.getParent());
            CsMusicAreaFileData data = toFileData(areas);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("[CSMusic] Failed to write {}", file, e);
        }
    }

    private static Path areaFile(MinecraftServer server, ServerLevel level) {
        LevelStorageSource.LevelStorageAccess storage = server.storageSource;
        Path dimPath = storage.getDimensionPath(level.dimension());
        return dimPath.resolve("cobblesafari").resolve(FILE_NAME);
    }

    private static CsMusicArea fromEntry(CsMusicAreaFileData.AreaEntry entry, Path file) {
        if (entry == null || entry.id == null || entry.music == null) {
            return null;
        }
        String id = entry.id.trim();
        if (!ID_PATTERN.matcher(id).matches()) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} invalid area id '{}'", file, entry.id);
            return null;
        }
        String musicId = entry.music.trim();
        if (musicId.isEmpty()) {
            return null;
        }
        List<CsMusicBox> boxes = new ArrayList<>();
        if (entry.boxes != null) {
            for (CsMusicAreaFileData.BoxEntry boxEntry : entry.boxes) {
                CsMusicBox box = boxFromEntry(boxEntry, file);
                if (box != null) {
                    boxes.add(box);
                }
            }
        }
        int priority = entry.priority > 0 ? entry.priority : defaultAreaPriority();
        return new CsMusicArea(id, musicId, entry.activated, priority, List.copyOf(boxes));
    }

    private static int defaultAreaPriority() {
        DimensionalMusicData cfg = DimensionalMusicConfig.data;
        return cfg != null ? Math.max(1, cfg.defaultAreaPriority) : 1;
    }

    private static CsMusicBox boxFromEntry(CsMusicAreaFileData.BoxEntry entry, Path file) {
        if (entry == null || entry.min == null || entry.max == null
                || entry.min.length != 3 || entry.max.length != 3) {
            CobbleSafari.LOGGER.warn("[CSMusic] {} invalid box entry — skipped", file);
            return null;
        }
        return new CsMusicBox(
                Math.min(entry.min[0], entry.max[0]),
                Math.min(entry.min[1], entry.max[1]),
                Math.min(entry.min[2], entry.max[2]),
                Math.max(entry.min[0], entry.max[0]),
                Math.max(entry.min[1], entry.max[1]),
                Math.max(entry.min[2], entry.max[2]));
    }

    private static CsMusicAreaFileData toFileData(Map<String, CsMusicArea> areas) {
        CsMusicAreaFileData data = new CsMusicAreaFileData();
        for (CsMusicArea area : areas.values()) {
            CsMusicAreaFileData.AreaEntry entry = new CsMusicAreaFileData.AreaEntry();
            entry.id = area.id();
            entry.music = area.musicId();
            entry.activated = area.activated();
            entry.priority = area.priority();
            entry.boxes = new ArrayList<>();
            for (CsMusicBox box : area.boxes()) {
                CsMusicAreaFileData.BoxEntry boxEntry = new CsMusicAreaFileData.BoxEntry();
                boxEntry.min = new int[]{box.minX(), box.minY(), box.minZ()};
                boxEntry.max = new int[]{box.maxX(), box.maxY(), box.maxZ()};
                entry.boxes.add(boxEntry);
            }
            data.areas.add(entry);
        }
        return data;
    }
}
