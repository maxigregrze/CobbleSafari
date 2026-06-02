package maxigregrze.cobblesafari.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO de {@code dimensional_music.json} (plan 105 § 3).
 * {@code enabled} : toggle global du csmusic. {@code dimensions} : dimensionId → csmusicId.
 */
public class DimensionalMusicData {

    public boolean enabled = true;
    public Map<String, String> dimensions = new LinkedHashMap<>();

    public DimensionalMusicData() {
        // Défauts écrits uniquement à la 1re création (cf. DimensionalMusicConfig.load).
        dimensions.put("cobblesafari:dungeon_underground", "cobblesafari:underground");
        dimensions.put("cobblesafari:dungeon_distortion", "cobblesafari:distortion");
    }
}
