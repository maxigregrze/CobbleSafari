package maxigregrze.cobblesafari.gts;

import com.cobblemon.mod.common.pokemon.Gender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Locale;
import java.util.UUID;

public final class GtsOffer {

    public enum ShinyWish {
        ANY,
        SHINY,
        NOT_SHINY;

        public static ShinyWish parse(String s) {
            if (s == null || s.isEmpty()) {
                return ANY;
            }
            return switch (s.toLowerCase(Locale.ROOT)) {
                case "shiny", "yes", "true" -> SHINY;
                case "not_shiny", "notshiny", "no", "false" -> NOT_SHINY;
                default -> ANY;
            };
        }

        public ShinyWish next() {
            return switch (this) {
                case ANY -> SHINY;
                case SHINY -> NOT_SHINY;
                case NOT_SHINY -> ANY;
            };
        }
    }

    /**
     * Provenance of a personal offer (B3). {@code MESSAGE} = earned through gameplay (chat rewards) —
     * never expires, never capped. {@code ADMIN} = injected by a command / external quest API — subject to
     * a configurable per-player cap. Defaults to {@code MESSAGE} for backward compatibility.
     */
    public enum PersonalSource {
        MESSAGE,
        ADMIN
    }

    private final int id;
    private final UUID depositorUuid;
    private CompoundTag pokemonData;
    private final String wishSpecies;
    private final int wishLevelBucket;
    private final GenderFilter wishGender;
    private final ShinyWish wishShiny;
    private final String depositedSpeciesPath;
    private final Gender depositedGender;
    private final boolean depositedShiny;
    private final boolean uniqueOffer;
    private final String uniqueOfferTemplateId;
    private final UUID personalTargetUuid;
    private PersonalSource personalSource = PersonalSource.MESSAGE;
    private int age;
    private boolean locked;
    private UUID lockOwnerUuid;
    private long lockExpireEpochMs;

    public GtsOffer(
            int id,
            UUID depositorUuid,
            CompoundTag pokemonData,
            String wishSpecies,
            int wishLevelBucket,
            GenderFilter wishGender,
            ShinyWish wishShiny,
            String depositedSpeciesPath,
            Gender depositedGender,
            boolean depositedShiny) {
        this(
                id,
                depositorUuid,
                pokemonData,
                wishSpecies,
                wishLevelBucket,
                wishGender,
                wishShiny,
                depositedSpeciesPath,
                depositedGender,
                depositedShiny,
                false,
                "");
    }

    public GtsOffer(
            int id,
            UUID depositorUuid,
            CompoundTag pokemonData,
            String wishSpecies,
            int wishLevelBucket,
            GenderFilter wishGender,
            ShinyWish wishShiny,
            String depositedSpeciesPath,
            Gender depositedGender,
            boolean depositedShiny,
            boolean uniqueOffer,
            String uniqueOfferTemplateId) {
        this(
                id,
                depositorUuid,
                pokemonData,
                wishSpecies,
                wishLevelBucket,
                wishGender,
                wishShiny,
                depositedSpeciesPath,
                depositedGender,
                depositedShiny,
                uniqueOffer,
                uniqueOfferTemplateId,
                null);
    }

    public GtsOffer(
            int id,
            UUID depositorUuid,
            CompoundTag pokemonData,
            String wishSpecies,
            int wishLevelBucket,
            GenderFilter wishGender,
            ShinyWish wishShiny,
            String depositedSpeciesPath,
            Gender depositedGender,
            boolean depositedShiny,
            boolean uniqueOffer,
            String uniqueOfferTemplateId,
            UUID personalTargetUuid) {
        this.id = id;
        this.depositorUuid = depositorUuid;
        this.pokemonData = pokemonData.copy();
        this.wishSpecies = wishSpecies;
        this.wishLevelBucket = wishLevelBucket;
        this.wishGender = wishGender;
        this.wishShiny = wishShiny == null ? ShinyWish.ANY : wishShiny;
        this.depositedSpeciesPath = depositedSpeciesPath;
        this.depositedGender = depositedGender;
        this.depositedShiny = depositedShiny;
        this.uniqueOffer = uniqueOffer;
        this.uniqueOfferTemplateId = uniqueOfferTemplateId == null ? "" : uniqueOfferTemplateId;
        this.personalTargetUuid = personalTargetUuid;
        this.age = 0;
        this.locked = false;
        this.lockOwnerUuid = null;
        this.lockExpireEpochMs = 0L;
    }

