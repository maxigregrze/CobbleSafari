package maxigregrze.cobblesafari.csmusic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The weather where a player stands, as a four-way partition: exactly one value is true at any
 * instant and any position. {@link #RAIN} and {@link #SNOW} mean "and not thundering", so a rule
 * asking for rain does not also fire during a storm.
 *
 * <p>Two different scopes are folded into one enum, deliberately. {@link #THUNDER} is a <b>sky</b>
 * state: a storm covers the whole dimension, dry biomes included, and lightning does strike the
 * desert. {@link #RAIN} and {@link #SNOW} are <b>precipitation</b> states, read from the biome at the
 * player's position, so they answer what actually falls there - nothing, in a desert.</p>
 */
public enum CsMusicWeather {
    /** Nothing falling: clear sky, a dry biome under any weather, or a dimension without weather. */
    NONE,
    /** Rain falling, no thunder. */
    RAIN,
    /** Snow falling, no thunder - a cold biome, or high enough for the height-adjusted temperature. */
    SNOW,
    /** Thunderstorm, anywhere in the dimension. */
    THUNDER;

    /** Parses a json value; returns {@code null} if unknown. */
    @Nullable
    public static CsMusicWeather fromJson(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "none", "clear" -> NONE;
            case "rain", "raining" -> RAIN;
            case "snow", "snowing" -> SNOW;
            case "thunder", "thunderstorm", "storm" -> THUNDER;
            default -> null;
        };
    }

    /**
     * The state at {@code pos}, whose biome holder the caller already resolved (the eval context
     * memoizes it per player and per sweep, so this costs nothing extra).
     *
     * <p>Dimensions with no skylight or with a ceiling have no weather and always report
     * {@link #NONE} - this is vanilla's own predicate for thunder ({@code Level.isThundering}),
     * applied to rain as well, which vanilla omits: {@code Level.isRaining()} would otherwise answer
     * true under a full ceiling, where the client draws nothing.</p>
     */
    public static CsMusicWeather current(ServerLevel level, Holder<Biome> biome, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) {
            return NONE;
        }
        if (level.isThundering()) {
            return THUNDER; // a storm is a sky event: it reaches the desert too
        }
        if (!level.isRaining()) {
            return NONE;
        }
        // Deliberately not Level.isRainingAt: that also demands canSeeSky and a heightmap test, which
        // would cut the track the moment the player steps under a roof.
        return switch (biome.value().getPrecipitationAt(pos)) {
            case RAIN -> RAIN;
            case SNOW -> SNOW;
            case NONE -> NONE;
        };
    }
}
