package maxigregrze.cobblesafari.security;

import java.util.Locale;
import java.util.Set;

public final class WishLineValidator {

    private static final int MAX_LEN = 64;
    private static final Set<String> ALLOWED_KEYS = Set.of("species", "form");

    private WishLineValidator() {}

    public static boolean isSafe(String line) {
        if (line == null || line.length() > MAX_LEN) {
            return false;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String[] parts = trimmed.split("\\s+");
        String first = parts[0].toLowerCase(Locale.ROOT);
        if (!first.matches("[a-z0-9_\\-]+") && !first.startsWith("species=")) {
            return false;
        }
        for (int i = 1; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq <= 0) {
                return false;
            }
            String k = parts[i].substring(0, eq).toLowerCase(Locale.ROOT);
            if (!ALLOWED_KEYS.contains(k)) {
                return false;
            }
        }
        return true;
    }
}
