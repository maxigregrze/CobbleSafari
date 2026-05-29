package maxigregrze.cobblesafari.power;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record GuaranteedShinyRequest(
        String key,
        long triggerGameTime,
        long expiryGameTime,
        ResourceLocation requiredEffectId,
        Integer variantIndex
) {

    private static final String KEY_EXPIRY = "Expiry";
    private static final String KEY_REQUIRED_EFFECT = "RequiredEffect";
    private static final String KEY_VARIANT = "Variant";

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Key", key);
        tag.putLong("Trigger", triggerGameTime);
        tag.putLong(KEY_EXPIRY, expiryGameTime);
        if (requiredEffectId != null) {
            tag.putString(KEY_REQUIRED_EFFECT, requiredEffectId.toString());
        }
        if (variantIndex != null) {
            tag.putInt(KEY_VARIANT, variantIndex);
        }
        return tag;
    }

    public static GuaranteedShinyRequest fromNbt(CompoundTag tag) {
        String key = tag.getString("Key");
        long trigger = tag.getLong("Trigger");
        long expiry = tag.contains(KEY_EXPIRY) ? tag.getLong(KEY_EXPIRY) : -1L;
        ResourceLocation requiredEffectId = tag.contains(KEY_REQUIRED_EFFECT)
                ? ResourceLocation.tryParse(tag.getString(KEY_REQUIRED_EFFECT))
                : null;
        Integer variantIndex = tag.contains(KEY_VARIANT) ? tag.getInt(KEY_VARIANT) : null;
        return new GuaranteedShinyRequest(key, trigger, expiry, requiredEffectId, variantIndex);
    }
}
