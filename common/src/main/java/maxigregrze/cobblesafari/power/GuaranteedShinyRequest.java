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

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Key", key);
        tag.putLong("Trigger", triggerGameTime);
        tag.putLong("Expiry", expiryGameTime);
        if (requiredEffectId != null) {
            tag.putString("RequiredEffect", requiredEffectId.toString());
        }
        if (variantIndex != null) {
            tag.putInt("Variant", variantIndex);
        }
        return tag;
    }

    public static GuaranteedShinyRequest fromNbt(CompoundTag tag) {
        String key = tag.getString("Key");
        long trigger = tag.getLong("Trigger");
        long expiry = tag.contains("Expiry") ? tag.getLong("Expiry") : -1L;
        ResourceLocation requiredEffectId = tag.contains("RequiredEffect")
                ? ResourceLocation.tryParse(tag.getString("RequiredEffect"))
                : null;
        Integer variantIndex = tag.contains("Variant") ? tag.getInt("Variant") : null;
        return new GuaranteedShinyRequest(key, trigger, expiry, requiredEffectId, variantIndex);
    }
}
