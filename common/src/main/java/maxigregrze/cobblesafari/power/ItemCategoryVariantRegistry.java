package maxigregrze.cobblesafari.power;

/**
 * Variant dimension for the {@code item} donut power. Unlike {@link PowerVariantRegistry}
 * (indexed on the 18 elemental types + "all"), the {@code item} power is typed by the
 * <b>item category</b> of the reward pools. The indices here <b>must</b> match the order
 * used by the reward block entities' {@code getPoolIdForCategory(int)} :
 * {@code 0=berry, 1=candy, 2=balls, 3=treasures}.
 */
public final class ItemCategoryVariantRegistry {

    public static final int COUNT = 4;

    private static final String[] SUFFIXES = {"berry", "candy", "balls", "treasures"};

    private ItemCategoryVariantRegistry() {}

    public static String suffix(int index) {
        if (index < 0 || index >= COUNT) {
            throw new IllegalArgumentException("item category index out of range: " + index);
        }
        return SUFFIXES[index];
    }
}
