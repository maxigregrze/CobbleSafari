package maxigregrze.cobblesafari.gts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

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

    /** All distinct tags present on at least one definition (sorted). */
    public static Set<String> getAllTags() {
        TreeSet<String> out = new TreeSet<>();
        for (GtsUniqueOfferDefinition def : OFFERS.values()) {
            out.addAll(def.getTags());
        }
        return out;
    }

    /** All definitions carrying the given tag (normalized). */
    public static List<GtsUniqueOfferDefinition> getByTag(String tag) {
        String norm = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
        List<GtsUniqueOfferDefinition> out = new ArrayList<>();
        if (norm.isEmpty()) {
            return out;
        }
        for (GtsUniqueOfferDefinition def : OFFERS.values()) {
            if (def.getTags().contains(norm)) {
                out.add(def);
            }
        }
        return out;
    }
}
