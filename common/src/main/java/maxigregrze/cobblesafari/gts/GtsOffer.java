package maxigregrze.cobblesafari.gts;

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

    private final int id;
    private final UUID depositorUuid;
    private CompoundTag pokemonData;
    private final String wishSpecies;
    private final int wishLevelBucket;
    private final GenderFilter wishGender;
    private final ShinyWish wishShiny;
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
            ShinyWish wishShiny) {
        this.id = id;
        this.depositorUuid = depositorUuid;
        this.pokemonData = pokemonData.copy();
        this.wishSpecies = wishSpecies;
        this.wishLevelBucket = wishLevelBucket;
        this.wishGender = wishGender;
        this.wishShiny = wishShiny == null ? ShinyWish.ANY : wishShiny;
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

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putUUID("Depositor", depositorUuid);
        tag.put("Pokemon", pokemonData.copy());
        tag.putString("WishSpecies", wishSpecies);
        tag.putInt("WishLevelBucket", wishLevelBucket);
        tag.putString("WishGender", wishGender.name());
        tag.putString("WishShiny", wishShiny.name());
        tag.putInt("Age", age);
        tag.putBoolean("Locked", locked);
        if (lockOwnerUuid != null) {
            tag.putUUID("LockOwner", lockOwnerUuid);
        }
        if (lockExpireEpochMs != 0L) {
            tag.putLong("LockExpire", lockExpireEpochMs);
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
        if (tag.contains("WishGender", Tag.TAG_STRING)) {
            try {
                gf = GenderFilter.valueOf(tag.getString("WishGender"));
            } catch (IllegalArgumentException ignored) {
            }
        }
        ShinyWish ws = ShinyWish.ANY;
        if (tag.contains("WishShiny", Tag.TAG_STRING)) {
            try {
                ws = ShinyWish.valueOf(tag.getString("WishShiny"));
            } catch (IllegalArgumentException ignored) {
            }
        }
        GtsOffer o = new GtsOffer(id, dep, p, wish, bucket, gf, ws);
        o.age = tag.getInt("Age");
        o.locked = tag.contains("Locked", Tag.TAG_BYTE) && tag.getBoolean("Locked");
        if (tag.hasUUID("LockOwner")) {
            o.lockOwnerUuid = tag.getUUID("LockOwner");
        }
        if (tag.contains("LockExpire", Tag.TAG_LONG)) {
            o.lockExpireEpochMs = tag.getLong("LockExpire");
        }
        return o;
    }
}
