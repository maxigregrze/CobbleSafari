package maxigregrze.cobblesafari.data;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Identifies <em>one visit</em> to a timed dimension (action plan 181, V1).
 * <p>
 * A non-instanced dimension (Safari, Union Room) has a single account per player, so its scope is
 * just the dimension id. An instanced dimension (a dungeon) has one <em>session</em> per player per
 * live instance, so its scope carries an opaque {@code scopeId} - for dungeons, the
 * {@link DungeonInstanceRecord#instanceId()} as a string.
 * <p>
 * The scope id is deliberately a {@code String} and not a typed instance id: the Union Room
 * identifies its rooms with an {@code int}, and an addon could use anything. The storage key is a
 * {@code String} either way, so the generality costs nothing.
 * <p>
 * <b>This type exists to stop a storage key being mistaken for a dimension id.</b> Methods that take
 * a key take a {@code TimerScope}; methods that take a dimension keep their {@code String}. None
 * takes both.
 */
public record TimerScope(String dimensionId, @Nullable String scopeId) {

    public static final char SEP = '@';

    public static TimerScope of(String dimensionId) {
        return new TimerScope(dimensionId, null);
    }

    public static TimerScope of(String dimensionId, String scopeId) {
        return new TimerScope(dimensionId, scopeId);
    }

    public static TimerScope of(String dimensionId, UUID instanceId) {
        return new TimerScope(dimensionId, instanceId.toString());
    }

    /** {@code true} when this scope names a single visit rather than a whole dimension. */
    public boolean isSession() {
        return scopeId != null;
    }

    /** Storage key: {@code "dim"} for a non-instanced dimension, {@code "dim@scope"} otherwise. */
    public String key() {
        return scopeId == null ? dimensionId : dimensionId + SEP + scopeId;
    }

    public static TimerScope parse(String key) {
        int sep = key.indexOf(SEP);
        return sep < 0
                ? of(key)
                : new TimerScope(key.substring(0, sep), key.substring(sep + 1));
    }

    @Override
    public String toString() {
        return key();
    }
}
