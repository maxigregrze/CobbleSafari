package maxigregrze.cobblesafari.csmusic;

import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * What arbitration resolved for one player: either a single track, or a pool of tracks the client
 * sequences itself. The server decides <b>which pool wins</b> - that depends on dimension, biome,
 * battle state, areas and structures, all server-side truth. <b>Which track</b> inside a pool has
 * no gameplay effect and was already per-player, so it is drawn client-side.
 *
 * @param displayId identity of the instruction: a track id, or {@code #tag} for a pool
 * @param poolTag   the tag when this is a pool, {@code null} for a single track
 * @param entries   one definition for a single track, the whole pool otherwise
 * @param repeat    loop plays before the client draws again; {@code 0} = loop forever
 * @param priority  priority of the winning source, used to pick the transition (never sent)
 */
public record CsMusicInstruction(String displayId,
                                 @Nullable String poolTag,
                                 List<CsMusicDefinition> entries,
                                 int repeat,
                                 int priority) {

    public static CsMusicInstruction single(CsMusicDefinition def, int priority) {
        return new CsMusicInstruction(def.id(), null, List.of(def), 0, priority);
    }

    /**
     * Pool drawn by tag. Entries are sorted by id so the {@link #signature()} is canonical: the
     * insertion order of the registry's tag index depends on load order and could otherwise yield
     * two different signatures for the same pool.
     *
     * <p>A pool holding a single track falls back to {@code repeat = 0}: there is nothing to draw
     * next, and replaying its intro every N loops would sound like the track restarting itself.</p>
     */
    public static CsMusicInstruction pool(String tag, List<CsMusicDefinition> defs,
                                          int repeat, int priority) {
        List<CsMusicDefinition> sorted = defs.stream()
                .sorted(Comparator.comparing(CsMusicDefinition::id)).toList();
        return new CsMusicInstruction("#" + tag, tag, sorted,
                sorted.size() < 2 ? 0 : Math.max(0, repeat), priority);
    }

    public boolean isPool() {
        return poolTag != null;
    }

    /**
     * The one track this instruction can only ever play, or {@code null} when the client gets to
     * choose. A pool of one qualifies: the server knows exactly what will be heard.
     */
    @Nullable
    public CsMusicDefinition soleTrack() {
        return entries.size() == 1 ? entries.get(0) : null;
    }

    /** True when every entry declares an outro, i.e. any of them can conclude on its own. */
    public boolean allHaveOutro() {
        return entries.stream().allMatch(CsMusicDefinition::hasOutro);
    }

    /**
     * Content signature: two instructions sharing one need no packet. This is what replaces the old
     * per-rule latch - two rules pointing at the same tag now produce the same signature, so
     * straddling the border between them no longer re-draws a track every sweep.
     *
     * <p>{@link #priority} is deliberately <b>excluded</b>: it picks the transition, it does not
     * decide whether to re-emit. Two rules of different priorities naming the same content must not
     * restart playback.</p>
     */
    public String signature() {
        StringBuilder sb = new StringBuilder(displayId).append('|').append(repeat);
        for (CsMusicDefinition d : entries) {
            sb.append('|').append(d.id());
        }
        return sb.toString();
    }
}
