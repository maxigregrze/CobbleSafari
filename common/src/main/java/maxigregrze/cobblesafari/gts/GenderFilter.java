package maxigregrze.cobblesafari.gts;

import com.cobblemon.mod.common.pokemon.Gender;

import java.util.Locale;

public enum GenderFilter {
    ANY,
    MALE,
    FEMALE,
    GENDERLESS;

    public static GenderFilter parse(String input) {
        if (input == null || input.isEmpty()) {
            return ANY;
        }
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "any", "*", "" -> ANY;
            case "male", "m" -> MALE;
            case "female", "f" -> FEMALE;
            case "genderless", "n", "none" -> GENDERLESS;
            default -> ANY;
        };
    }

    /** @return {@code null} when {@link #ANY} */
    public Gender toCobblemonGenderOrNull() {
        return switch (this) {
            case ANY -> null;
            case MALE -> Gender.MALE;
            case FEMALE -> Gender.FEMALE;
            case GENDERLESS -> Gender.GENDERLESS;
        };
    }
}
