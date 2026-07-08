package maxigregrze.cobblesafari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server configuration for global csmusic settings ({@code dimensional_music.json}).
 * Written only on <b>first creation</b>; any manual edit is respected afterward. Where music plays
 * is defined by {@code csmusic/definition/*.json} trigger files, not this config.
 */
public final class DimensionalMusicConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir().resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("dimensional_music.json");

    public static DimensionalMusicData data = new DimensionalMusicData();

    private DimensionalMusicConfig() {}

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            data = new DimensionalMusicData();
            save();
            CobbleSafari.LOGGER.info("CobbleSafari >> Created default dimensional_music.json at {}", CONFIG_PATH);
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            DimensionalMusicData parsed = GSON.fromJson(reader, DimensionalMusicData.class);
            data = parsed != null ? parsed : new DimensionalMusicData();
            CobbleSafari.LOGGER.info("CobbleSafari >> dimensional_music.json loaded from {}", CONFIG_PATH);
        } catch (Exception e) {
            CobbleSafari.LOGGER.error(
                    "CobbleSafari >> Failed to read dimensional_music.json at {} — using in-memory defaults, file not overwritten",
                    CONFIG_PATH, e);
            data = new DimensionalMusicData();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            CobbleSafari.LOGGER.error("CobbleSafari >> Failed to write dimensional_music.json", e);
        }
    }
}
