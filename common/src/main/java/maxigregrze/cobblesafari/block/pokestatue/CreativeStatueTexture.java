package maxigregrze.cobblesafari.block.pokestatue;

import net.minecraft.util.StringRepresentable;

/**
 * Texture mode of the Creative Statue, replacing the regular statue's material selector.
 *
 * <p>Unlike {@link PokeStatueMaterial} the colour field has no pedestal to act on here, so it only
 * feeds the model tint - and in {@link #DEFAULT} it does nothing at all.</p>
 */
public enum CreativeStatueTexture implements StringRepresentable {

    /** Cobblemon's own texture, opaque. Colour ignored. */
    DEFAULT("default"),
    /** Greyscale, tinted with the colour when one is set. */
    TINTED("tinted"),
    /** Half transparent: plain texture when no colour is set, greyscale + tint when one is. */
    TRANSPARENT("transparent");

    private static final CreativeStatueTexture[] VALUES = values();

    /** Opacity used by {@link #TRANSPARENT}. */
    private static final float TRANSPARENT_ALPHA = 0.5f;

    private final String name;

    CreativeStatueTexture(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int getId() {
        return this.ordinal();
    }

    public CreativeStatueTexture next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public CreativeStatueTexture prev() {
        return VALUES[(this.ordinal() + VALUES.length - 1) % VALUES.length];
    }

    public static CreativeStatueTexture byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : DEFAULT;
    }

    /**
     * @param color the configured colour, or {@link AbstractPokeStatueBlockEntity#NO_COLOR} when unset.
     */
    public PokeStatueRenderStyle styleFor(int color) {
        boolean hasColor = color != AbstractPokeStatueBlockEntity.NO_COLOR;
        return switch (this) {
            case DEFAULT -> PokeStatueRenderStyle.PLAIN;
            case TINTED -> new PokeStatueRenderStyle(true, hasColor ? color : 0xFFFFFF, 1.0f);
            case TRANSPARENT -> hasColor
                    ? new PokeStatueRenderStyle(true, color, TRANSPARENT_ALPHA)
                    : new PokeStatueRenderStyle(false, 0xFFFFFF, TRANSPARENT_ALPHA);
        };
    }
}
