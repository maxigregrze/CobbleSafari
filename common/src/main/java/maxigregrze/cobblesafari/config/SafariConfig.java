package maxigregrze.cobblesafari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class SafariConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir()
            .resolve("cobblesafari");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("safari_config.json");

    private static SafariConfig INSTANCE;

    private int dailySafariBallsCount = 16;
    private boolean enableEntryFee = false;
    private String entryFee = "cobblesafari:ticket_safari";
    private boolean cobbledollarEntryFee = false;
    private int entryFeeAmount = 5000;
    private boolean allowPaidReentry = false;
    private int teleporterRadius = 5000;
    private boolean balloonSafariEnabled = true;
    private int balloonSafariMinDrop = 1;
    private int balloonSafariMaxDrop = 4;
    private int balloonSafariItemMinDrop = 4;
    private int balloonSafariItemMaxDrop = 8;
    private double balloonSafariSpawnMultiplier = 1.0;
    private boolean kickOnReset = true;
    private boolean allowGracePeriod = true;
    private int gracePeriodDuration = 300;

    private int baseFleeRate = 60;
    private int fleeGracePeriodTicks = 100;
    private float shinyCatchMultiplier = 8.0f;
    private boolean canShinyFlee = false;
    private int dailyBaitCount = 32;
    private int dailyMudBallCount = 32;

    public SafariConfig() {
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                migrateLegacyAllowPaidReentry(json);
                INSTANCE = GSON.fromJson(json, SafariConfig.class);
                if (INSTANCE == null) {
                    CobbleSafari.LOGGER.warn(
                            "CobbleSafari >> safari_config.json at {} deserialized to null; using defaults",
                            CONFIG_PATH);
                    INSTANCE = new SafariConfig();
                }
                validateAndFixConfig();
                CobbleSafari.LOGGER.info("CobbleSafari >> safari_config.json loaded successfully from {}", CONFIG_PATH);
                CobbleSafari.LOGGER.info("CobbleSafari >> Persisting safari_config.json after load (validated schema)");
                save();
            } catch (IOException e) {
                CobbleSafari.LOGGER.error(
                        "CobbleSafari >> Failed to read safari_config.json at {} (I/O error). Using in-memory defaults; the file on disk was not modified.",
                        CONFIG_PATH,
                        e);
                INSTANCE = new SafariConfig();
            } catch (Exception e) {
                CobbleSafari.LOGGER.error(
                        "CobbleSafari >> Failed to parse safari_config.json at {} (invalid JSON or unexpected structure). Using in-memory defaults; the file on disk was not modified.",
                        CONFIG_PATH,
                        e);
                INSTANCE = new SafariConfig();
            }
        } else {
            CobbleSafari.LOGGER.info(
                    "CobbleSafari >> safari_config.json not found at {}, creating default file",
                    CONFIG_PATH);
            INSTANCE = new SafariConfig();
            CobbleSafari.LOGGER.info("CobbleSafari >> Persisting safari_config.json after load (first-time default file)");
            save();
        }
    }

    private static void migrateLegacyAllowPaidReentry(JsonObject json) {
        if (!json.has("allowMultiplePayment")) {
            return;
        }
        if (!json.has("allowPaidReentry")) {
            json.addProperty("allowPaidReentry", json.get("allowMultiplePayment").getAsBoolean());
        }
        json.remove("allowMultiplePayment");
    }

    private static void validateAndFixConfig() {
        if (INSTANCE == null) return;

        if (INSTANCE.entryFee != null && !isValidItemId(INSTANCE.entryFee)) {
            CobbleSafari.LOGGER.warn("Invalid entry fee item ID '{}', resetting to default 'minecraft:diamond'", INSTANCE.entryFee);
            INSTANCE.entryFee = "minecraft:diamond";
        }
    }

    private static boolean isValidItemId(String itemId) {
        try {
            ResourceLocation location = ResourceLocation.parse(itemId);
            return BuiltInRegistries.ITEM.containsKey(location);
        } catch (Exception e) {
            return false;
        }
    }

    public static void save() {
        if (INSTANCE == null) {
            INSTANCE = new SafariConfig();
        }
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to create config directory", e);
        }
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
            CobbleSafari.LOGGER.info("CobbleSafari >> safari_config.json written to {}", CONFIG_PATH);
        } catch (IOException e) {
            CobbleSafari.LOGGER.error("Failed to save safari config", e);
        }
    }

    public static int getDailySafariBallsCount() {
        if (INSTANCE == null) return 16;
        return INSTANCE.dailySafariBallsCount;
    }

    public static boolean isEntryFeeEnabled() {
        if (INSTANCE == null) return false;
        return INSTANCE.enableEntryFee;
    }

    public static String getEntryFeeItem() {
        if (INSTANCE == null) return "cobblesafari:ticket_safari";
        return INSTANCE.entryFee != null ? INSTANCE.entryFee : "cobblesafari:ticket_safari";
    }

    public static boolean isCobbledollarFeeEnabled() {
        if (INSTANCE == null) return false;
        return INSTANCE.cobbledollarEntryFee;
    }

    public static int getEntryFeeAmount() {
        if (INSTANCE == null) return 5000;
        return INSTANCE.entryFeeAmount > 0 ? INSTANCE.entryFeeAmount : 5000;
    }

    public static boolean isAllowPaidReentry() {
        if (INSTANCE == null) return false;
        return INSTANCE.allowPaidReentry;
    }

    public static int getTeleporterRadius() {
        if (INSTANCE == null) return 5000;
        return Math.max(1, INSTANCE.teleporterRadius);
    }

    public static boolean isBalloonSafariEnabled() {
        if (INSTANCE == null) return true;
        return INSTANCE.balloonSafariEnabled;
    }

    public static int getBalloonSafariMinDrop() {
        if (INSTANCE == null) return 1;
        return Math.max(1, INSTANCE.balloonSafariMinDrop);
    }

    public static int getBalloonSafariMaxDrop() {
        if (INSTANCE == null) return 4;
        return Math.max(getBalloonSafariMinDrop(), INSTANCE.balloonSafariMaxDrop);
    }

    public static int getBalloonSafariItemMinDrop() {
        if (INSTANCE == null) return 4;
        return Math.max(0, INSTANCE.balloonSafariItemMinDrop);
    }

    public static int getBalloonSafariItemMaxDrop() {
        if (INSTANCE == null) return 8;
        return Math.max(getBalloonSafariItemMinDrop(), INSTANCE.balloonSafariItemMaxDrop);
    }

    public static double getBalloonSafariSpawnMultiplier() {
        if (INSTANCE == null) return 1.0;
        return Math.max(0.0, INSTANCE.balloonSafariSpawnMultiplier);
    }

    public static boolean isKickOnReset() {
        if (INSTANCE == null) return true;
        return INSTANCE.kickOnReset;
    }

    public static boolean isAllowGracePeriod() {
        if (INSTANCE == null) return true;
        return INSTANCE.allowGracePeriod;
    }

    public static int getGracePeriodDuration() {
        if (INSTANCE == null) return 300;
        return Math.max(0, INSTANCE.gracePeriodDuration);
    }

    public static int getBaseFleeRate() {
        if (INSTANCE == null) return 60;
        return Math.clamp(INSTANCE.baseFleeRate, 1, 254);
    }

    public static int getFleeGracePeriodTicks() {
        if (INSTANCE == null) return 100;
        return Math.max(20, INSTANCE.fleeGracePeriodTicks);
    }

    public static int getMaxMoodLevel() {
        return 6;
    }

    public static float getShinyCatchMultiplier() {
        if (INSTANCE == null) return 8.0f;
        return Math.max(1.0f, INSTANCE.shinyCatchMultiplier);
    }

    public static boolean canShinyFlee() {
        if (INSTANCE == null) return false;
        return INSTANCE.canShinyFlee;
    }

    public static int getDailyBaitCount() {
        if (INSTANCE == null) return 32;
        return Math.max(0, INSTANCE.dailyBaitCount);
    }

    public static int getDailyMudBallCount() {
        if (INSTANCE == null) return 32;
        return Math.max(0, INSTANCE.dailyMudBallCount);
    }
}
