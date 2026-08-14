package maxigregrze.cobblesafari.block.pokestatue;

/**
 * How a statue's Pokémon model should be drawn, resolved from the block's mode plus its colour.
 * Pure data so the two statue blocks can describe themselves to one shared renderer.
 *
 * @param greyscale whether the texture is desaturated before the tint is multiplied over it.
 * @param rgb       multiply tint; {@code 0xFFFFFF} leaves the texture untouched.
 * @param alpha     1 for opaque, below 1 to draw translucent.
 */
public record PokeStatueRenderStyle(boolean greyscale, int rgb, float alpha) {

    /** Untouched Cobblemon texture, fully opaque. */
    public static final PokeStatueRenderStyle PLAIN = new PokeStatueRenderStyle(false, 0xFFFFFF, 1.0f);

    public boolean translucent() {
        return alpha < 1.0f;
    }
}
