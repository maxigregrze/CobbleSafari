package maxigregrze.cobblesafari.wondertrade;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class WonderTradeEventRegistry {
    private static final Map<String, WonderTradeEventDefinition> EVENTS = new HashMap<>();

    private WonderTradeEventRegistry() {}

    public static void clear() {
        EVENTS.clear();
    }

    public static void register(WonderTradeEventDefinition def) {
        EVENTS.put(def.getEventId(), def);
    }

    public static Optional<WonderTradeEventDefinition> get(String eventId) {
        return Optional.ofNullable(EVENTS.get(eventId));
    }

    public static Set<String> getEventIds() {
        return Collections.unmodifiableSet(EVENTS.keySet());
    }
}
