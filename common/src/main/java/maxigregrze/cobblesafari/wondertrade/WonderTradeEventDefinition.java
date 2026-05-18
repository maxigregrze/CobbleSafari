package maxigregrze.cobblesafari.wondertrade;

import maxigregrze.cobblesafari.config.WonderTradeSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WonderTradeEventDefinition {
    private final String eventId;
    private final String eventName;
    private final boolean hasCustomBanner;
    private final String customBannerName;
    private final List<WonderTradeSettings.WeightedPoolEntry> eventPools;

    public WonderTradeEventDefinition(
            String eventId,
            String eventName,
            boolean hasCustomBanner,
            String customBannerName,
            List<WonderTradeSettings.WeightedPoolEntry> eventPools) {
        this.eventId = eventId;
        this.eventName = eventName == null || eventName.isEmpty() ? eventId : eventName;
        this.hasCustomBanner = hasCustomBanner;
        this.customBannerName = customBannerName == null ? "" : customBannerName.toLowerCase().replace(" ", "");
        this.eventPools = Collections.unmodifiableList(new ArrayList<>(eventPools));
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public boolean isHasCustomBanner() {
        return hasCustomBanner;
    }

    public String getCustomBannerName() {
        return customBannerName;
    }

    public List<WonderTradeSettings.WeightedPoolEntry> getEventPools() {
        return eventPools;
    }
}
