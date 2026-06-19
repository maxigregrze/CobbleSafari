package maxigregrze.cobblesafari.csmusic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CsMusicAreaStore {

    private static final Map<String, Map<String, CsMusicArea>> BY_DIMENSION = new HashMap<>();
    private static final Set<String> LOADED_DIMENSIONS = new HashSet<>();

    private CsMusicAreaStore() {}

    public static Collection<CsMusicArea> areasIn(ServerLevel level) {
        ensureLoaded(level.getServer(), level);
        Map<String, CsMusicArea> areas = BY_DIMENSION.get(dimensionId(level));
        return areas == null || areas.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableCollection(areas.values());
    }

    @Nullable
    public static CsMusicArea get(ServerLevel level, String areaId) {
        ensureLoaded(level.getServer(), level);
        Map<String, CsMusicArea> areas = BY_DIMENSION.get(dimensionId(level));
        return areas != null ? areas.get(areaId) : null;
    }

    public static void put(ServerLevel level, CsMusicArea area) {
        ensureLoaded(level.getServer(), level);
        BY_DIMENSION.computeIfAbsent(dimensionId(level), k -> new HashMap<>()).put(area.id(), area);
    }

    public static boolean remove(ServerLevel level, String areaId) {
        ensureLoaded(level.getServer(), level);
        Map<String, CsMusicArea> areas = BY_DIMENSION.get(dimensionId(level));
        return areas != null && areas.remove(areaId) != null;
    }

    public static void loadAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            loadOne(server, level);
        }
    }

    public static void loadOne(MinecraftServer server, ServerLevel level) {
        String dimId = dimensionId(level);
        if (LOADED_DIMENSIONS.contains(dimId)) {
            return;
        }
        Map<String, CsMusicArea> target = new HashMap<>();
        if (!CsMusicAreaStorage.loadOne(server, level, target)) {
            return;
        }
        if (target.isEmpty()) {
            BY_DIMENSION.remove(dimId);
        } else {
            BY_DIMENSION.put(dimId, target);
        }
        LOADED_DIMENSIONS.add(dimId);
    }

    public static void reload(MinecraftServer server, ServerLevel level) {
        String dimId = dimensionId(level);
        Map<String, CsMusicArea> target = new HashMap<>();
        if (!CsMusicAreaStorage.loadOne(server, level, target)) {
            return;
        }
        if (target.isEmpty()) {
            BY_DIMENSION.remove(dimId);
        } else {
            BY_DIMENSION.put(dimId, target);
        }
        LOADED_DIMENSIONS.add(dimId);
    }

    public static void save(MinecraftServer server, ServerLevel level) {
        ensureLoaded(server, level);
        Map<String, CsMusicArea> areas = BY_DIMENSION.getOrDefault(dimensionId(level), Map.of());
        CsMusicAreaStorage.save(server, level, areas);
    }

    private static void ensureLoaded(MinecraftServer server, ServerLevel level) {
        if (!LOADED_DIMENSIONS.contains(dimensionId(level))) {
            loadOne(server, level);
        }
    }

    private static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }
}