    public int getId() {
        return id;
    }

    public UUID getDepositorUuid() {
        return depositorUuid;
    }

    public CompoundTag getPokemonData() {
        return pokemonData;
    }

    public String getWishSpecies() {
        return wishSpecies;
    }

    public int getWishLevelBucket() {
        return wishLevelBucket;
    }

    public GenderFilter getWishGender() {
        return wishGender;
    }

    public ShinyWish getWishShiny() {
        return wishShiny;
    }

    public String getDepositedSpeciesPath() {
        return depositedSpeciesPath;
    }

    public Gender getDepositedGender() {
        return depositedGender;
    }

    public boolean isDepositedShiny() {
        return depositedShiny;
    }

    public boolean isUniqueOffer() {
        return uniqueOffer;
    }

    public String getUniqueOfferTemplateId() {
        return uniqueOfferTemplateId;
    }

    public boolean isPersonalOffer() {
        return personalTargetUuid != null;
    }

    public UUID getPersonalTargetUuid() {
        return personalTargetUuid;
    }

    public PersonalSource getPersonalSource() {
        return personalSource;
    }

    public void setPersonalSource(PersonalSource personalSource) {
        this.personalSource = personalSource == null ? PersonalSource.MESSAGE : personalSource;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public UUID getLockOwnerUuid() {
        return lockOwnerUuid;
    }

    public void setLockOwnerUuid(UUID lockOwnerUuid) {
        this.lockOwnerUuid = lockOwnerUuid;
    }

    public long getLockExpireEpochMs() {
        return lockExpireEpochMs;
    }

    public void setLockExpireEpochMs(long lockExpireEpochMs) {
        this.lockExpireEpochMs = lockExpireEpochMs;
    }

    public void clearLock() {
        this.locked = false;
        this.lockOwnerUuid = null;
        this.lockExpireEpochMs = 0L;
    }

    private static final String KEY_WISH_GENDER = "WishGender";
    private static final String KEY_WISH_SHINY = "WishShiny";
    private static final String KEY_LOCKED = "Locked";
    private static final String KEY_LOCK_OWNER = "LockOwner";
    private static final String KEY_LOCK_EXPIRE = "LockExpire";
    private static final String KEY_DEP_SPECIES_PATH = "DepSpeciesPath";
    private static final String KEY_UNIQUE_OFFER = "UniqueOffer";
    private static final String KEY_UNIQUE_OFFER_TEMPLATE_ID = "UniqueOfferTemplateId";
    private static final String KEY_PERSONAL_TARGET = "PersonalTarget";
    private static final String KEY_PERSONAL_SOURCE = "PersonalSource";

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putUUID("Depositor", depositorUuid);
        tag.put("Pokemon", pokemonData.copy());
        tag.putString("WishSpecies", wishSpecies);
        tag.putInt("WishLevelBucket", wishLevelBucket);
        tag.putString(KEY_WISH_GENDER, wishGender.name());
        tag.putString(KEY_WISH_SHINY, wishShiny.name());
        tag.putInt("Age", age);
        tag.putBoolean(KEY_LOCKED, locked);
        if (lockOwnerUuid != null) {
            tag.putUUID(KEY_LOCK_OWNER, lockOwnerUuid);
        }
        if (lockExpireEpochMs != 0L) {
            tag.putLong(KEY_LOCK_EXPIRE, lockExpireEpochMs);
        }
        tag.putString(KEY_DEP_SPECIES_PATH, depositedSpeciesPath);
        tag.putString("DepGender", depositedGender.name());
        tag.putBoolean("DepShiny", depositedShiny);
        tag.putBoolean(KEY_UNIQUE_OFFER, uniqueOffer);
        if (uniqueOffer && uniqueOfferTemplateId != null && !uniqueOfferTemplateId.isEmpty()) {
            tag.putString(KEY_UNIQUE_OFFER_TEMPLATE_ID, uniqueOfferTemplateId);
        }
        if (personalTargetUuid != null) {
            tag.putUUID(KEY_PERSONAL_TARGET, personalTargetUuid);
            // Only ADMIN needs persisting; MESSAGE is the backward-compatible default (B3).
            if (personalSource == PersonalSource.ADMIN) {
                tag.putString(KEY_PERSONAL_SOURCE, PersonalSource.ADMIN.name());
            }
        }
        return tag;
    }

