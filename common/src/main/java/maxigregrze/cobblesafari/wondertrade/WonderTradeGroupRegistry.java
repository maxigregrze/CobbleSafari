package maxigregrze.cobblesafari.wondertrade;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class WonderTradeGroupRegistry {
    private static final Map<String, PokemonGroupDefinition> GROUPS = new HashMap<>();

    private WonderTradeGroupRegistry() {}

    public static void clear() {
        GROUPS.clear();
    }

    public static void register(PokemonGroupDefinition def) {
        GROUPS.put(def.getGroupId(), def);
    }

    public static Optional<PokemonGroupDefinition> get(String groupId) {
        return Optional.ofNullable(GROUPS.get(groupId));
    }

    public static Map<String, PokemonGroupDefinition> getAll() {
        return Collections.unmodifiableMap(GROUPS);
    }
}
