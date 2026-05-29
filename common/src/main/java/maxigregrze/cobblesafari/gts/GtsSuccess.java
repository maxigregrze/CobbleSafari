package maxigregrze.cobblesafari.gts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

public final class GtsSuccess {
    public enum Reason {
        TRADED,
        EXPIRED,
        ADMIN_REMOVED
    }

    private final int id;
    private final UUID recipientUuid;
    private CompoundTag pokemonData;
    private final Reason reason;
    private boolean notified;

    public GtsSuccess(int id, UUID recipientUuid, CompoundTag pokemonData, Reason reason) {
        this.id = id;
        this.recipientUuid = recipientUuid;
        this.pokemonData = pokemonData.copy();
        this.reason = reason;
        this.notified = false;
    }

    public int getId() {
        return id;
    }

    public UUID getRecipientUuid() {
        return recipientUuid;
    }

    public CompoundTag getPokemonData() {
        return pokemonData;
    }

    public Reason getReason() {
        return reason;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    private static final String KEY_REASON = "Reason";
    private static final String KEY_NOTIFIED = "Notified";

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putUUID("Recipient", recipientUuid);
        tag.put("Pokemon", pokemonData.copy());
        tag.putString(KEY_REASON, reason.name());
        tag.putBoolean(KEY_NOTIFIED, notified);
        return tag;
    }

    public static GtsSuccess fromNbt(CompoundTag tag) {
        int id = tag.getInt("Id");
        UUID recipient = tag.getUUID("Recipient");
        CompoundTag p = tag.getCompound("Pokemon");
        Reason reason = Reason.TRADED;
        if (tag.contains(KEY_REASON, Tag.TAG_STRING)) {
            try {
                reason = Reason.valueOf(tag.getString(KEY_REASON));
            } catch (IllegalArgumentException ignored) {
                // Unknown reason value; keep the TRADED default.
            }
        }
        GtsSuccess s = new GtsSuccess(id, recipient, p, reason);
        if (tag.contains(KEY_NOTIFIED, Tag.TAG_BYTE)) {
            s.notified = tag.getBoolean(KEY_NOTIFIED);
        }
        return s;
    }
}