    public static GtsOffer fromNbt(CompoundTag tag) {
        int id = tag.getInt("Id");
        UUID dep = tag.getUUID("Depositor");
        CompoundTag p = tag.getCompound("Pokemon");
        String wish = tag.getString("WishSpecies");
        int bucket = tag.getInt("WishLevelBucket");
        GenderFilter gf = GenderFilter.ANY;
        if (tag.contains(KEY_WISH_GENDER, Tag.TAG_STRING)) {
            try {
                gf = GenderFilter.valueOf(tag.getString(KEY_WISH_GENDER));
            } catch (IllegalArgumentException ignored) {
                // Unknown gender filter value; keep the ANY default.
            }
        }
        ShinyWish ws = ShinyWish.ANY;
        if (tag.contains(KEY_WISH_SHINY, Tag.TAG_STRING)) {
            try {
                ws = ShinyWish.valueOf(tag.getString(KEY_WISH_SHINY));
            } catch (IllegalArgumentException ignored) {
                // Unknown shiny wish value; keep the ANY default.
            }
        }

        String depPath;
        Gender depGender;
        boolean depShiny;
        if (tag.contains(KEY_DEP_SPECIES_PATH, Tag.TAG_STRING)) {
            depPath = tag.getString(KEY_DEP_SPECIES_PATH);
            depGender = parseGenderSafe(tag.getString("DepGender"));
            depShiny = tag.getBoolean("DepShiny");
        } else {
            depPath = extractSpeciesPathFromPokemonNbt(p);
            depGender = extractGenderFromPokemonNbt(p);
            depShiny = p.getBoolean("Shiny");
        }

        boolean unique = tag.contains(KEY_UNIQUE_OFFER, Tag.TAG_BYTE) && tag.getBoolean(KEY_UNIQUE_OFFER);
        String uniqueTemplateId =
                tag.contains(KEY_UNIQUE_OFFER_TEMPLATE_ID, Tag.TAG_STRING) ? tag.getString(KEY_UNIQUE_OFFER_TEMPLATE_ID) : "";
        UUID personalTarget = tag.hasUUID(KEY_PERSONAL_TARGET) ? tag.getUUID(KEY_PERSONAL_TARGET) : null;
        GtsOffer o =
                new GtsOffer(
                        id, dep, p, wish, bucket, gf, ws, depPath, depGender, depShiny, unique, uniqueTemplateId,
                        personalTarget);
        o.age = tag.getInt("Age");
        if (tag.contains(KEY_PERSONAL_SOURCE, Tag.TAG_STRING)
                && PersonalSource.ADMIN.name().equals(tag.getString(KEY_PERSONAL_SOURCE))) {
            o.personalSource = PersonalSource.ADMIN;
        }
        o.locked = tag.contains(KEY_LOCKED, Tag.TAG_BYTE) && tag.getBoolean(KEY_LOCKED);
        if (tag.hasUUID(KEY_LOCK_OWNER)) {
            o.lockOwnerUuid = tag.getUUID(KEY_LOCK_OWNER);
        }
        if (tag.contains(KEY_LOCK_EXPIRE, Tag.TAG_LONG)) {
            o.lockExpireEpochMs = tag.getLong(KEY_LOCK_EXPIRE);
        }
        return o;
    }

    private static String extractSpeciesPathFromPokemonNbt(CompoundTag pokemonNbt) {
        if (pokemonNbt.contains("Species", Tag.TAG_STRING)) {
            String s = pokemonNbt.getString("Species");
            int colon = s.indexOf(':');
            return (colon >= 0 ? s.substring(colon + 1) : s).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static Gender extractGenderFromPokemonNbt(CompoundTag pokemonNbt) {
        if (pokemonNbt.contains("Gender", Tag.TAG_STRING)) {
            return parseGenderSafe(pokemonNbt.getString("Gender"));
        }
        return Gender.GENDERLESS;
    }

    private static Gender parseGenderSafe(String s) {
        if (s == null || s.isEmpty()) {
            return Gender.GENDERLESS;
        }
        try {
            return Gender.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Gender.GENDERLESS;
        }
    }
}
