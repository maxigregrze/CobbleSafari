package maxigregrze.cobblesafari.gts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class GtsUniqueOfferRegistry {
    private static final Map<String, GtsUniqueOfferDefinition> OFFERS = new HashMap<>();

    private GtsUniqueOfferRegistry() {}

    public static void clear() {
        OFFERS.clear();
    }

    public static void register(GtsUniqueOfferDefinition def) {
        OFFERS.put(def.getOfferId(), def);
    }

    public static Optional<GtsUniqueOfferDefinition> get(String offerId) {
        return Optional.ofNullable(OFFERS.get(offerId));
    }

    public static Map<String, GtsUniqueOfferDefinition> getAll() {
        return Collections.unmodifiableMap(OFFERS);
    }
}
