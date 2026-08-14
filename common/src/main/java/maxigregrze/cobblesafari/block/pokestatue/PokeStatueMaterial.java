package maxigregrze.cobblesafari.block.pokestatue;

import net.minecraft.util.StringRepresentable;

/**
 * Finish of the regular Pokémon statue. Stored as an int 0-4 in the block entity; the hologram
 * entry additionally mirrors into the {@code hologram} block state property (light level +
 * pedestal swap).
 *
 * <p>{@link #PAINTED} is deliberately <b>last</b>: the mode is persisted as a raw ordinal, so
 * inserting it anywhere else would silently re-label every statue already placed in a world.</p>
 */
public enum PokeStatueMaterial implements StringRepresentable {

    STONE("stone", true, 0xFFFFFF, 1.0f),
    GOLD("gold", true, 0xF2C705, 1.0f),
    DIAMOND("diamond", true, 0x4AEDD9, 1.0f),
    HOLOGRAM("hologram", true, 0x64FF54, 0.5f),
    /** Cobblemon's own texture, untouched. Emissive layers stay off, as for every finish. */
    PAINTED("painted", false, 0xFFFFFF, 1.0f);

    /** Fallback tint for the hologram finish when the colour field is empty. */
    public static final int HOLOGRAM_DEFAULT_COLOR = 0x64FF54;

    private static final PokeStatueMaterial[] VALUES = values();

    private final String name;
    private final boolean greyscale;
    private final int tint;
    private final float alpha;

    PokeStatueMaterial(String name, boolean greyscale, int tint, float alpha) {
        this.name = name;
        this.greyscale = greyscale;
        this.tint = tint;
        this.alpha = alpha;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** Base tint of the finish; ignored for {@link #HOLOGRAM}, which uses the colour field. */
    public int getTint() {
        return this.tint;
    }

    /** 1 for the opaque finishes, 0.5 for the hologram. */
    public float getAlpha() {
        return this.alpha;
    }

    public boolean isGreyscale() {
        return this.greyscale;
    }

    public boolean isHologram() {
        return this == HOLOGRAM;
    }

    public int getId() {
        return this.ordinal();
    }

    public PokeStatueMaterial next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public PokeStatueMaterial prev() {
        return VALUES[(this.ordinal() + VALUES.length - 1) % VALUES.length];
    }

    public static PokeStatueMaterial byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : STONE;
    }

    /**
     * @param color the configured colour, or {@link AbstractPokeStatueBlockEntity#NO_COLOR} when unset.
     */
    public PokeStatueRenderStyle styleFor(int color) {
        int rgb = isHologram()
                ? (color != AbstractPokeStatueBlockEntity.NO_COLOR ? color : HOLOGRAM_DEFAULT_COLOR)
                : this.tint;
        return new PokeStatueRenderStyle(this.greyscale, rgb, this.alpha);
    }
}
