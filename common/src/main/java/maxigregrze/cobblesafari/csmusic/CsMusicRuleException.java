package maxigregrze.cobblesafari.csmusic;

import org.jetbrains.annotations.Nullable;

/**
 * Internal control exception: a value the loader cannot interpret. Because {@code when} is an AND of
 * constraints, dropping a malformed one <b>widens</b> the rule - a typo in {@code biome_tag} used to
 * make the track play in every biome of every dimension. Raising instead lets
 * {@link CsMusicTriggerDataLoader} reject the whole file, so the failure direction is "the rule never
 * plays" rather than "the rule plays everywhere".
 *
 * <p>Carries no stack trace: it is flow control over a file being parsed, never a crash.</p>
 */
final class CsMusicRuleException extends RuntimeException {

    private final transient int ruleIndex;
    private final transient String field;
    @Nullable
    private final transient String value;

    CsMusicRuleException(int ruleIndex, String field, @Nullable String value) {
        super("rule#" + ruleIndex + " invalid " + field + " '" + value + "'", null, false, false);
        this.ruleIndex = ruleIndex;
        this.field = field;
        this.value = value;
    }

    int ruleIndex() {
        return ruleIndex;
    }

    String field() {
        return field;
    }

    @Nullable
    String value() {
        return value;
    }
}
