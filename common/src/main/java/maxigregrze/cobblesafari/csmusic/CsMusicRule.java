package maxigregrze.cobblesafari.csmusic;

import org.jetbrains.annotations.Nullable;

/**
 * A compiled play-trigger rule: when {@link #condition} matches a player, this rule proposes a track
 * (either an explicit {@link #musicId} or a random pick from {@link #poolTag}) at {@link #priority}.
 * {@link #source} is a stable, human-readable key ({@code definition:<file>#<idx>} or
 * {@code config:dimension:<id>}) used for the resolver latch and {@code /csmusic current}.
 */
public record CsMusicRule(
        String source,
        @Nullable String musicId,
        @Nullable String poolTag,
        int priority,
        CsMusicCondition condition
) {
    public boolean isPool() {
        return poolTag != null;
    }
}
