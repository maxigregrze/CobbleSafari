package maxigregrze.cobblesafari.gts;

import java.util.UUID;

public record GtsTradeCandidate(CandidateSource source, int partySlot, int pcBox, int pcSlot, UUID pokemonUuid) {
    public enum CandidateSource {
        PARTY,
        PC
    }
}
