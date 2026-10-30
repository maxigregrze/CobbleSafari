package maxigregrze.cobblesafari.block.misc;

import net.minecraft.util.StringRepresentable;

/**
 * Visual skin of the Lost Item block. Purely cosmetic: loot mode, claim list and rolls are unaffected.
 *
 * <p>{@link #RUBBLES} is the historical look and the default of the {@code skin} block state property,
 * so every block placed before action plan 176 keeps exactly the appearance it had - including the ones
 * baked into the 19 shipped structure templates, whose palettes carry no {@code skin} key at all.
 */
public enum LostItemSkin implements StringRepresentable {
    RUBBLES("rubbles", "lost_item", "lost_item", true, false),
    CLASSIC("classic", "lost_item_classic", "lost_item_classic", true, false),
    /**
     * The authored block models ship a misspelled file name ({@code lost_item_burried_*}) that is kept
     * as-is rather than renamed. Everything this mod controls - the block state value, the item id and
     * every lang key - spells it correctly. The model prefix is the one place the two diverge.
     */
    BURIED("buried", "lost_item_buried", "lost_item_burried", true, false),
    /** Animated ball and aura drawn by the block entity renderer (action plan 193, section 7). */
    FLOATING("floating", "lost_item_floating", "lost_item_floating", true, false),
    /**
     * Z-A sparkle on the ground (action plan 196, section 7.9): one {@code lost_item_shine} model and no
     * ball, picked up by walking into the block rather than by right-click. Both traits belong to the skin
     * and are not GUI settings.
     */
    SHINE("shine", "lost_item_shine", "lost_item_shine", false, true);

    private final String name;
    private final String itemName;
    private final String modelPrefix;
    private final boolean rendersBall;
    private final boolean claimedByStepping;

    LostItemSkin(String name, String itemName, String modelPrefix, boolean rendersBall, boolean claimedByStepping) {
        this.name = name;
        this.itemName = itemName;
        this.modelPrefix = modelPrefix;
        this.rendersBall = rendersBall;
        this.claimedByStepping = claimedByStepping;
    }

    /** Whether the block entity renderer draws the configured ball; without one, the skin model is the item. */
    public boolean rendersBall() {
        return rendersBall;
    }

    /** Claimed by walking into the block instead of right-clicking it. */
    public boolean claimedByStepping() {
        return claimedByStepping;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Block model path suffix, e.g. {@code lost_item_classic_full}. Follows the authored file names. A skin
     * without a ball has a single model for both states.
     */
    public String modelName(boolean hasItem) {
        if (!rendersBall) {
            return modelPrefix;
        }
        return modelPrefix + (hasItem ? "_full" : "_empty");
    }

    /** Item registry path of this skin's creative block item, e.g. {@code lost_item_classic}. */
    public String itemName() {
        return itemName;
    }

    public LostItemSkin next() {
        LostItemSkin[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public LostItemSkin prev() {
        LostItemSkin[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }

    /** Never trusts the caller: any unknown or blank name falls back to the default skin. */
    public static LostItemSkin byName(String name) {
        if (name == null || name.isBlank()) {
            return RUBBLES;
        }
        for (LostItemSkin skin : values()) {
            if (skin.name.equals(name)) {
                return skin;
            }
        }
        return RUBBLES;
    }
}
