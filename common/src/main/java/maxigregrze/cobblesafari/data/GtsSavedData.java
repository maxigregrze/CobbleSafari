package maxigregrze.cobblesafari.data;

import maxigregrze.cobblesafari.CobbleSafari;
import maxigregrze.cobblesafari.gts.GtsOffer;
import maxigregrze.cobblesafari.gts.GtsSuccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GtsSavedData extends SavedData {
    private static final String DATA_NAME = CobbleSafari.MOD_ID + "_gts";

    private int nextOfferId = 1;
    private int nextSuccessId = 1;
    private long lastDailyResetEpochDay = -1L;

    private final List<GtsOffer> offers = new ArrayList<>();
    private final List<GtsSuccess> successes = new ArrayList<>();

    public GtsSavedData() {}

    public static GtsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GtsSavedData data = new GtsSavedData();
        if (tag.contains("NextOfferId")) {
            data.nextOfferId = Math.max(1, tag.getInt("NextOfferId"));
        }
        if (tag.contains("NextSuccessId")) {
            data.nextSuccessId = Math.max(1, tag.getInt("NextSuccessId"));
        }
        if (tag.contains("LastDailyEpochDay")) {
            data.lastDailyResetEpochDay = tag.getLong("LastDailyEpochDay");
        }
        if (tag.contains("Offers", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Offers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                data.offers.add(GtsOffer.fromNbt(list.getCompound(i)));
            }
        }
        if (tag.contains("Successes", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Successes", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                data.successes.add(GtsSuccess.fromNbt(list.getCompound(i)));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("NextOfferId", nextOfferId);
        tag.putInt("NextSuccessId", nextSuccessId);
        tag.putLong("LastDailyEpochDay", lastDailyResetEpochDay);
        ListTag offerList = new ListTag();
        for (GtsOffer o : offers) {
            offerList.add(o.toNbt());
        }
        tag.put("Offers", offerList);
        ListTag succList = new ListTag();
        for (GtsSuccess s : successes) {
            succList.add(s.toNbt());
        }
        tag.put("Successes", succList);
        return tag;
    }

    public static GtsSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(new Factory<>(GtsSavedData::new, GtsSavedData::load, null), DATA_NAME);
    }

    public int allocateOfferId() {
        int id = nextOfferId++;
        setDirty();
        return id;
    }

    public int allocateSuccessId() {
        int id = nextSuccessId++;
        setDirty();
        return id;
    }

    public List<GtsOffer> getOffers() {
        return offers;
    }

    public List<GtsSuccess> getSuccesses() {
        return successes;
    }

    public void addOffer(GtsOffer o) {
        offers.add(o);
        setDirty();
    }

    public boolean removeOffer(int id) {
        boolean r = offers.removeIf(o -> o.getId() == id);
        if (r) {
            setDirty();
        }
        return r;
    }

    public Optional<GtsOffer> findOffer(int id) {
        return offers.stream().filter(o -> o.getId() == id).findFirst();
    }

    public List<GtsOffer> findOffersByDepositor(UUID u) {
        return offers.stream().filter(o -> u.equals(o.getDepositorUuid())).toList();
    }

    public void addSuccess(GtsSuccess s) {
        successes.add(s);
        setDirty();
    }

    public boolean removeSuccess(int id) {
        boolean r = successes.removeIf(s -> s.getId() == id);
        if (r) {
            setDirty();
        }
        return r;
    }

    public Optional<GtsSuccess> findSuccess(int id) {
        return successes.stream().filter(s -> s.getId() == id).findFirst();
    }

    public List<GtsSuccess> findSuccessesByRecipient(UUID u) {
        return successes.stream().filter(s -> u.equals(s.getRecipientUuid())).toList();
    }

    public long getLastDailyResetEpochDay() {
        return lastDailyResetEpochDay;
    }

    public void setLastDailyResetEpochDay(long lastDailyResetEpochDay) {
        this.lastDailyResetEpochDay = lastDailyResetEpochDay;
        setDirty();
    }
}
