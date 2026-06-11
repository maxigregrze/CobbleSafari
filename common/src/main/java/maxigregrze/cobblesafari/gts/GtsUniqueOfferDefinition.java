package maxigregrze.cobblesafari.gts;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class GtsUniqueOfferDefinition {
    private final String offerId;
    private final String givenLine;
    private final List<ResourceLocation> givenMarkIds;
    private final ResourceLocation givenActiveMarkId;
    private final String wishSpeciesLine;
    private final int wishLevelBucket;
    private final GenderFilter wishGender;
    private final GtsOffer.ShinyWish wishShiny;
    private final String sourcePath;
    private final List<String> tags;

    public GtsUniqueOfferDefinition(
            String offerId,
            String givenLine,
            List<ResourceLocation> givenMarkIds,
            ResourceLocation givenActiveMarkId,
            String wishSpeciesLine,
            int wishLevelBucket,
            GenderFilter wishGender,
            GtsOffer.ShinyWish wishShiny,
            String sourcePath,
            List<String> tags) {
        this.offerId = offerId;
        this.givenLine = givenLine;
        this.givenMarkIds = Collections.unmodifiableList(givenMarkIds);
        this.givenActiveMarkId = givenActiveMarkId;
        this.wishSpeciesLine = wishSpeciesLine;
        this.wishLevelBucket = wishLevelBucket;
        this.wishGender = wishGender;
        this.wishShiny = wishShiny == null ? GtsOffer.ShinyWish.ANY : wishShiny;
        this.sourcePath = sourcePath == null ? "" : sourcePath;
        this.tags =
                tags == null
                        ? List.of()
                        : Collections.unmodifiableList(new ArrayList<>(tags));
    }

    public String getOfferId() {
        return offerId;
    }

    public String getGivenLine() {
        return givenLine;
    }

    public List<ResourceLocation> getGivenMarkIds() {
        return givenMarkIds;
    }

    public ResourceLocation getGivenActiveMarkId() {
        return givenActiveMarkId;
    }

    public String getWishSpeciesLine() {
        return wishSpeciesLine;
    }

    public int getWishLevelBucket() {
        return wishLevelBucket;
    }

    public GenderFilter getWishGender() {
        return wishGender;
    }

    public GtsOffer.ShinyWish getWishShiny() {
        return wishShiny;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean hasTag(String tag) {
        return tag != null && tags.contains(tag.toLowerCase(Locale.ROOT));
    }
}
