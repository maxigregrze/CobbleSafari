package maxigregrze.cobblesafari.gts;

import java.util.List;

public final class GtsPendingTrade {
    private final int offerId;
    private final List<GtsTradeCandidate> candidates;
    private final long expireDeadlineMs;

    public GtsPendingTrade(int offerId, List<GtsTradeCandidate> candidates, long expireDeadlineMs) {
        this.offerId = offerId;
        this.candidates = List.copyOf(candidates);
        this.expireDeadlineMs = expireDeadlineMs;
    }

    public int getOfferId() {
        return offerId;
    }

    public List<GtsTradeCandidate> getCandidates() {
        return candidates;
    }

    public long getExpireDeadlineMs() {
        return expireDeadlineMs;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireDeadlineMs;
    }
}
